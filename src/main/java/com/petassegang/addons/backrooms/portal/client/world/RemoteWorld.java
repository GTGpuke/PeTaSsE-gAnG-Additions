package com.petassegang.addons.backrooms.portal.client.world;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySetHeadYawS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExperienceOrbSpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;

import com.petassegang.addons.backrooms.portal.sync.RemoteChunkWindow;
import com.petassegang.addons.mixin.client.ClientPlayNetworkHandlerAccessor;
import com.petassegang.addons.mixin.client.WorldRendererAccessor;

/**
 * Une dimension vue a travers un portail : un {@link ClientWorld} et un {@link WorldRenderer} dedie, dessines
 * depuis une camera transposee par {@link RemoteViewRenderer}.
 *
 * <p>Deux formes. Le monde distant proprement dit est nourri par les payloads du serveur (chunks,
 * blocs, paquets d'entities) et tique ses entities pour les interpoler ; il n'a ni particules, ni
 * sons, ni tick de blocs. La vue secondaire du monde principal partage le monde du joueur, deja
 * entretenu par vanilla, et ne possede que son renderer : elle sert aux portails qui ramenent dans la
 * dimension courante, directement ou imbriques dans un autre portail.
 */
public final class RemoteWorld {

    private static final int RENDER_BUFFER_COUNT = 2;
    private static final int SIMULATION_DISTANCE = 2;
    /** Paquets d'entities vanilla qu'un monde distant accepte ; les autres concernent le joueur lui-meme ou son monde. */
    private static final Set<Class<?>> ENTITY_PACKETS = Set.of(
            EntitySpawnS2CPacket.class,
            ExperienceOrbSpawnS2CPacket.class,
            EntityS2CPacket.MoveRelative.class,
            EntityS2CPacket.Rotate.class,
            EntityS2CPacket.RotateAndMoveRelative.class,
            EntityPositionS2CPacket.class,
            EntityVelocityUpdateS2CPacket.class,
            EntitySetHeadYawS2CPacket.class,
            EntityTrackerUpdateS2CPacket.class,
            EntityAttributesS2CPacket.class,
            EntityEquipmentUpdateS2CPacket.class,
            EntityPassengersSetS2CPacket.class,
            EntityAttachS2CPacket.class,
            EntityStatusEffectS2CPacket.class,
            RemoveEntityStatusEffectS2CPacket.class,
            EntityAnimationS2CPacket.class,
            EntityStatusS2CPacket.class,
            EntityDamageS2CPacket.class,
            ItemPickupAnimationS2CPacket.class);

    private final ClientWorld world;
    private final WorldRenderer renderer;
    /** Vrai pour la vue secondaire du monde principal : chunks, entities et paquets restent geres par vanilla. */
    private final boolean mirror;
    private final RemoteViewRenderer viewRenderer;
    private int centerChunkX = Integer.MIN_VALUE;
    private int centerChunkZ = Integer.MIN_VALUE;

    private RemoteWorld(ClientWorld world, WorldRenderer renderer, boolean mirror) {
        this.world = world;
        this.renderer = renderer;
        this.mirror = mirror;
        this.viewRenderer = new RemoteViewRenderer(this);
    }

    /** Cree un monde distant vide et son renderer pour une dimension annoncee par le serveur. */
    static RemoteWorld create(
            MinecraftClient client,
            ClientPlayNetworkHandler networkHandler,
            RegistryKey<World> dimension,
            RegistryEntry<DimensionType> dimensionType) {
        WorldRenderer renderer = newRenderer(client);
        ClientWorld world = new ClientWorld(
                networkHandler,
                new ClientWorld.Properties(Difficulty.NORMAL, false, false),
                dimension,
                dimensionType,
                RemoteChunkWindow.LOAD_DISTANCE,
                SIMULATION_DISTANCE,
                client::getProfiler,
                renderer,
                false,
                0L);
        renderer.setWorld(world);
        return new RemoteWorld(world, renderer, false);
    }

