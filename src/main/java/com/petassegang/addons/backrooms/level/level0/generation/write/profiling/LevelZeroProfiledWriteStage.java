package com.petassegang.addons.backrooms.level.level0.generation.write.profiling;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import com.petassegang.addons.perf.section.ModPerformanceMonitor;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroResolvedColumn;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroWriteStage;

/**
 * Decorateur de profiling pour une etape d'ecriture du Level 0.
 *
 * <p>Ce wrapper ne change aucune logique : il mesure uniquement le cout de
 * chaque stage quand le monitoring de performance est actif.
 */
public final class LevelZeroProfiledWriteStage implements LevelZeroWriteStage {

    private final String baseSectionName;
    private final LevelZeroWriteStage delegate;

    /**
     * Construit un decorateur profile pour un stage d'ecriture.
     *
     * @param baseSectionName prefixe stable de section
     * @param delegate stage reel
     */
    public LevelZeroProfiledWriteStage(String baseSectionName, LevelZeroWriteStage delegate) {
        this.baseSectionName = baseSectionName;
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("try")
    public void initializeColumnSample(BlockState[] states) {
        try (ModPerformanceMonitor.Scope ignored =
                     ModPerformanceMonitor.scope(baseSectionName + ".initialize_column_sample")) {
            delegate.initializeColumnSample(states);
        }
    }

    @Override
    @SuppressWarnings("try")
    public void writeChunkColumn(Chunk chunk,
                                 BlockPos.Mutable mutablePos,
                                 int localX,
                                 int localZ,
                                 LevelZeroResolvedColumn resolvedColumn) {
        try (ModPerformanceMonitor.Scope ignored =
                     ModPerformanceMonitor.scope(baseSectionName + ".write_chunk_column")) {
            delegate.writeChunkColumn(chunk, mutablePos, localX, localZ, resolvedColumn);
        }
    }

    @Override
    @SuppressWarnings("try")
    public void writeColumnSample(BlockState[] states, LevelZeroResolvedColumn resolvedColumn) {
        try (ModPerformanceMonitor.Scope ignored =
                     ModPerformanceMonitor.scope(baseSectionName + ".write_column_sample")) {
            delegate.writeColumnSample(states, resolvedColumn);
        }
    }
}
