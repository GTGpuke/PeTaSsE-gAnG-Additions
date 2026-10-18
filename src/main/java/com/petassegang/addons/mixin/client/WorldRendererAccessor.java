package com.petassegang.addons.mixin.client;

import java.util.Set;
import java.util.SortedSet;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.BlockBreakingInfo;

/**
 * Acces aux etapes privees du rendu monde, pour les rejouer sur le renderer d'une vue de portail.
 */
@Mixin(WorldRenderer.class)
public interface WorldRendererAccessor {

    /** Prepare la grille et le culling du renderer pour une camera. */
    @Invoker("setupTerrain")
    void backrooms$setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator);

    /** Programme la construction des sections retenues. */
    @Invoker("updateChunks")
    void backrooms$updateChunks(Camera camera);

    /** Dessine une couche de blocs des sections retenues. */
    @Invoker("renderLayer")
    void backrooms$renderLayer(RenderLayer renderLayer, double x, double y, double z, Matrix4f positionMatrix, Matrix4f projectionMatrix);

    /** Dessine une entity comme le rendu monde vanilla. */
    @Invoker("renderEntity")
    void backrooms$renderEntity(
            Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    /** Programme la reconstruction d'une section. */
    @Invoker("scheduleChunkRender")
    void backrooms$scheduleChunkRender(int x, int y, int z, boolean important);

    /** Grille de sections du renderer, pour le parcours synchrone des sections d'une vue. */
    @Accessor("chunks")
    BuiltChunkStorage backrooms$getChunks();

    /** Chunk X du centre de la grille de sections, fixe par le dernier {@code setupTerrain}. */
    @Accessor("cameraChunkX")
    int backrooms$getCameraChunkX();

    /** Chunk Z du centre de la grille de sections, fixe par le dernier {@code setupTerrain}. */
    @Accessor("cameraChunkZ")
    int backrooms$getCameraChunkZ();

    /**
     * Sections a dessiner, dans l'ordre de rendu : celles du dernier {@code setupTerrain} pour le monde du joueur, celles du
     * parcours propre a la vue ({@code ViewSectionWalker}) pour une vue de portail.
     */
    @Accessor("builtChunks")
    ObjectArrayList<ChunkBuilder.BuiltChunk> backrooms$getBuiltChunks();

    /**
     * Tangage de camera, par pas de deux degres, du dernier {@code setupTerrain} qui a applique le frustum : une valeur
     * qu'aucune camera ne donne force vanilla a refaire sa liste de sections au suivant.
     */
    @Accessor("lastCameraPitch")
    void backrooms$setLastCameraPitch(double pitch);

    /** Block entities rendues sans culling de section (grands modeles) ; a parcourir sous {@code synchronized}. */
    @Accessor("noCullingBlockEntities")
    Set<BlockEntity> backrooms$getNoCullingBlockEntities();

    /** Fissures de minage en cours, par bloc. */
    @Accessor("blockBreakingProgressions")
    Long2ObjectMap<SortedSet<BlockBreakingInfo>> backrooms$getBlockBreakingProgressions();
}