    /**
     * Adopte l'ancien monde principal et son renderer comme monde distant apres une traversee.
     *
     * @param world monde quitte par le joueur
     * @param renderer son renderer, qui garde ses sections construites
     * @param centerChunkX chunk du joueur au moment du passage
     * @param centerChunkZ chunk du joueur au moment du passage
     * @return monde distant pret a etre rendu a travers le portail de retour
     */
    static RemoteWorld adopt(ClientWorld world, WorldRenderer renderer, int centerChunkX, int centerChunkZ) {
        RemoteWorld remote = new RemoteWorld(world, renderer, false);
        remote.centerChunkX = centerChunkX;
        remote.centerChunkZ = centerChunkZ;
        // Meme carte de chunks que les mondes distants crees de zero : les chunks existants sont conserves.
        world.getChunkManager().updateLoadDistance(RemoteChunkWindow.LOAD_DISTANCE);
        world.getChunkManager().setChunkMapCenter(centerChunkX, centerChunkZ);
        return remote;
    }

    /** Vue secondaire du monde principal : un renderer de plus sur le monde que vanilla entretient deja. */
    static RemoteWorld mirror(MinecraftClient client, ClientWorld world) {
        WorldRenderer renderer = newRenderer(client);
        renderer.setWorld(world);
        return new RemoteWorld(world, renderer, true);
    }

    private static WorldRenderer newRenderer(MinecraftClient client) {
        return new WorldRenderer(
                client,
                client.getEntityRenderDispatcher(),
                client.getBlockEntityRenderDispatcher(),
                new BufferBuilderStorage(RENDER_BUFFER_COUNT));
    }

    public ClientWorld world() {
        return this.world;
    }

    public WorldRenderer renderer() {
        return this.renderer;
    }

    /** {@code true} pour la vue secondaire du monde principal. */
    public boolean isMirror() {
        return this.mirror;
    }

    /** Dessin de ce monde depuis une camera transposee. */
    public RemoteViewRenderer viewRenderer() {
        return this.viewRenderer;
    }

    /** Charge (ou remplace) un chunk recu du serveur, avec sa lumiere, et programme son rendu. */
    public void loadChunk(int chunkX, int chunkZ, ChunkData chunkData, LightData lightData) {
        recenterIfNeeded(chunkX, chunkZ);
        ClientChunkManager chunks = this.world.getChunkManager();
        WorldChunk chunk = chunks.loadChunkFromPacket(
                chunkX, chunkZ, chunkData.getSectionsDataBuf(), chunkData.getHeightmap(), chunkData.getBlockEntities(chunkX, chunkZ));
        if (chunk == null) {
            return;
        }
        LightingProvider lighting = chunks.getLightingProvider();
        applyLight(lighting, chunkX, chunkZ, LightType.SKY, lightData.getInitedSky(), lightData.getUninitedSky(), lightData.getSkyNibbles().iterator());
        applyLight(lighting, chunkX, chunkZ, LightType.BLOCK, lightData.getInitedBlock(), lightData.getUninitedBlock(), lightData.getBlockNibbles().iterator());
        lighting.setColumnEnabled(new ChunkPos(chunkX, chunkZ), true);
        ChunkSection[] sections = chunk.getSectionArray();
        for (int index = 0; index < sections.length; index++) {
            int sectionY = this.world.sectionIndexToCoord(index);
            lighting.setSectionStatus(ChunkSectionPos.from(chunkX, sectionY, chunkZ), sections[index].isEmpty());
            this.world.scheduleBlockRenders(chunkX, sectionY, chunkZ);
        }
    }

    /** Oublie un chunk que le serveur ne synchronise plus. */
    public void unloadChunk(ChunkPos pos) {
        ClientChunkManager chunks = this.world.getChunkManager();
        WorldChunk chunk = chunks.getWorldChunk(pos.x, pos.z);
        if (chunk != null) {
            // Comme le gestionnaire de paquets vanilla : block entities et entities de la colonne cessent de ticker.
            this.world.unloadBlockEntities(chunk);
        }
        chunks.unload(pos);
        LightingProvider lighting = this.world.getLightingProvider();
        lighting.setColumnEnabled(pos, false);
        for (int sectionY = lighting.getBottomY(); sectionY < lighting.getTopY(); sectionY++) {
            ChunkSectionPos sectionPos = ChunkSectionPos.from(pos, sectionY);
            lighting.enqueueSectionData(LightType.BLOCK, sectionPos, null);
            lighting.enqueueSectionData(LightType.SKY, sectionPos, null);
        }
        for (int sectionY = this.world.getBottomSectionCoord(); sectionY < this.world.getTopSectionCoord(); sectionY++) {
            lighting.setSectionStatus(ChunkSectionPos.from(pos, sectionY), true);
            this.world.scheduleBlockRenders(pos.x, sectionY, pos.z);
        }
    }

