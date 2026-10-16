package com.petassegang.addons.backrooms.portal.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntityRegistry;
import com.petassegang.addons.backrooms.portal.transition.DestinationLoader;
import com.petassegang.addons.backrooms.portal.transition.PortalRelevance;
import com.petassegang.addons.config.ModConfig;

/**
 * Envoie a chaque joueur ce qu'il voit a travers ses portails, et le tient a jour : chunks, blocs et entities.
 *
 * <p>Vanilla ne synchronise qu'une dimension par joueur : celle ou il se trouve. Pour montrer l'autre
 * cote d'un portail, le serveur choisit pour chaque dimension distante un foyer (le portail regarde le
 * plus proche), envoie la fenetre de chunks autour de son arrivee, relaie les changements de blocs,
 * branche le joueur sur le suivi des entities de la fenetre, et retire tout ce qui n'est plus regarde.
 * Tout vit sur le thread serveur ; les structures sont reutilisees d'un tick a l'autre pour ne rien
 * allouer quand personne ne regarde de portail.
 */
public final class PortalViewTracker {

    /** Intervalle de recalcul des fenetres voulues. */
    static final int UPDATE_INTERVAL_TICKS = 5;
    /** Chunks complets envoyes au plus par joueur et par recalcul, pour lisser la bande passante. */
    static final int MAX_CHUNKS_PER_UPDATE = 8;
    /** Delai avant de decharger une dimension que le joueur ne regarde plus : un demi-tour ne doit pas tout renvoyer. */
    static final int UNLOAD_GRACE_TICKS = 100;

    private static final Map<UUID, PlayerView> VIEWS = new HashMap<>();
    private static final List<PlayerView> VIEW_LIST = new ArrayList<>();
    private static final List<BackroomsPortalEntity> RELEVANT_SCRATCH = new ArrayList<>();
    private static final Map<RegistryKey<World>, BackroomsPortalEntity> ANCHOR_SCRATCH = new HashMap<>();
    private static final Map<RegistryKey<World>, LongList> WANTED_SCRATCH = new HashMap<>();
    private static final List<RegistryKey<World>> DIMENSION_SCRATCH = new ArrayList<>();
    /** Chunks de sa nouvelle dimension que le client d'un joueur tient deja, le temps que vanilla les lui propose. */
    private static final Map<UUID, LongSet> HELD_AFTER_CROSSING = new HashMap<>();
    /** Nombre total de chunks distants presents chez les clients ; zero = rien a faire sur les changements de blocs. */
    private static int watchedChunkCount;
    private static boolean initialized;

    /** Etat de synchronisation d'un joueur. */
    private static final class PlayerView {
        final UUID playerUuid;
        final Map<RegistryKey<World>, LongSet> sentChunks = new HashMap<>();
        final Map<RegistryKey<World>, LongSet> pendingBlockUpdates = new HashMap<>();
        final Map<RegistryKey<World>, RemoteEntityView> entities = new HashMap<>();
        final List<RegistryKey<World>> announcedDimensions = new ArrayList<>();
        /** Dernier tick ou chaque dimension etait voulue (zero si jamais). */
        final Object2LongMap<RegistryKey<World>> lastWantedTick = new Object2LongOpenHashMap<>();
        boolean hasPendingBlockUpdates;

        PlayerView(UUID playerUuid) {
            this.playerUuid = playerUuid;
        }
    }

