package com.petassegang.addons.backrooms.portal.client.world;

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
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.backrooms.portal.client.interaction.CrossPortalTargeting;
import com.petassegang.addons.backrooms.portal.client.view.PortalRenderState;
import com.petassegang.addons.mixin.client.FrustumAccessor;
import com.petassegang.addons.mixin.client.WorldRendererAccessor;

/**
 * Dessine un {@link RemoteWorld} depuis une camera transposee : ciel, couches de blocs, entities, block
 * entities, fissures de minage, contour du bloc vise et nuages, avec le brouillard de sa dimension.
 *
 * <p>L'etat GL (stencil, profondeur) est pose par l'appelant, qui remet ensuite les dispatchers de rendu sur
 * le monde principal. Le frustum de culling se construit sur la projection normale :
 * {@code Frustum.coverBoxAroundSetPosition} recule le frustum le long de la troisieme ligne de la matrice et ne
 * termine jamais avec un plan proche oblique. La projection oblique ne sert qu'au dessin, ou elle coupe ce qui
 * est derriere le portail d'arrivee.
 */
public final class RemoteViewRenderer {

    /** Au-dela de cette distance au carre, les fissures de minage ne sont plus dessinees (comme vanilla). */
    private static final double BLOCK_DAMAGE_RANGE_SQUARED = 1024.0;
    private static final List<RenderLayer> BLOCK_LAYERS = RenderLayer.getBlockLayers();
    /** Pile de matrices reutilisee pour les entities et block entities ; vide entre deux rendus. */
    private static final MatrixStack MATRICES = new MatrixStack();
    private static final Matrix4f IDENTITY = new Matrix4f();

    private final RemoteWorld owner;
    private final ClientWorld world;
    private final WorldRenderer renderer;
    private final boolean mirror;
    /** Frustum du dernier rendu, reutilise d'une image a l'autre : vanilla ne le reinitialise que par constructeur. */
    private final Frustum frustum = new Frustum(IDENTITY, IDENTITY);
    private boolean frustumReady;
    private boolean terrainCenterChecked;
    /** Portee de vue (blocs) et brouillard epais poses par {@link #prepareFog} pour le rendu qui suit. */
    private float viewDistance;
    private boolean thickFog;

    RemoteViewRenderer(RemoteWorld owner) {
        this.owner = owner;
        this.world = owner.world();
        this.renderer = owner.renderer();
        this.mirror = owner.isMirror();
    }

    /** Frustum du dernier rendu, pour choisir les portails imbriques a dessiner ; {@code null} avant le premier rendu. */
    public @Nullable Frustum lastFrustum() {
        return this.frustumReady ? this.frustum : null;
    }

