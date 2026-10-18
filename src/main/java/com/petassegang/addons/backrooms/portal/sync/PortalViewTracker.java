package com.petassegang.addons.backrooms.portal.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldEventS2CPacket;
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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntityRegistry;
import com.petassegang.addons.backrooms.portal.transition.DestinationLoader;
import com.petassegang.addons.backrooms.portal.transition.PortalRelevance;
import com.petassegang.addons.mixin.EntityTrackerAccessor;
import com.petassegang.addons.mixin.EntityTrackerEntryAccessor;
import com.petassegang.addons.perf.ModPerformanceMonitor;

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
    private static final String PERF_SYNC_TICK = "portal.sync.tick";
    private static final String PERF_SYNC_CHUNKS = "portal.sync.chunks";
    private static final String PERF_SYNC_ENTITIES = "portal.sync.entities";

    private static final Map<UUID, PlayerView> VIEWS = new HashMap<>();
    private static final List<PlayerView> VIEW_LIST = new ArrayList<>();
    private static final List<BackroomsPortalEntity> RELEVANT_SCRATCH = new ArrayList<>();
    private static final Map<RegistryKey<World>, BackroomsPortalEntity> ANCHOR_SCRATCH = new HashMap<>();
    private static final Map<RegistryKey<World>, LongSet> WANTED_SCRATCH = new HashMap<>();
    private static final List<RegistryKey<World>> DIMENSION_SCRATCH = new ArrayList<>();
    /**
     * Duree, en ticks serveur, pendant laquelle les chunks qu'un client tient apres une traversee valent : vanilla propose
     * les chunks proches en quelques ticks ; un chunk qu'il ne propose qu'apres (hors de la distance de vue, propose bien
     * plus tard) a pu etre oublie par le client entre-temps, et doit partir comme les autres.
     */
    public static final int HELD_CHUNKS_TTL_TICKS = 100;
    /** Chunks de sa nouvelle dimension que le client d'un joueur tient deja, le temps que vanilla les lui propose. */
    private static final Map<UUID, HeldChunks> HELD_AFTER_CROSSING = new HashMap<>();
    /** Nombre total de chunks distants presents chez les clients ; zero = rien a faire sur les changements de blocs. */
    private static int watchedChunkCount;
    private static boolean initialized;

    /**
     * Etat de synchronisation d'un joueur. La vue est oubliee a la deconnexion et a chaque changement de monde ou
     * reapparition (nouvel objet joueur) : le joueur qu'elle garde est toujours celui qui est connecte.
     */
    private static final class PlayerView {
        final ServerPlayerEntity player;
        final Map<RegistryKey<World>, LongSet> sentChunks = new HashMap<>();
        final Map<RegistryKey<World>, LongSet> pendingBlockUpdates = new HashMap<>();
        final Map<RegistryKey<World>, RemoteEntityView> entities = new HashMap<>();
        final List<RegistryKey<World>> announcedDimensions = new ArrayList<>();
        /** Dernier tick ou chaque dimension etait voulue (zero si jamais). */
        final Object2LongMap<RegistryKey<World>> lastWantedTick = new Object2LongOpenHashMap<>();
        boolean hasPendingBlockUpdates;

        PlayerView(ServerPlayerEntity player) {
            this.player = player;
        }
    }

    /** Chunks tenus par le client d'un joueur apres une traversee, et tick serveur jusqu'auquel ils valent. */
    private record HeldChunks(LongSet chunks, long expiresAt) {
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
        // Le client repart d'un monde vide apres un changement de dimension vanilla ou un respawn : on renvoie tout, et il
        // ne tient plus rien d'une traversee recente. Une traversee par portail passe par onPlayerChangedWorld seul.
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> onVanillaWorldChange(player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> onVanillaWorldChange(newPlayer));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> onEntityUnloaded(world, entity));
        // Les listes de travail tiennent des portails, donc leurs mondes : en solo, elles retiendraient la partie precedente.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            VIEWS.clear();
            VIEW_LIST.clear();
            HELD_AFTER_CROSSING.clear();
            RELEVANT_SCRATCH.clear();
            ANCHOR_SCRATCH.clear();
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

    /** Changement de monde vanilla ou respawn : le client recree son monde, et ne tient plus aucun chunk. */
    private static void onVanillaWorldChange(ServerPlayerEntity player) {
        HELD_AFTER_CROSSING.remove(player.getUuid());
        onPlayerChangedWorld(player);
    }

    /**
     * Le joueur passe dans {@code destination} : les chunks de cette dimension que son client tient deja (a jour, deja
     * construits) ne lui seront pas renvoyes par vanilla, voir {@code MixinChunkDataSender}. A appeler avant
     * {@link #onPlayerChangedWorld}, qui oublie la vue.
     *
     * @return nombre de chunks de {@code destination} que le client tient deja
     */
    public static int keepHeldChunks(ServerPlayerEntity player, RegistryKey<World> destination) {
        PlayerView view = VIEWS.get(player.getUuid());
        LongSet sent = view == null ? null : view.sentChunks.get(destination);
        int held = sent == null ? 0 : sent.size();
        if (held == 0) {
            HELD_AFTER_CROSSING.remove(player.getUuid());
            return 0;
        }
        HELD_AFTER_CROSSING.put(player.getUuid(), new HeldChunks(new LongOpenHashSet(sent), player.getServer().getTicks() + HELD_CHUNKS_TTL_TICKS));
        return held;
    }

    /**
     * {@code true} si vanilla peut se dispenser d'envoyer ce chunk au joueur : son client le tient deja, depuis une
     * traversee recente. Consomme l'entree ; passe {@link #HELD_CHUNKS_TTL_TICKS}, plus rien n'est tenu.
     */
    public static boolean consumeHeldChunk(ServerPlayerEntity player, ChunkPos pos) {
        HeldChunks held = HELD_AFTER_CROSSING.get(player.getUuid());
        if (held == null) {
            return false;
        }
        if (player.getServer().getTicks() > held.expiresAt()) {
            HELD_AFTER_CROSSING.remove(player.getUuid());
            return false;
        }
        if (!held.chunks().remove(pos.toLong())) {
            return false;
        }
        if (held.chunks().isEmpty()) {
            HELD_AFTER_CROSSING.remove(player.getUuid());
        }
        return true;
    }

    /** Nombre de chunks distants actuellement presents chez tous les clients. */
    public static int watchedChunkCount() {
        return watchedChunkCount;
    }

    /**
     * Envoie un son, un evenement de monde ou des particules d'une dimension aux joueurs qui la voient a travers un
     * portail, si le point tombe dans un chunk de leur fenetre ; appele par les mixins de {@code PlayerManager} et de
     * {@code ServerWorld}, apres l'envoi vanilla aux joueurs de la dimension. C'est le client qui decide par quel
     * portail le son lui parvient et ou s'affichent les particules.
     *
     * @return nombre de joueurs servis
     */
    public static int relayToViewers(RegistryKey<World> dimension, double x, double z, Packet<?> packet) {
        if (watchedChunkCount == 0 || !isRelayed(packet)) {
            return 0;
        }
        long chunk = ChunkPos.toLong(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z));
        int served = 0;
        for (int i = 0; i < VIEW_LIST.size(); i++) {
            RemoteEntityView entities = viewerOf(VIEW_LIST.get(i), dimension, chunk);
            if (entities != null) {
                entities.sendPacket(packet);
                served++;
            }
        }
        return served;
    }

    /**
     * Une entity vient d'arriver dans une dimension par un portail : les joueurs qui la voient a travers un portail
     * recoivent sa copie tout de suite, comme vanilla l'envoie aux joueurs de la dimension, et non au prochain
     * alignement de leur fenetre. Leur client recoit au meme tick la destruction de l'ancienne copie, de ce cote :
     * sans cela, celle-ci resterait figee dans le cadre jusqu'a cinq ticks avant que la nouvelle n'en sorte.
     *
     * @param destination monde ou l'entity est arrivee
     * @param entity entity arrivee
     */
    public static void onEntityArrived(ServerWorld destination, Entity entity) {
        if (watchedChunkCount == 0) {
            return;
        }
        RegistryKey<World> dimension = destination.getRegistryKey();
        long chunk = entity.getChunkPos().toLong();
        for (int i = 0; i < VIEW_LIST.size(); i++) {
            RemoteEntityView entities = viewerOf(VIEW_LIST.get(i), dimension, chunk);
            if (entities != null) {
                entities.track(destination, entity);
            }
        }
    }

    /**
     * Une entity quitte son monde (morte, retiree, chunk decharge, autre dimension). Vanilla envoie sa destruction aux
     * auditeurs de son suivi par leur connexion, sans enveloppe : le client l'appliquerait a son monde principal. Ceux qui
     * la voyaient par un portail recoivent la destruction de leur copie distante tout de suite, et non au prochain
     * alignement de leur fenetre. Fabric le signale juste avant que vanilla ne retire le suivi de l'entity : une vue ne
     * garde ainsi jamais d'identifiant sans suivi vanilla.
     *
     * @param world monde que l'entity quitte
     * @param entity entity retiree
     */
    static void onEntityUnloaded(ServerWorld world, Entity entity) {
        if (VIEW_LIST.isEmpty()) {
            return;
        }
        RegistryKey<World> dimension = world.getRegistryKey();
        for (int i = 0; i < VIEW_LIST.size(); i++) {
            RemoteEntityView entities = VIEW_LIST.get(i).entities.get(dimension);
            if (entities != null) {
                entities.forget(entity.getId());
            }
        }
    }

    /**
     * Un portail vient de deplacer une entity dans son propre monde : son prochain deplacement part aux clients en position
     * absolue, comme a la descente d'une monture. En dessous de huit blocs, vanilla enverrait un deplacement relatif, et les
     * clients la feraient glisser jusqu'a l'arrivee a travers ce qui les separe ; en position absolue, ils la font passer
     * par le portail. Une entity que vanilla ne suit pas (un marqueur) n'a rien a envoyer.
     *
     * @param world monde de l'entity
     * @param entity entity deplacee
     */
    public static void onEntityJumped(ServerWorld world, Entity entity) {
        if (RemoteEntityView.trackers(world).get(entity.getId()) instanceof EntityTrackerAccessor tracker) {
            ((EntityTrackerEntryAccessor) tracker.backrooms$getEntry()).backrooms$setHadVehicle(true);
        }
    }

    /** Vue d'entities d'un joueur pour cette dimension si le chunk est dans sa fenetre, sinon {@code null}. */
    private static @Nullable RemoteEntityView viewerOf(PlayerView view, RegistryKey<World> dimension, long chunk) {
        // Une vue d'entities n'existe que pour une dimension dont des chunks ont ete envoyes.
        RemoteEntityView entities = view.entities.get(dimension);
        return entities != null && view.sentChunks.get(dimension).contains(chunk) ? entities : null;
    }

    /** Paquets qu'un joueur recoit d'une dimension vue a travers un portail, en plus de ceux de ses entities. */
    static boolean isRelayed(Packet<?> packet) {
        return packet instanceof PlaySoundS2CPacket
                || packet instanceof PlaySoundFromEntityS2CPacket
                || packet instanceof WorldEventS2CPacket
                || packet instanceof ParticleS2CPacket;
    }

    @SuppressWarnings("try")
    private static void tick(MinecraftServer server) {
        try (ModPerformanceMonitor.Scope ignored = ModPerformanceMonitor.scope(PERF_SYNC_TICK)) {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS == 0) {
                List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
                for (int i = 0; i < players.size(); i++) {
                    update(server, players.get(i));
                }
            }
            flushBlockUpdates(server);
        }
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
            view = new PlayerView(player);
            VIEWS.put(player.getUuid(), view);
            VIEW_LIST.add(view);
        }
        computeWantedWindows(player);
        long tick = server.getTicks();
        for (int i = 0; i < DIMENSION_SCRATCH.size(); i++) {
            view.lastWantedTick.put(DIMENSION_SCRATCH.get(i), tick);
        }
        for (Map.Entry<RegistryKey<World>, LongSet> entry : view.sentChunks.entrySet()) {
            // Toute dimension deja synchronisee a son ensemble voulu : les ensembles ne quittent jamais WANTED_SCRATCH.
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
        List<BackroomsPortalEntity> portals = BackroomsPortalEntityRegistry.all();
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (portal.getWorld() != world || !portal.visible() || portal.destinationDim().equals(world.getRegistryKey())) {
                continue;
            }
            if (PortalRelevance.isRelevant(player, portal.getPos(), PortalRelevance.MAX_DISTANCE)) {
                RELEVANT_SCRATCH.add(portal);
            }
        }
    }

    /** Par dimension distante : foyer = portail le plus proche ; fenetres de tous les portails qui tiennent autour. */
    private static void computeWantedWindows(ServerPlayerEntity player) {
        ANCHOR_SCRATCH.clear();
        DIMENSION_SCRATCH.clear();
        for (LongSet wanted : WANTED_SCRATCH.values()) {
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
            // Ensemble ordonne : les chunks partent du centre vers l'exterieur, et l'appartenance se teste sans parcours.
            LongSet wanted = WANTED_SCRATCH.computeIfAbsent(dimension, key -> new LongLinkedOpenHashSet());
            if (wanted.isEmpty()) {
                DIMENSION_SCRATCH.add(dimension);
            }
            RemoteChunkWindow.collect(chunkX, chunkZ, radius, wanted);
        }
    }

    @SuppressWarnings("try")
    private static void syncDimension(
            MinecraftServer server,
            ServerPlayerEntity player,
            PlayerView view,
            RegistryKey<World> dimension,
            LongSet wanted) {
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
        try (ModPerformanceMonitor.Scope ignored = ModPerformanceMonitor.scope(PERF_SYNC_CHUNKS)) {
            int budget = MAX_CHUNKS_PER_UPDATE;
            LongIterator wantedKeys = wanted.iterator();
            while (budget > 0 && wantedKeys.hasNext()) {
                long key = wantedKeys.nextLong();
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
        }
        if (!sent.isEmpty()) {
            try (ModPerformanceMonitor.Scope ignored = ModPerformanceMonitor.scope(PERF_SYNC_ENTITIES)) {
                RemoteEntityView entities = view.entities.get(dimension);
                if (entities == null) {
                    entities = new RemoteEntityView(player, dimension);
                    view.entities.put(dimension, entities);
                }
                entities.update(destination, sent);
                entities.syncWeather(destination);
            }
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
            for (Map.Entry<RegistryKey<World>, LongSet> entry : view.pendingBlockUpdates.entrySet()) {
                LongSet pending = entry.getValue();
                // La dimension existe tant que le serveur tourne.
                ServerWorld world = server.getWorld(entry.getKey());
                Identifier dimensionId = entry.getKey().getValue();
                LongIterator iterator = pending.iterator();
                while (iterator.hasNext()) {
                    BlockPos pos = BlockPos.fromLong(iterator.nextLong());
                    ServerPlayNetworking.send(view.player, new RemoteBlockUpdatePayload(dimensionId, pos, world.getBlockState(pos)));
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
