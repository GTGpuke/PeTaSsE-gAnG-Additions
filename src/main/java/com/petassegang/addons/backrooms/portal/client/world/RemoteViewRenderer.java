package com.petassegang.addons.backrooms.portal.client.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.backrooms.portal.client.interaction.RemoteTargeting;
import com.petassegang.addons.backrooms.portal.client.view.PortalClipping;
import com.petassegang.addons.backrooms.portal.client.view.PortalRenderState;
import com.petassegang.addons.backrooms.portal.client.view.PortalStencilRenderer;
import com.petassegang.addons.backrooms.portal.client.view.ViewCone;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.mixin.client.FrustumAccessor;
import com.petassegang.addons.mixin.client.WorldRendererAccessor;

/**
 * Dessine un {@link RemoteWorld} depuis une camera transposee : ciel, couches de blocs, entities, block
 * entities, fissures de minage, contour du bloc vise et nuages, avec le brouillard de sa dimension.
 *
 * <p>L'etat GL (stencil, profondeur) est pose par l'appelant, qui remet ensuite les dispatchers de rendu sur le
 * monde principal. Tout se dessine avec la projection du joueur ; le plan du portail d'arrivee coupe ce qui est
 * derriere lui par les shaders ({@link PortalClipping}). Les sections a dessiner sont choisies par un parcours
 * synchrone propre a la vue ({@link ViewSectionWalker}), limite au volume visible a travers le cadre
 * ({@link ViewCone}) ; la grille de sections du renderer est ancree sur l'arrivee du portail, pas sur la camera.
 */
public final class RemoteViewRenderer {

    /** Au-dela de cette distance au carre, les fissures de minage ne sont plus dessinees (comme vanilla). */
    private static final double BLOCK_DAMAGE_RANGE_SQUARED = 1024.0;
    /** Cote, en blocs, de la cellule autour de la camera que le frustum doit couvrir : le recul de vanilla. */
    private static final int FRUSTUM_COVER_BOX = 8;
    /**
     * La grille de sections d'un renderer ne se recentre que si l'ancrage s'eloigne de plus de ce nombre de chunks de
     * son centre : deux portails voisins vers la meme dimension, ou le retour juste apres une traversee, la partagent
     * sans la redecouper (chaque recentrage remet des colonnes entieres de sections a reconstruire).
     */
    static final int GRID_MARGIN_CHUNKS = 2;
    private static final List<RenderLayer> BLOCK_LAYERS = RenderLayer.getBlockLayers();
    /**
     * Entities engagees dans le cadre d'arrivee d'une vue imbriquee dans une autre vue a travers un cadre, dessinees apres
     * les autres sans la decoupe : leur partie encore derriere l'arrivee ne se dessine nulle part ailleurs a ce niveau.
     */
    private static final List<Entity> STRADDLING = new ArrayList<>();
    /** Plans empiles dans une vue imbriquee dans une autre vue a travers un cadre. */
    private static final int NESTED_DEPTH = 2;
    /** Pile de matrices reutilisee pour les entities et block entities ; vide entre deux rendus. */
    private static final MatrixStack MATRICES = new MatrixStack();
    /**
     * Tampons des vues, distincts de ceux du monde principal : les vues se dessinent au milieu du rendu principal, dont
     * les couches translucides attendent encore dans les siens.
     */
    private static final VertexConsumerProvider.Immediate ENTITY_BUFFERS = VertexConsumerProvider.immediate(new BufferAllocator(786_432));
    private static final VertexConsumerProvider.Immediate EFFECT_BUFFERS = VertexConsumerProvider.immediate(new BufferAllocator(1536));
    private static final Matrix4f IDENTITY = new Matrix4f();

    private final RemoteWorld owner;
    private final ClientWorld world;
    private final WorldRenderer renderer;
    /** Frustum du rendu, reutilise d'une image a l'autre : vanilla ne le reinitialise que par constructeur. */
    private final Frustum frustum = new Frustum(IDENTITY, IDENTITY);
    private final ViewCone cone = new ViewCone();
    private final ViewSectionWalker sectionWalker = new ViewSectionWalker();
    /** Brouillard du ciel, passe au rendu du ciel : cree une fois, il lit la camera et l'instant poses avant chaque dessin. */
    private final Runnable skyFog = this::applySkyFog;
    /** Camera et fraction de tick du ciel en cours de dessin, lues par {@link #skyFog}. */
    private Camera skyFogCamera;
    private float skyFogTickDelta;
    /** Image ou ce renderer a prepare son terrain pour la derniere fois : une seule fois par image, pour sa premiere vue. */
    private int terrainFrame = -1;
    private boolean terrainCenterChecked;
    /** Portee de vue (blocs) et brouillard epais poses par {@link #prepareFog} pour le rendu qui suit. */
    private float viewDistance;
    private boolean thickFog;