    /**
     * Pose couleur et portee du brouillard pour cette vue, depuis sa camera, comme le fait le rendu principal : le
     * fond du cadre et le terrain s'y fondent. A appeler avec {@code MinecraftClient.world} deja remplace par ce monde.
     */
    public void prepareFog(MinecraftClient client, Camera camera, float tickDelta) {
        this.viewDistance = client.gameRenderer.getViewDistance();
        this.thickFog = thickFog(client, this.world, camera);
        BackgroundRenderer.render(camera, tickDelta, this.world, client.options.getClampedViewDistance(), client.gameRenderer.getSkyDarkness(tickDelta));
        BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(this.viewDistance, 32.0f), this.thickFog, tickDelta);
    }

    /** Meme regle que {@code WorldRenderer.render} : brouillard epais de la dimension a la camera, ou impose par un boss. */
    private static boolean thickFog(MinecraftClient client, ClientWorld world, Camera camera) {
        BlockPos cameraBlock = camera.getBlockPos();
        return world.getDimensionEffects().useThickFog(cameraBlock.getX(), cameraBlock.getY()) || client.inGameHud.getBossBarHud().shouldThickenFog();
    }

    /**
     * Dessine blocs, entities et block entities depuis la camera distante.
     *
     * @param camera camera distante, position et rotation deja ecrites
     * @param cameraPos position de la camera distante
     * @param positionMatrix matrice de vue distante
     * @param cullingProjection projection du joueur, non modifiee, pour le choix des sections a dessiner
     * @param drawProjection projection distante a plan proche oblique, pour le dessin
     * @param tickDelta fraction de tick de l'image
     */
    public void render(
            MinecraftClient client,
            Camera camera,
            Vec3d cameraPos,
            Matrix4f positionMatrix,
            Matrix4f cullingProjection,
            Matrix4f drawProjection,
            float tickDelta) {
        PortalRenderState.setViewCameraPos(cameraPos);
        try {
            renderView(client, camera, cameraPos, positionMatrix, cullingProjection, drawProjection, tickDelta);
        } finally {
            PortalRenderState.setViewCameraPos(null);
        }
    }

    private void renderView(
            MinecraftClient client,
            Camera camera,
            Vec3d cameraPos,
            Matrix4f positionMatrix,
            Matrix4f cullingProjection,
            Matrix4f drawProjection,
            float tickDelta) {
        this.world.getChunkManager().getLightingProvider().doLightUpdates();
        ((FrustumAccessor) this.frustum).backrooms$init(positionMatrix, cullingProjection);
        this.frustum.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
        this.frustumReady = true;
        WorldRendererAccessor accessor = (WorldRendererAccessor) this.renderer;
        if (!this.mirror) {
            // Le terrain distant se centre sur la camera transposee ; la vue secondaire reste centree sur le joueur,
            // comme le renderer principal, pour ne pas redecouper sa carte de sections a chaque image.
            PortalRenderState.setCameraOverride(cameraPos);
        }
        try {
            // "Spectateur" : pas d'occlusion depuis l'interieur d'un bloc plein si la camera distante y tombe.
            accessor.backrooms$setupTerrain(camera, this.frustum, false, true);
        } finally {
            PortalRenderState.setCameraOverride(null);
        }
        if (!this.mirror) {
            checkTerrainCenter(accessor, cameraPos);
        }
        accessor.backrooms$updateChunks(camera);
        float viewDistance = this.viewDistance;
        boolean thickFog = this.thickFog;
        // Ciel de la vue avec son brouillard de ciel, puis brouillard du terrain, comme le rendu principal. Projection
        // normale pour le ciel : il est a l'infini, le stencil le confine au cadre, et le plan lointain oblique le couperait.
        // Le dome du ciel se dessine avec le shader courant : celui des positions, comme vanilla, et non le dernier pose par
        // le remplissage du cadre, dont l'attribut couleur absent du tampon vaudrait noir.
        RenderSystem.setShader(GameRenderer::getPositionProgram);
        this.renderer.renderSky(positionMatrix, cullingProjection, tickDelta, camera, thickFog,
                () -> BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_SKY, viewDistance, thickFog, tickDelta));
        BackgroundRenderer.applyFog(camera, BackgroundRenderer.FogType.FOG_TERRAIN, Math.max(viewDistance, 32.0f), thickFog, tickDelta);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        for (int i = 0; i < BLOCK_LAYERS.size(); i++) {
            RenderLayer layer = BLOCK_LAYERS.get(i);
            if (layer == RenderLayer.getTranslucent()) {
                // Meme ordre que vanilla : entities et block entities avant les couches translucides.
                renderEntities(client, camera, cameraPos, positionMatrix, drawProjection, tickDelta);
            }
            accessor.backrooms$renderLayer(layer, cameraPos.x, cameraPos.y, cameraPos.z, positionMatrix, drawProjection);
        }
        if (client.options.getCloudRenderModeValue() != CloudRenderMode.OFF) {
            this.renderer.renderClouds(MATRICES, positionMatrix, drawProjection, tickDelta, cameraPos.x, cameraPos.y, cameraPos.z);
        }
    }

    private void renderEntities(
            MinecraftClient client,
            Camera camera,
            Vec3d cameraPos,
            Matrix4f positionMatrix,
            Matrix4f drawProjection,
            float tickDelta) {
        EntityRenderDispatcher entityDispatcher = client.getEntityRenderDispatcher();
        BlockEntityRenderDispatcher blockEntityDispatcher = client.getBlockEntityRenderDispatcher();
        entityDispatcher.configure(this.world, camera, null);
        blockEntityDispatcher.configure(this.world, camera, null);
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        VertexSorter sorter = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(drawProjection, VertexSorter.BY_DISTANCE);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(positionMatrix);
        RenderSystem.applyModelViewMatrix();
        WorldRendererAccessor accessor = (WorldRendererAccessor) this.renderer;
        BufferBuilderStorage buffers = client.getBufferBuilders();
        VertexConsumerProvider.Immediate immediate = buffers.getEntityVertexConsumers();
        try {
            for (Entity entity : this.world.getEntities()) {
                if (!entityDispatcher.shouldRender(entity, this.frustum, cameraPos.x, cameraPos.y, cameraPos.z)) {
                    continue;
                }
                if (entity.age == 0) {
                    entity.lastRenderX = entity.getX();
                    entity.lastRenderY = entity.getY();
                    entity.lastRenderZ = entity.getZ();
                }
                accessor.backrooms$renderEntity(entity, cameraPos.x, cameraPos.y, cameraPos.z, tickDelta, MATRICES, immediate);
            }
            immediate.drawCurrentLayer();
            renderBlockEntities(accessor, blockEntityDispatcher, cameraPos, tickDelta, immediate);
            renderBlockDamage(client, accessor, cameraPos, buffers);
            renderTargetOutline(client, cameraPos, immediate);
            immediate.draw();
            buffers.getEffectVertexConsumers().draw();
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorter);
            // Les couches d'entities retablissent le culling : les quads de portail qui suivent se dessinent des deux cotes.
            RenderSystem.disableCull();
            // Un renderer d'entity qui a leve une exception laisse la pile sale : elle est partagee entre les vues.
            while (!MATRICES.isEmpty()) {
                MATRICES.pop();
            }
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
    private void renderBlockDamage(MinecraftClient client, WorldRendererAccessor accessor, Vec3d cameraPos, BufferBuilderStorage buffers) {
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
                    buffers.getEffectVertexConsumers().getBuffer(ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(infos.last().getStage())),
                    MATRICES.peek(),
                    1.0f);
            client.getBlockRenderManager().renderDamage(this.world.getBlockState(pos), pos, this.world, MATRICES, overlay);
            MATRICES.pop();
        }
    }

    /** Contour du bloc vise a travers un portail, quand le regard ressort dans cette vue. */
    private void renderTargetOutline(MinecraftClient client, Vec3d cameraPos, VertexConsumerProvider.Immediate immediate) {
        BlockHitResult hit = CrossPortalTargeting.blockHit();
        if (hit == null || CrossPortalTargeting.view() != this.owner || client.player == null || !PortalRenderState.blockOutlineAllowed()) {
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

    /** Verifie une fois que la substitution de camera du mixin a bien centre le terrain distant. */
    private void checkTerrainCenter(WorldRendererAccessor accessor, Vec3d cameraPos) {
        if (this.terrainCenterChecked) {
            return;
        }
        this.terrainCenterChecked = true;
        if (accessor.backrooms$getCameraChunkX() != ChunkSectionPos.getSectionCoord(cameraPos.x)
                || accessor.backrooms$getCameraChunkZ() != ChunkSectionPos.getSectionCoord(cameraPos.z)) {
            PortalRenderState.disableRemoteRendering("la substitution de camera de WorldRenderer.setupTerrain est inactive", null);
        }
    }
}