    private PortalViewTracker() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Branche le tick serveur et les evenements de joueur ; idempotent. */
    public static void init() {
        if (initialized) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(PortalViewTracker::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(server, handler.player.getUuid(), false));
        // Le client repart d'un monde vide apres un changement de dimension vanilla ou un respawn : on renvoie tout.
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> onPlayerChangedWorld(player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> onPlayerChangedWorld(newPlayer));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            VIEWS.clear();
            VIEW_LIST.clear();
            HELD_AFTER_CROSSING.clear();
            watchedChunkCount = 0;
        });
        initialized = true;
    }

    /**
     * Signale un changement de bloc serveur ; appele par le mixin de {@code ServerChunkManager}.
     * Sans joueur regardant un chunk distant, le cout est un test d'entier.
     *
     * @param world monde du bloc
     * @param pos position du bloc
     */
    public static void onBlockChanged(ServerWorld world, BlockPos pos) {
        if (watchedChunkCount == 0) {
            return;
        }
        long chunkKey = ChunkPos.toLong(pos);
        RegistryKey<World> dimension = world.getRegistryKey();
        for (int i = 0; i < VIEW_LIST.size(); i++) {
            PlayerView view = VIEW_LIST.get(i);
            LongSet sent = view.sentChunks.get(dimension);
            if (sent != null && sent.contains(chunkKey)) {
                view.pendingBlockUpdates.computeIfAbsent(dimension, key -> new LongOpenHashSet()).add(pos.asLong());
                view.hasPendingBlockUpdates = true;
            }
        }
    }

    /**
     * Le joueur vient de changer de monde (portail ou vanilla) : sa synchronisation repart de zero. Son client garde
     * ses mondes distants apres une traversee par portail : les entities qu'il y suivait lui sont retirees, celles de sa
     * nouvelle dimension comprises (copies distantes a effacer avant les apparitions vanilla, qui suivent).
     */
    public static void onPlayerChangedWorld(ServerPlayerEntity player) {
        forget(player.getServer(), player.getUuid(), true);
    }

    /**
     * Le joueur passe dans {@code destination} : les chunks de cette dimension que son client tient deja (a jour, deja
     * construits) ne lui seront pas renvoyes par vanilla, voir {@code MixinChunkDataSender}. A appeler avant
     * {@link #onPlayerChangedWorld}, qui oublie la vue.
     */
    public static void keepHeldChunks(ServerPlayerEntity player, RegistryKey<World> destination) {
        PlayerView view = VIEWS.get(player.getUuid());
        LongSet sent = view == null ? null : view.sentChunks.get(destination);
        if (sent == null || sent.isEmpty()) {
            HELD_AFTER_CROSSING.remove(player.getUuid());
            return;
        }
        HELD_AFTER_CROSSING.put(player.getUuid(), new LongOpenHashSet(sent));
    }

    /** {@code true} si vanilla peut se dispenser d'envoyer ce chunk au joueur : son client le tient deja. Consomme l'entree. */
    public static boolean consumeHeldChunk(ServerPlayerEntity player, ChunkPos pos) {
        LongSet held = HELD_AFTER_CROSSING.get(player.getUuid());
        if (held == null || !held.remove(pos.toLong())) {
            return false;
        }
        if (held.isEmpty()) {
            HELD_AFTER_CROSSING.remove(player.getUuid());
        }
        return true;
    }

    /** Nombre de chunks distants actuellement presents chez tous les clients. */
    public static int watchedChunkCount() {
        return watchedChunkCount;
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % UPDATE_INTERVAL_TICKS == 0) {
            List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
            for (int i = 0; i < players.size(); i++) {
                update(server, players.get(i));
            }
        }
        flushBlockUpdates(server);
    }

    private static void update(MinecraftServer server, ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        collectRelevantPortals(player, world);
        PlayerView view = VIEWS.get(player.getUuid());
        // Une vue existante a toujours au moins une dimension inscrite : elle reste a decharger meme sans portail regarde.
        if (RELEVANT_SCRATCH.isEmpty() && view == null) {
            return;
        }
        if (view == null) {
            view = new PlayerView(player.getUuid());
            VIEWS.put(player.getUuid(), view);
            VIEW_LIST.add(view);
        }
        computeWantedWindows(player);
        long tick = server.getTicks();
        for (int i = 0; i < DIMENSION_SCRATCH.size(); i++) {
            view.lastWantedTick.put(DIMENSION_SCRATCH.get(i), tick);
        }
        for (Map.Entry<RegistryKey<World>, LongSet> entry : view.sentChunks.entrySet()) {
            // Toute dimension deja synchronisee a sa liste voulue : les listes ne quittent jamais WANTED_SCRATCH.
            boolean unwanted = WANTED_SCRATCH.get(entry.getKey()).isEmpty();
            if (unwanted && tick - view.lastWantedTick.getLong(entry.getKey()) > UNLOAD_GRACE_TICKS) {
                unloadAll(server, player, view, entry.getKey(), entry.getValue());
            }
        }
        for (int i = 0; i < DIMENSION_SCRATCH.size(); i++) {
            RegistryKey<World> dimension = DIMENSION_SCRATCH.get(i);
            syncDimension(server, player, view, dimension, WANTED_SCRATCH.get(dimension));
        }
    }

    /** Portails du monde du joueur qu'il regarde et qui menent dans une autre dimension. */
    private static void collectRelevantPortals(ServerPlayerEntity player, ServerWorld world) {
        RELEVANT_SCRATCH.clear();
        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        double maxDistance = Math.max(ModConfig.PORTAL_ACTIVATION_RADIUS_BLOCKS, ModConfig.PORTAL_RENDER_DISTANCE_BLOCKS);
        List<BackroomsPortalEntity> portals = BackroomsPortalEntityRegistry.all();
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (portal.getWorld() != world || !portal.visible() || portal.destinationDim().equals(world.getRegistryKey())) {
                continue;
            }
            if (PortalRelevance.isRelevant(eye, look, portal.getPos(), maxDistance)) {
                RELEVANT_SCRATCH.add(portal);
            }
        }
    }

    /** Par dimension distante : foyer = portail le plus proche ; fenetres de tous les portails qui tiennent autour. */
    private static void computeWantedWindows(ServerPlayerEntity player) {
        ANCHOR_SCRATCH.clear();
        DIMENSION_SCRATCH.clear();
        for (LongList wanted : WANTED_SCRATCH.values()) {
            wanted.clear();
        }
        for (int i = 0; i < RELEVANT_SCRATCH.size(); i++) {
            BackroomsPortalEntity portal = RELEVANT_SCRATCH.get(i);
            BackroomsPortalEntity anchor = ANCHOR_SCRATCH.get(portal.destinationDim());
            if (anchor == null || player.squaredDistanceTo(portal) < player.squaredDistanceTo(anchor)) {
                ANCHOR_SCRATCH.put(portal.destinationDim(), portal);
            }
        }
        for (int i = 0; i < RELEVANT_SCRATCH.size(); i++) {
            BackroomsPortalEntity portal = RELEVANT_SCRATCH.get(i);
            // Pres du portail, la traversee est imminente : fenetre plus large pour que l'arrivee soit deja construite.
            int radius = DestinationLoader.syncRadius(DestinationLoader.isNear(player, portal));
            RegistryKey<World> dimension = portal.destinationDim();
            Vec3d anchorPos = ANCHOR_SCRATCH.get(dimension).destinationPos();
            Vec3d destinationPos = portal.destinationPos();
            int chunkX = ChunkSectionPos.getSectionCoord(destinationPos.x);
            int chunkZ = ChunkSectionPos.getSectionCoord(destinationPos.z);
            if (!RemoteChunkWindow.fitsAround(
                    ChunkSectionPos.getSectionCoord(anchorPos.x), ChunkSectionPos.getSectionCoord(anchorPos.z), chunkX, chunkZ, radius)) {
                continue;
            }
            LongList wanted = WANTED_SCRATCH.computeIfAbsent(dimension, key -> new LongArrayList());
            if (wanted.isEmpty()) {
                DIMENSION_SCRATCH.add(dimension);
            }
            RemoteChunkWindow.collect(chunkX, chunkZ, radius, wanted);
        }
    }

    private static void syncDimension(
            MinecraftServer server,
            ServerPlayerEntity player,
            PlayerView view,
            RegistryKey<World> dimension,
            LongList wanted) {
        LongSet sent = view.sentChunks.computeIfAbsent(dimension, key -> new LongOpenHashSet());
        Identifier dimensionId = dimension.getValue();
        // Dechargements d'abord : si le foyer a saute, le client recentre sa carte sur le premier chunk suivant.
        LongIterator iterator = sent.iterator();
        while (iterator.hasNext()) {
            long key = iterator.nextLong();
            if (!wanted.contains(key)) {
                iterator.remove();
                watchedChunkCount--;
                ServerPlayNetworking.send(player, new RemoteChunkUnloadPayload(dimensionId, new ChunkPos(key)));
            }
        }
        ServerWorld destination = server.getWorld(dimension);
        if (destination == null) {
            return;
        }
        int budget = MAX_CHUNKS_PER_UPDATE;
        for (int i = 0; i < wanted.size() && budget > 0; i++) {
            long key = wanted.getLong(i);
            if (sent.contains(key)) {
                continue;
            }
            WorldChunk chunk = destination.getChunkManager().getWorldChunk(ChunkPos.getPackedX(key), ChunkPos.getPackedZ(key));
            if (chunk == null) {
                // Pas encore charge : le ticket de DestinationLoader s'en occupe, on reessaie au prochain passage.
                continue;
            }
            announceDimension(player, view, destination);
            ServerPlayNetworking.send(player, RemoteChunkPayload.of(destination, chunk));
            sent.add(key);
            watchedChunkCount++;
            budget--;
        }
        if (!sent.isEmpty()) {
            view.entities.computeIfAbsent(dimension, key -> new RemoteEntityView(player, key))
                    .update(destination, sent);
        }
    }

    private static void announceDimension(ServerPlayerEntity player, PlayerView view, ServerWorld destination) {
        RegistryKey<World> dimension = destination.getRegistryKey();
        if (view.announcedDimensions.contains(dimension)) {
            return;
        }
        ServerPlayNetworking.send(player, RemoteDimensionPayload.of(destination));
        view.announcedDimensions.add(dimension);
    }

    private static void unloadAll(MinecraftServer server, ServerPlayerEntity player, PlayerView view, RegistryKey<World> dimension, LongSet sent) {
        RemoteEntityView entities = view.entities.remove(dimension);
        if (entities != null) {
            entities.clear(server.getWorld(dimension), true);
        }
        Identifier dimensionId = dimension.getValue();
        LongIterator iterator = sent.iterator();
        while (iterator.hasNext()) {
            ServerPlayNetworking.send(player, new RemoteChunkUnloadPayload(dimensionId, new ChunkPos(iterator.nextLong())));
        }
        watchedChunkCount -= sent.size();
        sent.clear();
    }

    private static void flushBlockUpdates(MinecraftServer server) {
        for (int i = 0; i < VIEW_LIST.size(); i++) {
            PlayerView view = VIEW_LIST.get(i);
            if (!view.hasPendingBlockUpdates) {
                continue;
            }
            view.hasPendingBlockUpdates = false;
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(view.playerUuid);
            for (Map.Entry<RegistryKey<World>, LongSet> entry : view.pendingBlockUpdates.entrySet()) {
                LongSet pending = entry.getValue();
                // La dimension existe tant que le serveur tourne ; la vue d'un joueur deconnecte est retiree avant ce tick.
                ServerWorld world = server.getWorld(entry.getKey());
                if (player != null) {
                    Identifier dimensionId = entry.getKey().getValue();
                    LongIterator iterator = pending.iterator();
                    while (iterator.hasNext()) {
                        BlockPos pos = BlockPos.fromLong(iterator.nextLong());
                        ServerPlayNetworking.send(player, new RemoteBlockUpdatePayload(dimensionId, pos, world.getBlockState(pos)));
                    }
                }
                pending.clear();
            }
        }
    }

    private static void forget(MinecraftServer server, UUID playerUuid, boolean notifyClient) {
        if (!notifyClient) {
            HELD_AFTER_CROSSING.remove(playerUuid);
        }
        PlayerView view = VIEWS.remove(playerUuid);
        if (view == null) {
            return;
        }
        VIEW_LIST.remove(view);
        for (LongSet sent : view.sentChunks.values()) {
            watchedChunkCount -= sent.size();
        }
        for (RemoteEntityView entities : view.entities.values()) {
            entities.clear(server.getWorld(entities.dimension()), notifyClient);
        }
    }
}