    RemoteViewRenderer(RemoteWorld owner) {
        this.owner = owner;
        this.world = owner.world();
        this.renderer = owner.renderer();
    }

    /**
     * Pose couleur et portee du brouillard pour cette vue, depuis sa camera, comme le fait le rendu principal : le
     * fond du cadre et le terrain s'y fondent. A appeler avec {@code MinecraftClient.world} deja remplace par ce monde.
     */
    public void prepareFog(MinecraftClient client, Camera camera, float tickDelta) {
        this.viewDistance = client.gameRenderer.getViewDistance();
        this.thickFog = thickFog(client, this.world, camera);
        BackgroundRenderer.render(camera, tickDelta, this.world, client.options.getClampedViewDistance(), client.gameRenderer.getSkyDarkness(tickDelta));
        // BackgroundRenderer.render calcule la couleur, applyFogColor la donne aux shaders : sans lui, la vue se fondrait dans la
        // couleur du brouillard du monde du joueur (le noir du Level 0 sur l'horizon de l'Overworld).
        BackgroundRenderer.applyFogColor();
        BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(this.viewDistance, 32.0f), this.thickFog, tickDelta);
    }

    /** Meme regle que {@code WorldRenderer.render} : brouillard epais de la dimension a la camera, ou impose par un boss. */
    private static boolean thickFog(MinecraftClient client, ClientWorld world, Camera camera) {
        BlockPos cameraBlock = camera.getBlockPos();
        return world.getDimensionEffects().useThickFog(cameraBlock.getX(), cameraBlock.getY()) || client.inGameHud.getBossBarHud().shouldThickenFog();
    }

    /**
     * Dessine la vue a travers un portail depuis la camera transposee.
     *
     * @param client client, dont le monde et le renderer sont deja ceux de la vue
     * @param camera camera de la vue, position et rotation deja ecrites
     * @param cameraPos position de la camera de la vue
     * @param positionMatrix matrice de vue de cette camera
     * @param projection projection du joueur
     * @param portal portail dont c'est la vue : son arrivee donne le plan de decoupe, le volume visible et l'ancrage de la grille
     * @param frameCameraPos camera qui regarde le cadre, dans le monde du portail, pour une vue a travers lui ; {@code null}
     *                       quand la camera elle-meme est passee de l'autre cote (troisieme personne) : elle voit alors
     *                       tout, sans plan de decoupe ni volume limite
     * @param lightmap lightmap deja recalculee pour ce monde
     * @param tickDelta fraction de tick de l'image
     */
    public void render(
            MinecraftClient client,
            Camera camera,
            Vec3d cameraPos,
            Matrix4f positionMatrix,
            Matrix4f projection,
            BackroomsPortalEntity portal,
            @Nullable Vec3d frameCameraPos,
            LightmapTextureManager lightmap,
            float tickDelta) {
        boolean throughFrame = frameCameraPos != null;
        PortalRenderState.setViewCameraPos(cameraPos);
        this.owner.markViewed(cameraPos);
        PortalFacing arrival = portal.facingDestination();
        if (throughFrame) {
            PortalClipping.push(arrival.normal, portal.destinationPos(), cameraPos, positionMatrix);
            this.cone.set(cameraPos, portal.destinationPos(), arrival.axisW, arrival.axisH, arrival.normal, portal.width() / 2.0, portal.height() / 2.0);
        } else {
            this.cone.unbounded();
        }
        try {
            renderView(client, camera, cameraPos, positionMatrix, projection, portal, frameCameraPos, lightmap, tickDelta);
        } finally {
            if (throughFrame) {
                PortalClipping.pop();
            }
            PortalRenderState.setViewCameraPos(null);
        }
    }

    private void renderView(
            MinecraftClient client,
            Camera camera,
            Vec3d cameraPos,
            Matrix4f positionMatrix,
            Matrix4f projection,
            BackroomsPortalEntity portal,
            @Nullable Vec3d frameCameraPos,
            LightmapTextureManager lightmap,
            float tickDelta) {
        this.world.getChunkManager().getLightingProvider().doLightUpdates();
        ((FrustumAccessor) this.frustum).backrooms$init(positionMatrix, projection);
        this.frustum.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        // Comme vanilla, le frustum recule jusqu'a couvrir la cellule de la camera : les sections au bord ne clignotent pas.
        this.frustum.coverBoxAroundSetPosition(FRUSTUM_COVER_BOX);
        WorldRendererAccessor accessor = (WorldRendererAccessor) this.renderer;
        setupTerrain(accessor, camera, cameraPos, portal.destinationPos());
        accessor.backrooms$updateChunks(camera);
        // Ciel de la vue avec son brouillard de ciel, puis brouillard du terrain, comme le rendu principal. Le dome du ciel se
        // dessine avec le shader courant : celui des positions, comme vanilla, et non le dernier pose par le remplissage du
        // cadre, dont l'attribut couleur absent du tampon vaudrait noir.
        RenderSystem.setShader(GameRenderer::getPositionProgram);
        this.skyFogCamera = camera;
        this.skyFogTickDelta = tickDelta;
        this.renderer.renderSky(positionMatrix, projection, tickDelta, camera, this.thickFog, this.skyFog);
        BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(this.viewDistance, 32.0f), this.thickFog, tickDelta);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        for (int i = 0; i < BLOCK_LAYERS.size(); i++) {
            RenderLayer layer = BLOCK_LAYERS.get(i);
            if (layer == RenderLayer.getTranslucent()) {
                // Meme ordre que vanilla : entities et block entities avant les couches translucides.
                renderEntities(client, camera, cameraPos, positionMatrix, projection, portal, frameCameraPos, tickDelta);
            }
            accessor.backrooms$renderLayer(layer, cameraPos.x, cameraPos.y, cameraPos.z, positionMatrix, projection);
        }
        // Meme ordre que vanilla : les particules apres les couches translucides, avant les nuages.
        renderParticles(client, camera, positionMatrix, lightmap, tickDelta);
        if (client.options.getCloudRenderModeValue() != CloudRenderMode.OFF) {
            this.renderer.renderClouds(MATRICES, positionMatrix, projection, tickDelta, cameraPos.x, cameraPos.y, cameraPos.z);
        }
    }

    private void applySkyFog() {
        BackgroundRenderer.applyFog(this.skyFogCamera, BackgroundRenderer.FogType.FOG_SKY, this.viewDistance, this.thickFog, this.skyFogTickDelta);
    }

    /**
     * Prepare le terrain de la vue. La premiere vue de ce renderer dans l'image passe par le {@code setupTerrain}
     * vanilla, avec la position du joueur remplacee par le centre de grille retenu : la grille se place, le chunk
     * builder connait sa camera, et le graphe de culling vanilla reste chaud pour servir tel quel si ce renderer
     * devient le principal a la traversee. Les vues suivantes du meme renderer n'y touchent plus : la grille reste ou
     * elle est. Dans tous les cas, les sections dessinees viennent du parcours synchrone propre a la vue.
     */
    private void setupTerrain(WorldRendererAccessor accessor, Camera camera, Vec3d cameraPos, Vec3d anchor) {
        int frame = PortalRenderState.frameIndex();
        if (this.terrainFrame != frame) {
            this.terrainFrame = frame;
            int centerX = gridCenter(accessor.backrooms$getCameraChunkX(), ChunkSectionPos.getSectionCoord(anchor.x));
            int centerZ = gridCenter(accessor.backrooms$getCameraChunkZ(), ChunkSectionPos.getSectionCoord(anchor.z));
            PortalRenderState.setTerrainCenter(ChunkSectionPos.getOffsetPos(centerX, ViewSectionWalker.HALF_SECTION), anchor.y,
                    ChunkSectionPos.getOffsetPos(centerZ, ViewSectionWalker.HALF_SECTION));
            try {
                // "Spectateur" : pas d'occlusion depuis l'interieur d'un bloc plein si la camera y tombe.
                accessor.backrooms$setupTerrain(camera, this.frustum, false, true);
            } finally {
                PortalRenderState.clearTerrainCenter();
            }
            checkTerrainCenter(accessor, centerX, centerZ);
        } else {
            this.renderer.getChunkBuilder().setCameraPosition(cameraPos);
        }
        this.sectionWalker.discover(accessor.backrooms$getChunks(), this.world.getChunkManager(), camera.getBlockPos(), this.frustum, this.cone,
                accessor.backrooms$getBuiltChunks());
    }

    /**
     * Centre de grille retenu pour un ancrage : l'ancien tant que l'ancrage en reste a {@link #GRID_MARGIN_CHUNKS} pres,
     * l'ancrage sinon (et toujours lui pour une grille jamais centree).
     */
    static int gridCenter(int current, int anchor) {
        return current != Integer.MIN_VALUE && Math.abs(anchor - current) <= GRID_MARGIN_CHUNKS ? current : anchor;
    }

    /** Verifie une fois que la substitution de position du mixin a bien centre la grille la ou on l'a demande. */
    private void checkTerrainCenter(WorldRendererAccessor accessor, int centerX, int centerZ) {
        if (this.terrainCenterChecked) {
            return;
        }
        this.terrainCenterChecked = true;
        if (accessor.backrooms$getCameraChunkX() != centerX || accessor.backrooms$getCameraChunkZ() != centerZ) {
            PortalRenderState.disableRemoteRendering("la substitution de position de WorldRenderer.setupTerrain est inactive", null);
        }
    }

    private void renderEntities(
            MinecraftClient client,
            Camera camera,
            Vec3d cameraPos,
            Matrix4f positionMatrix,
            Matrix4f projection,
            BackroomsPortalEntity portal,
            @Nullable Vec3d frameCameraPos,
            float tickDelta) {
        EntityRenderDispatcher entityDispatcher = client.getEntityRenderDispatcher();
        BlockEntityRenderDispatcher blockEntityDispatcher = client.getBlockEntityRenderDispatcher();
        entityDispatcher.configure(this.world, camera, null);
        blockEntityDispatcher.configure(this.world, camera, null);
        PortalStencilRenderer.applyEntityLighting(this.world);
        Matrix4f previousProjection = RenderSystem.getProjectionMatrix();
        VertexSorter sorter = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(projection, VertexSorter.BY_DISTANCE);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(positionMatrix);
        RenderSystem.applyModelViewMatrix();
        WorldRendererAccessor accessor = (WorldRendererAccessor) this.renderer;
        VertexConsumerProvider.Immediate immediate = ENTITY_BUFFERS;
        try {
            for (Entity entity : this.world.getEntities()) {
                if (!entityDispatcher.shouldRender(entity, this.frustum, cameraPos.x, cameraPos.y, cameraPos.z)
                        || !this.cone.intersects(entity.getVisibilityBoundingBox())) {
                    continue;
                }
                if (entity.age == 0) {
                    entity.lastRenderX = entity.getX();
                    entity.lastRenderY = entity.getY();
                    entity.lastRenderZ = entity.getZ();
                }
                // Vue qui couvre l'ecran : ce qui est coupe par un portail de ce monde s'y dessine en deux moities, plus bas.
                if (frameCameraPos == null && StraddlingEntityRenderer.drawnInParts(entity, tickDelta)) {
                    continue;
                }
                // Vue imbriquee : ce qui sort de son arrivee n'a nulle part ou dessiner la partie encore derriere, coupee au
                // plan, son modele s'ouvrirait ; il se dessine entier, dans le cadre.
                if (PortalClipping.depth() >= NESTED_DEPTH && StraddlingEntityRenderer.straddlesArrival(portal, entity, tickDelta)) {
                    STRADDLING.add(entity);
                    continue;
                }
                accessor.backrooms$renderEntity(entity, cameraPos.x, cameraPos.y, cameraPos.z, tickDelta, MATRICES, immediate);
            }
            if (frameCameraPos != null) {
                // Ce qui entre dans le cadre de ce cote : la partie deja passee, ici, au-dela du plan d'arrivee.
                StraddlingEntityRenderer.renderEnteringParts(client, accessor, portal, frameCameraPos, tickDelta, immediate);
            } else {
                StraddlingEntityRenderer.renderWorldParts(client, this.world, cameraPos, positionMatrix, tickDelta);
            }
            immediate.drawCurrentLayer();
            renderBlockEntities(accessor, blockEntityDispatcher, cameraPos, tickDelta, immediate);
            renderBlockDamage(client, accessor, cameraPos);
            renderTargetOutline(client, cameraPos, immediate);
            immediate.draw();
            renderStraddling(accessor, cameraPos, tickDelta, immediate);
            EFFECT_BUFFERS.draw();
        } finally {
            STRADDLING.clear();
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(previousProjection, sorter);
            // Etat vanilla par defaut, que les couches de terrain supposent sans le poser : faces arriere eliminees.
            RenderSystem.enableCull();
            // Un renderer d'entity qui a leve une exception laisse la pile sale : elle est partagee entre les vues.
            while (!MATRICES.isEmpty()) {
                MATRICES.pop();
            }
        }
    }

    /**
     * Les entities engagees dans le cadre d'arrivee d'une vue imbriquee, entieres : la partie encore devant le plan reste
     * dans le cadre (le stencil l'y limite), la ou on la voit sortir du portail.
     */
    private static void renderStraddling(WorldRendererAccessor accessor, Vec3d cameraPos, float tickDelta, VertexConsumerProvider.Immediate immediate) {
        if (STRADDLING.isEmpty()) {
            return;
        }
        PortalClipping.suspend();
        try {
            for (int i = 0; i < STRADDLING.size(); i++) {
                accessor.backrooms$renderEntity(STRADDLING.get(i), cameraPos.x, cameraPos.y, cameraPos.z, tickDelta, MATRICES, immediate);
            }
            immediate.draw();
        } finally {
            PortalClipping.resume();
        }
    }

    /** Particules du monde de la vue, depuis sa camera : la matrice de vue est empilee comme vanilla le fait pour les siennes. */
    private void renderParticles(MinecraftClient client, Camera camera, Matrix4f positionMatrix, LightmapTextureManager lightmap, float tickDelta) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(positionMatrix);
        RenderSystem.applyModelViewMatrix();
        try {
            this.owner.renderParticles(client, lightmap, camera, tickDelta);
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.enableCull();
        }
    }

    private void renderBlockEntities(
            WorldRendererAccessor accessor,
            BlockEntityRenderDispatcher dispatcher,
            Vec3d cameraPos,
            float tickDelta,
            VertexConsumerProvider.Immediate immediate) {
        ObjectArrayList<ChunkBuilder.BuiltChunk> builtChunks = accessor.backrooms$getBuiltChunks();
        for (int i = 0; i < builtChunks.size(); i++) {
            List<BlockEntity> blockEntities = builtChunks.get(i).getData().getBlockEntities();
            for (int j = 0; j < blockEntities.size(); j++) {
                renderBlockEntity(dispatcher, blockEntities.get(j), cameraPos, tickDelta, immediate);
            }
        }
        Set<BlockEntity> noCulling = accessor.backrooms$getNoCullingBlockEntities();
        synchronized (noCulling) {
            for (BlockEntity blockEntity : noCulling) {
                renderBlockEntity(dispatcher, blockEntity, cameraPos, tickDelta, immediate);
            }
        }
    }

    private static void renderBlockEntity(
            BlockEntityRenderDispatcher dispatcher,
            BlockEntity blockEntity,
            Vec3d cameraPos,
            float tickDelta,
            VertexConsumerProvider vertexConsumers) {
        BlockPos pos = blockEntity.getPos();
        MATRICES.push();
        MATRICES.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
        dispatcher.render(blockEntity, tickDelta, MATRICES, vertexConsumers);
        MATRICES.pop();
    }

    /** Fissures des blocs en cours de minage a travers un portail (voir {@code WorldRenderer.setBlockBreakingInfo}). */
    private void renderBlockDamage(MinecraftClient client, WorldRendererAccessor accessor, Vec3d cameraPos) {
        Long2ObjectMap<SortedSet<BlockBreakingInfo>> progressions = accessor.backrooms$getBlockBreakingProgressions();
        if (progressions.isEmpty()) {
            return;
        }
        for (Long2ObjectMap.Entry<SortedSet<BlockBreakingInfo>> entry : progressions.long2ObjectEntrySet()) {
            SortedSet<BlockBreakingInfo> infos = entry.getValue();
            if (infos == null || infos.isEmpty()) {
                continue;
            }
            BlockPos pos = BlockPos.fromLong(entry.getLongKey());
            if (pos.getSquaredDistance(cameraPos) > BLOCK_DAMAGE_RANGE_SQUARED) {
                continue;
            }
            MATRICES.push();
            MATRICES.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
            VertexConsumer overlay = new OverlayVertexConsumer(
                    EFFECT_BUFFERS.getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(infos.last().getStage())),
                    MATRICES.peek(),
                    1.0f);
            client.getBlockRenderManager().renderDamage(this.world.getBlockState(pos), pos, this.world, MATRICES, overlay);
            MATRICES.pop();
        }
    }

    /** Contour du bloc vise a travers un portail, quand le regard ressort dans cette vue. */
    private void renderTargetOutline(MinecraftClient client, Vec3d cameraPos, VertexConsumerProvider.Immediate immediate) {
        BlockHitResult hit = RemoteTargeting.blockHit();
        if (hit == null || RemoteTargeting.view() != this.owner || client.player == null || !PortalRenderState.blockOutlineAllowed()) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = this.world.getBlockState(pos);
        if (state.isAir() || !this.world.getWorldBorder().contains(pos)) {
            return;
        }
        // Pas drawBlockOutline : Fabric API l'intercepte avec un contexte de rendu qui n'existe que dans WorldRenderer.render.
        WorldRenderer.drawShapeOutline(MATRICES, immediate.getBuffer(RenderLayer.getLines()),
                state.getOutlineShape(this.world, pos, ShapeContext.of(client.player)),
                pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z, 0.0f, 0.0f, 0.0f, 0.4f, false);
    }
}
