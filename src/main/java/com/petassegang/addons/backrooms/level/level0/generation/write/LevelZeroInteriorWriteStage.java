package com.petassegang.addons.backrooms.level.level0.generation.write;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

/**
 * Etape d'ecriture de l'interieur vertical d'une colonne.
 */
public final class LevelZeroInteriorWriteStage implements LevelZeroWriteStage {

    @Override
    public void initializeColumnSample(BlockState[] states) {
    }

    @Override
    public void writeChunkColumn(Chunk chunk,
                                 BlockPos.Mutable mutablePos,
                                 int localX,
                                 int localZ,
                                 LevelZeroResolvedColumn resolvedColumn) {
        if (resolvedColumn.material().walkable()) {
            return;
        }
        BlockState interior = resolvedColumn.material().interior();
        for (int y = resolvedColumn.verticalSlice().airMinY();
             y <= resolvedColumn.verticalSlice().airMaxY();
             y++) {
            chunk.setBlockState(mutablePos.set(localX, y, localZ), interior, false);
        }
    }

    @Override
    public void writeColumnSample(BlockState[] states, LevelZeroResolvedColumn resolvedColumn) {
        if (resolvedColumn.material().walkable()) {
            return;
        }

        BlockState interior = resolvedColumn.material().interior();
        for (int y = resolvedColumn.verticalSlice().airMinY();
             y <= resolvedColumn.verticalSlice().airMaxY();
             y++) {
            setColumnState(states, y, interior);
        }
    }

    private static void setColumnState(BlockState[] states, int y, BlockState state) {
        if (y >= 0 && y < states.length) {
            states[y] = state;
        }
    }
}
