package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.render.BuiltChunkStorage;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;

/**
 * Section de la grille de rendu qui couvre une position, pour le parcours synchrone des sections d'une vue.
 */
@Mixin(BuiltChunkStorage.class)
public interface BuiltChunkStorageAccessor {

    /** Section stockee a l'emplacement de la grille pour cette position ; son origine est ailleurs si la grille ne couvre pas la position. */
    @Invoker("getRenderedChunk")
    @Nullable ChunkBuilder.BuiltChunk backrooms$getRenderedChunk(BlockPos pos);
}
