package com.petassegang.addons.backrooms.level.level0.generation.write;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

/**
 * Etape d'ecriture des neons du Level 0.
 */
public final class LevelZeroLightWriteStage implements LevelZeroWriteStage {

    private final LevelZeroBlockPalette blockPalette;

    /**
     * Construit l'etape d'ecriture des neons.
     *
     * @param blockPalette palette de blocs du Level 0
     */
    public LevelZeroLightWriteStage(LevelZeroBlockPalette blockPalette) {
        this.blockPalette = blockPalette;
    }

    @Override
    public void initializeColumnSample(BlockState[] states) {
    }

    @Override
    public void writeChunkColumn(Chunk chunk,
                                 BlockPos.Mutable mutablePos,
                                 int localX,
                                 int localZ,
                                 LevelZeroResolvedColumn resolvedColumn) {
        if (!shouldPlaceLight(resolvedColumn)) {
            return;
        }
        chunk.setBlockState(mutablePos.set(localX, resolvedColumn.verticalSlice().ceilingY(), localZ),
                blockPalette.lightFixture(),
                false);
    }

    @Override
    public void writeColumnSample(BlockState[] states, LevelZeroResolvedColumn resolvedColumn) {
        if (!shouldPlaceLight(resolvedColumn)) {
            return;
        }
        int y = resolvedColumn.verticalSlice().ceilingY();
        if (y >= 0 && y < states.length) {
            states[y] = blockPalette.lightFixture();
        }
    }

    private static boolean shouldPlaceLight(LevelZeroResolvedColumn resolvedColumn) {
        return resolvedColumn.material().walkable() && resolvedColumn.cellState().lighted();
    }
}
