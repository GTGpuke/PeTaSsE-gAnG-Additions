package com.petassegang.addons;

import java.util.Locale;

import com.petassegang.addons.world.backrooms.BackroomsConstants;
import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;
import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;

/**
 * Benchmark local et deterministe du pipeline de generation du Level 0.
 *
 * <p>Cette classe n'est pas un test unitaire. Elle sert a comparer rapidement
 * le cout relatif de la generation d'une meme zone entre plusieurs revisions
 * du code, afin de detecter les regressions avant d'empiler plusieurs couches
 * de generation Backrooms.
 */
public final class LevelZeroPerformanceCheck {

    private static final long[] SAMPLE_SEEDS = {
            12345L,
            998877L,
            13579L,
            24680L,
            424242L
    };
    private static final int CHUNK_RADIUS = 6;
    private static final int WARMUP_PASSES = 3;
    private static final int MEASURED_PASSES = 5;
    private static final int NORTH_MASK = 1;
    private static final int SOUTH_MASK = 1 << 1;
    private static final int WEST_MASK = 1 << 2;
    private static final int EAST_MASK = 1 << 3;
    private static final int FULL_MASK = NORTH_MASK | SOUTH_MASK | WEST_MASK | EAST_MASK;

    private LevelZeroPerformanceCheck() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Lance le benchmark local du Level 0.
     *
     * @param args arguments optionnels, non utilises
     */
    public static void main(String[] args) {
        for (int pass = 0; pass < WARMUP_PASSES; pass++) {
            runScenario(false);
        }

        ScenarioStats total = new ScenarioStats();
        long bestDurationNs = Long.MAX_VALUE;
        long worstDurationNs = Long.MIN_VALUE;

        for (int pass = 0; pass < MEASURED_PASSES; pass++) {
            ScenarioStats current = runScenario(false);
            total.add(current);
            bestDurationNs = Math.min(bestDurationNs, current.durationNs);
            worstDurationNs = Math.max(worstDurationNs, current.durationNs);
        }

        double averageDurationMs = nanosToMillis(total.durationNs / (double) MEASURED_PASSES);
        double averageChunkDurationMs = averageDurationMs / total.generatedChunks * MEASURED_PASSES;
        double bestDurationMs = nanosToMillis(bestDurationNs);
        double worstDurationMs = nanosToMillis(worstDurationNs);
        double mixedColumnRatio = total.mixedColumns * 100.0D / total.wallColumns;

        System.out.println("=== Benchmark Level 0 ===");
        System.out.printf(Locale.ROOT, "Passes mesurees : %d%n", MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Chunks analyses : %d%n", total.generatedChunks / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Temps moyen total : %.3f ms%n", averageDurationMs);
        System.out.printf(Locale.ROOT, "Temps moyen par chunk : %.6f ms%n", averageChunkDurationMs);
        System.out.printf(Locale.ROOT, "Meilleur passage : %.3f ms%n", bestDurationMs);
        System.out.printf(Locale.ROOT, "Pire passage : %.3f ms%n", worstDurationMs);
        System.out.printf(Locale.ROOT, "Colonnes de murs : %d%n", total.wallColumns / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Colonnes exposees : %d%n", total.exposedColumns / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Colonnes mixtes : %d (%.2f%% des murs)%n",
                total.mixedColumns / MEASURED_PASSES,
                mixedColumnRatio);
        System.out.printf(Locale.ROOT, "Sondes de faces : %d%n", total.faceSamples / MEASURED_PASSES);

        String budgetProperty = System.getProperty("levelZeroPerfBudgetMsPerChunk");
        if (budgetProperty == null || budgetProperty.isBlank()) {
            return;
        }

        double budgetMsPerChunk = Double.parseDouble(budgetProperty);
        if (averageChunkDurationMs > budgetMsPerChunk) {
            throw new IllegalStateException(String.format(
                    Locale.ROOT,
                    "Le benchmark du Level 0 depasse le budget autorise : %.6f ms/chunk > %.6f ms/chunk.",
                    averageChunkDurationMs,
                    budgetMsPerChunk));
        }
    }

    private static ScenarioStats runScenario(boolean ignored) {
        ScenarioStats stats = new ScenarioStats();
        long startNs = System.nanoTime();

        for (long layoutSeed : SAMPLE_SEEDS) {
            LayoutSampler sampler = new LayoutSampler(layoutSeed);
            for (int chunkX = -CHUNK_RADIUS; chunkX <= CHUNK_RADIUS; chunkX++) {
                for (int chunkZ = -CHUNK_RADIUS; chunkZ <= CHUNK_RADIUS; chunkZ++) {
                    LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, chunkZ, layoutSeed);
                    sampler.putLayout(chunkX, chunkZ, layout);
                    scanChunk(layout, sampler, chunkX, chunkZ, stats);
                    stats.generatedChunks++;
                }
            }
        }

