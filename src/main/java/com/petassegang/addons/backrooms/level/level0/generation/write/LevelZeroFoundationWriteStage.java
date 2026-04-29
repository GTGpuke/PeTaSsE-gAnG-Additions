package com.petassegang.addons.backrooms.level.level0.generation.write;

import java.util.Arrays;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

/**
 * Etape d'ecriture des couches fixes du Level 0.
 */
public final class LevelZeroFoundationWriteStage implements LevelZeroWriteStage {

    private final BlockState bedrockState;
    private final BlockState subfloorState;

    /**
     * Construit l'etape des couches fixes.
     *
     * @param blockPalette palette de blocs
     */
    public LevelZeroFoundationWriteStage(LevelZeroBlockPalette blockPalette) {
        this.bedrockState = blockPalette.bedrock();
        this.subfloorState = blockPalette.subfloor();
    }

    @Override
    public void initializeColumnSample(BlockState[] states) {
        Arrays.fill(states, Blocks.AIR.getDefaultState());
    }

    @Override
    public void writeChunkColumn(Chunk chunk,
                                 BlockPos.Mutable mutablePos,
                                 int localX,
                                 int localZ,
                                 LevelZeroResolvedColumn resolvedColumn) {
        int bedrockMinY = resolvedColumn.verticalSlice().bedrockMinY();
        int bedrockMaxY = resolvedColumn.verticalSlice().bedrockMaxY();
        int subfloorY = resolvedColumn.verticalSlice().subfloorY();
        for (int y = bedrockMinY;
             y <= bedrockMaxY;
             y++) {
            chunk.setBlockState(mutablePos.set(localX, y, localZ), bedrockState, false);
        }
        if (subfloorY >= 0) {
            chunk.setBlockState(mutablePos.set(localX, subfloorY, localZ),
                    subfloorState,
                    false);
        }
    }

    @Override
    public void writeColumnSample(BlockState[] states, LevelZeroResolvedColumn resolvedColumn) {
        int bedrockMinY = resolvedColumn.verticalSlice().bedrockMinY();
        int bedrockMaxY = resolvedColumn.verticalSlice().bedrockMaxY();
        int subfloorY = resolvedColumn.verticalSlice().subfloorY();
        for (int y = bedrockMinY;
             y <= bedrockMaxY;
             y++) {
            setColumnState(states, y, bedrockState);
        }
        if (subfloorY >= 0) {
            setColumnState(states, subfloorY, subfloorState);
        }
    }

    private static void setColumnState(BlockState[] states, int y, BlockState state) {
        if (y >= 0 && y < states.length) {
            states[y] = state;
        }
    }
}
