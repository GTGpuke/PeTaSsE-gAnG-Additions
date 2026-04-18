package com.petassegang.addons.world.backrooms.level0.write;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroVerticalLayout;

/**
 * Etape d'ecriture des couches fixes du Level 0.
 */
public final class LevelZeroFoundationWriteStage implements LevelZeroWriteStage {

    private final LevelZeroBlockPalette blockPalette;

    /**
     * Construit l'etape des couches fixes.
     *
     * @param blockPalette palette de blocs
     */
    public LevelZeroFoundationWriteStage(LevelZeroBlockPalette blockPalette) {
        this.blockPalette = blockPalette;
    }

    @Override
    public void initializeColumnSample(BlockState[] states) {
        for (int i = 0; i < states.length; i++) {
            states[i] = Blocks.AIR.getDefaultState();
        }

        setColumnState(states, LevelZeroVerticalLayout.bedrockY(), blockPalette.bedrock());
        setColumnState(states, LevelZeroVerticalLayout.subfloorY(), blockPalette.subfloor());
    }

    @Override
    public void writeChunkColumn(Chunk chunk,
                                 BlockPos.Mutable mutablePos,
                                 int localX,
                                 int localZ,
                                 LevelZeroResolvedColumn resolvedColumn) {
        chunk.setBlockState(mutablePos.set(localX, LevelZeroVerticalLayout.bedrockY(), localZ),
                blockPalette.bedrock(),
                false);
        chunk.setBlockState(mutablePos.set(localX, LevelZeroVerticalLayout.subfloorY(), localZ),
                blockPalette.subfloor(),
                false);
    }

    @Override
    public void writeColumnSample(BlockState[] states, LevelZeroResolvedColumn resolvedColumn) {
    }

    private static void setColumnState(BlockState[] states, int y, BlockState state) {
        if (y >= 0 && y < states.length) {
            states[y] = state;
        }
    }
}
