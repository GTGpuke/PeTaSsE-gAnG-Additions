package com.petassegang.addons.world.backrooms.level0.write;

import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

/**
 * Composite simple de plusieurs etapes d'ecriture appliquees dans l'ordre.
 */
public final class LevelZeroCompositeWriteStage implements LevelZeroWriteStage {

    private final List<LevelZeroWriteStage> stages;

    /**
     * Construit un composite de stages d'ecriture.
     *
     * @param stages stages a executer dans l'ordre
     */
    public LevelZeroCompositeWriteStage(List<LevelZeroWriteStage> stages) {
        this.stages = List.copyOf(stages);
    }

    @Override
    public void initializeColumnSample(BlockState[] states) {
        for (LevelZeroWriteStage stage : stages) {
            stage.initializeColumnSample(states);
        }
    }

    @Override
    public void writeChunkColumn(Chunk chunk,
                                 BlockPos.Mutable mutablePos,
                                 int localX,
                                 int localZ,
                                 LevelZeroResolvedColumn resolvedColumn) {
        for (LevelZeroWriteStage stage : stages) {
            stage.writeChunkColumn(chunk, mutablePos, localX, localZ, resolvedColumn);
        }
    }

    @Override
    public void writeColumnSample(BlockState[] states, LevelZeroResolvedColumn resolvedColumn) {
        for (LevelZeroWriteStage stage : stages) {
            stage.writeColumnSample(states, resolvedColumn);
        }
    }
}