        stats.durationNs = System.nanoTime() - startNs;
        return stats;
    }

    private static void scanChunk(LevelZeroLayout layout,
                                  LayoutSampler sampler,
                                  int chunkX,
                                  int chunkZ,
                                  ScenarioStats stats) {
        int worldMinX = chunkX * LevelZeroLayout.CHUNK_SIZE;
        int worldMinZ = chunkZ * LevelZeroLayout.CHUNK_SIZE;

        for (int localX = 0; localX < LevelZeroLayout.CHUNK_SIZE; localX++) {
            int worldX = worldMinX + localX;
            for (int localZ = 0; localZ < LevelZeroLayout.CHUNK_SIZE; localZ++) {
                if (layout.isWalkable(localX, localZ)) {
                    continue;
                }

                stats.wallColumns++;
                int worldZ = worldMinZ + localZ;
                if (!sampler.isWallpaperExposed(worldX, worldZ)) {
                    continue;
                }

                stats.exposedColumns++;
                int faceMask = sampler.sampleWallpaperFaceMask(worldX, worldZ, stats);
                if (faceMask != 0 && faceMask != FULL_MASK) {
                    stats.mixedColumns++;
                }
            }
        }
    }

    private static double nanosToMillis(double nanos) {
        return nanos / 1_000_000.0D;
    }

    private static final class ScenarioStats {

        private long durationNs;
        private int generatedChunks;
        private int wallColumns;
        private int exposedColumns;
        private int mixedColumns;
        private int faceSamples;

        private void add(ScenarioStats other) {
            durationNs += other.durationNs;
            generatedChunks += other.generatedChunks;
            wallColumns += other.wallColumns;
            exposedColumns += other.exposedColumns;
            mixedColumns += other.mixedColumns;
            faceSamples += other.faceSamples;
        }
    }

    private static final class LayoutSampler {

        private final long layoutSeed;
        private final java.util.Map<Long, LevelZeroLayout> layouts = new java.util.HashMap<>();

        private LayoutSampler(long layoutSeed) {
            this.layoutSeed = layoutSeed;
        }

        private void putLayout(int chunkX, int chunkZ, LevelZeroLayout layout) {
            layouts.put(chunkKey(chunkX, chunkZ), layout);
        }

        private boolean isWallpaperExposed(int worldX, int worldZ) {
            return isWalkableAt(worldX + 1, worldZ)
                    || isWalkableAt(worldX - 1, worldZ)
                    || isWalkableAt(worldX, worldZ + 1)
                    || isWalkableAt(worldX, worldZ - 1);
        }

        private int sampleWallpaperFaceMask(int worldX, int worldZ, ScenarioStats stats) {
            int faceMask = 0;
            faceMask |= sampleFace(worldX, worldZ, 0, -1, NORTH_MASK, stats);
            faceMask |= sampleFace(worldX, worldZ, 0, 1, SOUTH_MASK, stats);
            faceMask |= sampleFace(worldX, worldZ, -1, 0, WEST_MASK, stats);
            faceMask |= sampleFace(worldX, worldZ, 1, 0, EAST_MASK, stats);
            return faceMask;
        }

        private int sampleFace(int worldX,
                               int worldZ,
                               int stepX,
                               int stepZ,
                               int maskBit,
                               ScenarioStats stats) {
            for (int distance = 1; distance <= BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 4; distance++) {
                int sampleX = worldX + stepX * distance;
                int sampleZ = worldZ + stepZ * distance;
                stats.faceSamples++;
                if (!isWalkableAt(sampleX, sampleZ)) {
                    continue;
                }
                return LevelZeroSurfaceBiome.sampleAtWorld(sampleX, sampleZ) == LevelZeroSurfaceBiome.RED ? maskBit : 0;
            }

            int fallbackX = worldX + stepX * BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 2;
            int fallbackZ = worldZ + stepZ * BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 2;
            stats.faceSamples++;
            return LevelZeroSurfaceBiome.sampleAtWorld(fallbackX, fallbackZ) == LevelZeroSurfaceBiome.RED ? maskBit : 0;
        }

        private boolean isWalkableAt(int worldX, int worldZ) {
            int chunkX = Math.floorDiv(worldX, LevelZeroLayout.CHUNK_SIZE);
            int chunkZ = Math.floorDiv(worldZ, LevelZeroLayout.CHUNK_SIZE);
            LevelZeroLayout layout = layoutAtChunk(chunkX, chunkZ);
            int localX = Math.floorMod(worldX, LevelZeroLayout.CHUNK_SIZE);
            int localZ = Math.floorMod(worldZ, LevelZeroLayout.CHUNK_SIZE);
            return layout.isWalkable(localX, localZ);
        }

        private LevelZeroLayout layoutAtChunk(int chunkX, int chunkZ) {
            return layouts.computeIfAbsent(
                    chunkKey(chunkX, chunkZ),
                    ignored -> LevelZeroLayout.generate(chunkX, chunkZ, layoutSeed));
        }

        private static long chunkKey(int chunkX, int chunkZ) {
            return ((long) chunkX << 32) ^ (chunkZ & 0xFFFFFFFFL);
        }
    }
}