    /** Applique un changement de bloc relaye par le serveur. */
    public void updateBlock(BlockPos pos, BlockState state) {
        this.world.handleBlockUpdate(pos, state, Block.NOTIFY_ALL | Block.FORCE_STATE);
    }

    /**
     * Applique un paquet d'entity vanilla a ce monde, avec le gestionnaire de paquets rebranche dessus le
     * temps du traitement : apparition, deplacement, donnees, equipement, passagers, effets, animations.
     */
    public void applyEntityPacket(MinecraftClient client, Packet<? super ClientPlayPacketListener> packet) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (this.mirror || handler == null) {
            return;
        }
        if (packet instanceof EntitiesDestroyS2CPacket destroy) {
            IntList ids = destroy.getEntityIds();
            for (int i = 0; i < ids.size(); i++) {
                this.world.removeEntity(ids.getInt(i), Entity.RemovalReason.DISCARDED);
            }
            return;
        }
        if (!ENTITY_PACKETS.contains(packet.getClass())) {
            return;
        }
        ClientPlayNetworkHandlerAccessor accessor = (ClientPlayNetworkHandlerAccessor) handler;
        ClientWorld bound = handler.getWorld();
        accessor.backrooms$setWorld(this.world);
        try {
            packet.apply(handler);
        } finally {
            accessor.backrooms$setWorld(bound);
        }
    }

    /** Heure et meteo sont celles du serveur, communes a toutes ses dimensions : le monde distant suit le monde principal. */
    public void syncEnvironment(ClientWorld main) {
        if (this.mirror) {
            return;
        }
        this.world.setTime(main.getTime());
        this.world.setTimeOfDay(main.getTimeOfDay());
        this.world.setRainGradient(main.getRainGradient(1.0f));
        this.world.setThunderGradient(main.getThunderGradient(1.0f));
    }

    /** Tick client : interpolation et animations des entities et block entities recues du serveur. */
    public void tickEntities() {
        if (!this.mirror) {
            this.world.tickEntities();
        }
    }

    /** Reconstruction de section demandee au renderer principal, repercutee sur la vue secondaire. */
    void scheduleChunkRender(int x, int y, int z, boolean important) {
        ((WorldRendererAccessor) this.renderer).backrooms$scheduleChunkRender(x, y, z, important);
    }

    /** Libere le renderer et son chunk builder. */
    void dispose() {
        this.renderer.setWorld(null);
        this.renderer.close();
    }

    /** Resume pour les logs de debug. */
    public String debugString() {
        return this.world.getChunkManager().getLoadedChunkCount() + " chunk(s) charge(s), "
                + this.world.getRegularEntityCount() + " entity(ies), renderer "
                + this.renderer.getChunksDebugString() + ", builder " + this.renderer.getChunkBuilder().getDebugString();
    }

    /** La carte de chunks du client est carree et centree : on la recentre sur le premier chunk hors de portee. */
    private void recenterIfNeeded(int chunkX, int chunkZ) {
        if (this.centerChunkX != Integer.MIN_VALUE
                && Math.abs(chunkX - this.centerChunkX) <= RemoteChunkWindow.MAP_RADIUS
                && Math.abs(chunkZ - this.centerChunkZ) <= RemoteChunkWindow.MAP_RADIUS) {
            return;
        }
        this.centerChunkX = chunkX;
        this.centerChunkZ = chunkZ;
        this.world.getChunkManager().setChunkMapCenter(chunkX, chunkZ);
        this.renderer.scheduleTerrainUpdate();
    }

    private void applyLight(
            LightingProvider lighting,
            int chunkX,
            int chunkZ,
            LightType type,
            BitSet inited,
            BitSet uninited,
            Iterator<byte[]> nibbles) {
        for (int index = 0; index < lighting.getHeight(); index++) {
            int sectionY = lighting.getBottomY() + index;
            boolean hasData = inited.get(index);
            if (!hasData && !uninited.get(index)) {
                continue;
            }
            lighting.enqueueSectionData(type, ChunkSectionPos.from(chunkX, sectionY, chunkZ),
                    hasData ? new ChunkNibbleArray(nibbles.next().clone()) : new ChunkNibbleArray());
            this.world.scheduleBlockRenders(chunkX, sectionY, chunkZ);
        }
    }
}
