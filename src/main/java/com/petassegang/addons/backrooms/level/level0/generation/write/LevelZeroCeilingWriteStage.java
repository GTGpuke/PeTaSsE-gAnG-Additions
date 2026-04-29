package com.petassegang.addons.backrooms.level.level0.generation.write;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

/**
 * Etape d'ecriture du plafond d'une colonne.
 */
public final class LevelZeroCeilingWriteStage implements LevelZeroWriteStage {

    @Override
    public void initializeColumnSample(BlockState[] states) {
    }

    @Override
    public void writeChunkColumn(Chunk chunk,
                                 BlockPos.Mutable mutablePos,
                                 int localX,
                                 int localZ,
                                 LevelZeroResolvedColumn resolvedColumn) {
        chunk.setBlockState(mutablePos.set(localX, resolvedColumn.verticalSlice().ceilingY(), localZ),
                resolvedColumn.material().ceiling(),
                false);
    }

    @Override
    public void writeColumnSample(BlockState[] states, LevelZeroResolvedColumn resolvedColumn) {
        setColumnState(states, resolvedColumn.verticalSlice().ceilingY(), resolvedColumn.material().ceiling());
    }

    private static void setColumnState(BlockState[] states, int y, BlockState state) {
        if (y >= 0 && y < states.length) {
            states[y] = state;
        }
    }
}
