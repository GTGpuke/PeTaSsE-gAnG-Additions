package com.gtgpuke.gtgsbackrooms.mixin.client;

import java.util.Set;
import java.util.SortedSet;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.ChunkRenderingDataPreparer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
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

    /**
     * Prepare les sections a dessiner pour une camera : la version qu'un autre mod a mise a la place de celle de vanilla
     * quand il dessine le terrain ({@code RemoteViewRenderer.setupForeignTerrain}) ; vanilla, elle centre sa grille sur
     * le joueur et les vues ne l'appellent pas.
     */
    @Invoker("setupTerrain")
    void backrooms$setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator);

    /** Programme la construction des sections retenues. */
    @Invoker("updateChunks")
    void backrooms$updateChunks(Camera camera);

    /** Pluie et neige autour d'une camera, dans le monde du client, animees par l'horloge du renderer. */
    @Invoker("renderWeather")
    void backrooms$renderWeather(LightmapTextureManager manager, float tickDelta, double cameraX, double cameraY, double cameraZ);

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

    /** Chunk Y du centre de la grille de sections. */
    @Accessor("cameraChunkY")
    int backrooms$getCameraChunkY();

    /** Chunk Z du centre de la grille de sections. */
    @Accessor("cameraChunkZ")
    int backrooms$getCameraChunkZ();

    @Accessor("cameraChunkX")
    void backrooms$setCameraChunkX(int chunkX);

    @Accessor("cameraChunkY")
    void backrooms$setCameraChunkY(int chunkY);

    @Accessor("cameraChunkZ")
    void backrooms$setCameraChunkZ(int chunkZ);

    /** Graphe de culling vanilla du renderer, asynchrone, que la vue fait avancer elle-meme ({@code RemoteViewRenderer}). */
    @Accessor("chunkRenderingDataPreparer")
    ChunkRenderingDataPreparer backrooms$getChunkRenderingDataPreparer();

    /** Cellule de huit blocs de la camera du dernier placement : le graphe repart d'elle quand elle change. */
    @Accessor("lastCameraX")
    double backrooms$getLastCameraX();

    @Accessor("lastCameraX")
    void backrooms$setLastCameraX(double cell);

    @Accessor("lastCameraY")
    double backrooms$getLastCameraY();

    @Accessor("lastCameraY")
    void backrooms$setLastCameraY(double cell);

    @Accessor("lastCameraZ")
    double backrooms$getLastCameraZ();

    @Accessor("lastCameraZ")
    void backrooms$setLastCameraZ(double cell);

    /** Distance de rendu de la grille : si celle des options change, le renderer se recharge (comme vanilla). */
    @Accessor("viewDistance")
    int backrooms$getViewDistance();

    /** Refait la liste des sections a dessiner depuis le graphe de culling, sous ce frustum. */
    @Invoker("applyFrustum")
    void backrooms$applyFrustum(Frustum frustum);

    /**
     * Sections a dessiner, dans l'ordre de rendu : celles du dernier {@code setupTerrain} pour le monde du joueur, celles du
     * parcours propre a la vue ({@code ViewSectionWalker}) pour une vue de portail.
     */
    @Accessor("builtChunks")
    ObjectArrayList<ChunkBuilder.BuiltChunk> backrooms$getBuiltChunks();

    /**
     * Tangage de camera, par pas de deux degres, du dernier placement qui a applique le frustum : une valeur qu'aucune
     * camera ne donne force vanilla a refaire sa liste de sections au suivant.
     */
    @Accessor("lastCameraPitch")
    void backrooms$setLastCameraPitch(double pitch);

    @Accessor("lastCameraPitch")
    double backrooms$getLastCameraPitch();

    /** Lacet de camera, par pas de deux degres, du dernier placement qui a applique le frustum. */
    @Accessor("lastCameraYaw")
    double backrooms$getLastCameraYaw();

    @Accessor("lastCameraYaw")
    void backrooms$setLastCameraYaw(double yaw);

    /** Horloge du renderer, en ticks : elle place les nuages et anime la pluie et la neige. */
    @Accessor("ticks")
    int backrooms$getTicks();

    @Accessor("ticks")
    void backrooms$setTicks(int ticks);

    /** Block entities rendues sans culling de section (grands modeles) ; a parcourir sous {@code synchronized}. */
    @Accessor("noCullingBlockEntities")
    Set<BlockEntity> backrooms$getNoCullingBlockEntities();

    /** Fissures de minage en cours, par bloc. */
    @Accessor("blockBreakingProgressions")
    Long2ObjectMap<SortedSet<BlockBreakingInfo>> backrooms$getBlockBreakingProgressions();

    @Accessor("bufferBuilders")
    BufferBuilderStorage backrooms$getBufferBuilders();

    @Mutable
    @Accessor("bufferBuilders")
    void backrooms$setBufferBuilders(BufferBuilderStorage bufferBuilders);
}
