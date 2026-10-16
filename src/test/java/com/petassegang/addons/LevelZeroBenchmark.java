package com.petassegang.addons;

import java.util.Arrays;
import java.util.Locale;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import net.minecraft.block.BlockState;

import com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroCoords;
import com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;
import com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroVerticalSlice;
import com.petassegang.addons.backrooms.level0.generation.layout.LevelZeroLayout;
import com.petassegang.addons.backrooms.level0.generation.noise.LevelZeroSeedResolver;
import com.petassegang.addons.backrooms.level0.generation.write.LevelZeroBlockPalette;
import com.petassegang.addons.backrooms.level0.generation.write.LevelZeroBlockWriter;

/**
 * Benchmark deterministe de la generation du Level 0 : layout (secteurs froids puis chauds)
 * et ecriture complete des colonnes des cinq layers, comme le ferait {@code populateNoise}.
 *
 * <p>Exclu de {@code ./gradlew test} ; lance par {@code ./gradlew benchmarkLevelZeroGeneration}
 * sous le classloader Fabric, avec la vraie palette du mod. La propriete
 * {@code levelZeroPerfBudgetMsPerChunk} fait echouer la tache si le cout moyen par chunk
 * (layout + ecriture, cinq layers) depasse le budget.
 */
@Tag("benchmark")
class LevelZeroBenchmark {

    private static final long[] SEEDS = {12345L, 998877L, 13579L};
    private static final int CHUNK_RADIUS = 6;
    private static final int WARMUP_PASSES = 2;
    private static final int MEASURED_PASSES = 5;

    @Test
    void benchmark() {
        MinecraftTestBootstrap.ensure();
        LevelZeroBlockWriter writer = new LevelZeroBlockWriter(LevelZeroBlockPalette.fromRegistry(), false, false);
        int chunksPerPass = SEEDS.length * (2 * CHUNK_RADIUS + 1) * (2 * CHUNK_RADIUS + 1);

        LevelZeroLayout.clearCache();
        long coldLayoutNanos = timeLayouts();
        for (int pass = 0; pass < WARMUP_PASSES; pass++) {
            timeLayouts();
            timeFullChunks(writer);
        }

        long warmLayoutNanos = 0L;
        long fullChunkNanos = 0L;
        for (int pass = 0; pass < MEASURED_PASSES; pass++) {
            warmLayoutNanos += timeLayouts();
            fullChunkNanos += timeFullChunks(writer);
        }
        double coldLayoutMsPerChunk = millis(coldLayoutNanos) / chunksPerPass;
        double warmLayoutMsPerChunk = millis(warmLayoutNanos) / (chunksPerPass * (double) MEASURED_PASSES);
        double fullChunkMsPerChunk = millis(fullChunkNanos) / (chunksPerPass * (double) MEASURED_PASSES);

        System.out.println("=== Benchmark Level 0 ===");
        System.out.printf(Locale.ROOT, "Chunks par passe        : %d (%d seeds, rayon %d, %d layers)%n",
                chunksPerPass, SEEDS.length, CHUNK_RADIUS, LevelZeroLayerStackLayout.LAYER_COUNT);
        System.out.printf(Locale.ROOT, "Layout, secteurs froids : %.4f ms/chunk (generation des secteurs incluse)%n", coldLayoutMsPerChunk);
        System.out.printf(Locale.ROOT, "Layout, secteurs chauds : %.4f ms/chunk%n", warmLayoutMsPerChunk);
        System.out.printf(Locale.ROOT, "Layout + ecriture       : %.4f ms/chunk (%d colonnes x %d layers)%n",
                fullChunkMsPerChunk, LevelZeroCoords.CHUNK_SIZE * LevelZeroCoords.CHUNK_SIZE, LevelZeroLayerStackLayout.LAYER_COUNT);
        System.out.printf(Locale.ROOT, "Secteurs en cache       : %d%n", LevelZeroLayout.cachedSectorCount());

        String budget = System.getProperty("levelZeroPerfBudgetMsPerChunk");
        if (budget != null && !budget.isBlank() && fullChunkMsPerChunk > Double.parseDouble(budget)) {
            throw new AssertionError(String.format(Locale.ROOT,
                    "Le benchmark du Level 0 depasse le budget : %.4f ms/chunk > %s ms/chunk.", fullChunkMsPerChunk, budget));
        }
    }

    private static long timeLayouts() {
        long start = System.nanoTime();
        for (long seed : SEEDS) {
            for (int layer = 0; layer < LevelZeroLayerStackLayout.LAYER_COUNT; layer++) {
                long layerSeed = LevelZeroSeedResolver.resolveLayerSeed(seed, layer);
                for (int chunkX = -CHUNK_RADIUS; chunkX <= CHUNK_RADIUS; chunkX++) {
                    for (int chunkZ = -CHUNK_RADIUS; chunkZ <= CHUNK_RADIUS; chunkZ++) {
                        LevelZeroLayout.generate(chunkX, chunkZ, layerSeed, layer);
                    }
                }
            }
        }
        return System.nanoTime() - start;
    }

    private static long timeFullChunks(LevelZeroBlockWriter writer) {
        BlockState[] column = new BlockState[LevelZeroLayerStackLayout.DIMENSION_HEIGHT];
        BlockState air = writer.palette().air();
        long start = System.nanoTime();
        for (long seed : SEEDS) {
            for (int chunkX = -CHUNK_RADIUS; chunkX <= CHUNK_RADIUS; chunkX++) {
                for (int chunkZ = -CHUNK_RADIUS; chunkZ <= CHUNK_RADIUS; chunkZ++) {
                    for (LevelZeroVerticalSlice slice : LevelZeroLayerStackLayout.slices()) {
                        long layerSeed = LevelZeroSeedResolver.resolveLayerSeed(seed, slice.layerIndex());
                        LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, chunkZ, layerSeed, slice.layerIndex());
                        for (int localX = 0; localX < LevelZeroCoords.CHUNK_SIZE; localX++) {
                            for (int localZ = 0; localZ < LevelZeroCoords.CHUNK_SIZE; localZ++) {
                                Arrays.fill(column, air);
                                writer.writeColumnSample(column, layout, slice,
                                        LevelZeroCoords.chunkStart(chunkX) + localX,
                                        LevelZeroCoords.chunkStart(chunkZ) + localZ);
                            }
                        }
                    }
                }
            }
        }
        return System.nanoTime() - start;
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0D;
    }
}
