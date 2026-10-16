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

    @Invoker("setupTerrain")
    void backrooms$setupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator);

    @Invoker("updateChunks")
    void backrooms$updateChunks(Camera camera);

    @Invoker("renderLayer")
    void backrooms$renderLayer(RenderLayer renderLayer, double x, double y, double z, Matrix4f positionMatrix, Matrix4f projectionMatrix);

    @Invoker("renderEntity")
    void backrooms$renderEntity(
            Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    @Invoker("scheduleChunkRender")
    void backrooms$scheduleChunkRender(int x, int y, int z, boolean important);

    @Accessor("cameraChunkX")
    int backrooms$getCameraChunkX();

    @Accessor("cameraChunkZ")
    int backrooms$getCameraChunkZ();

    /** Sections retenues par le dernier {@code setupTerrain}, dans l'ordre de rendu. */
    @Accessor("builtChunks")
    ObjectArrayList<ChunkBuilder.BuiltChunk> backrooms$getBuiltChunks();

    /** Block entities rendues sans culling de section (grands modeles) ; a parcourir sous {@code synchronized}. */
    @Accessor("noCullingBlockEntities")
    Set<BlockEntity> backrooms$getNoCullingBlockEntities();

    @Accessor("blockBreakingProgressions")
    Long2ObjectMap<SortedSet<BlockBreakingInfo>> backrooms$getBlockBreakingProgressions();
}
