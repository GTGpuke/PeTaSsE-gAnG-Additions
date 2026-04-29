package com.petassegang.addons.backrooms.level.level0.generation;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import com.petassegang.addons.perf.section.ModPerformanceMonitor;
import com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroLayerStackLayout;
import com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroVerticalSlice;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroLayout;
import com.petassegang.addons.backrooms.level.level0.generation.noise.LevelZeroSeedResolver;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroBlockPalette;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroBlockWriter;

/**
 * Generateur multi-layer du Level 0 des Backrooms.
 *
 * <p>Ce fichier est le point d'entree runtime de toute la generation. Son role
 * est volontairement restreint :
 *
 * <ol>
 *   <li>choisir les {@code verticalSlice} a generer ;</li>
 *   <li>deriver une seed par layer ;</li>
 *   <li>demander a {@link LevelZeroLayout} l'etat logique du chunk ;</li>
 *   <li>deleguer a {@code LevelZeroBlockWriter} la traduction en blocs.</li>
 * </ol>
 *
 * <p>Le generateur ne decide donc ni la topologie fine, ni la lumiere, ni les
 * details muraux. Il orchestre seulement la pile verticale et la reconstruction
 * deterministe de chaque layer.
 */
public final class LevelZeroChunkGenerator extends ChunkGenerator {

    /** Codec de serialisation du generateur. */
    public static final MapCodec<LevelZeroChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Biome.REGISTRY_CODEC.fieldOf("biome")
                            .forGetter(LevelZeroChunkGenerator::biome))
                    .apply(instance, LevelZeroChunkGenerator::new));

    private static final LevelZeroBlockPalette BLOCK_PALETTE = new LevelZeroBlockPalette();
    private static final LevelZeroBlockWriter BLOCK_WRITER = new LevelZeroBlockWriter(BLOCK_PALETTE);
    private final RegistryEntry<Biome> biome;

    /**
     * Construit le generateur avec un biome fixe.
     *
     * @param biome biome unique de la dimension
     */
    public LevelZeroChunkGenerator(RegistryEntry<Biome> biome) {
        super(new FixedBiomeSource(biome));
        this.biome = biome;
    }

    private RegistryEntry<Biome> biome() {
        return biome;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    @SuppressWarnings("try")
    public CompletableFuture<Chunk> populateNoise(Blender blender,
                                                  NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor,
                                                  Chunk chunk) {
        try (ModPerformanceMonitor.Scope ignored = ModPerformanceMonitor.scope("level0.chunk_generator.populate_noise")) {
            List<LevelZeroVerticalSlice> slices = LevelZeroLayerStackLayout.defaultSlices();
            for (LevelZeroVerticalSlice slice : slices) {
                long layerSeed = LevelZeroSeedResolver.resolveLayerLayoutSeed(noiseConfig, slice.layerIndex());
                // Chaque layer garde la meme pipeline que l'ancien Level 0, mais
                // avec une seed derivee propre pour eviter de dupliquer le meme
                // labyrinthe d'un etage a l'autre.
                LevelZeroLayout layout;
                try (ModPerformanceMonitor.Scope layoutScope =
                             ModPerformanceMonitor.scope("level0.chunk_generator.layout_generate")) {
                    layout = LevelZeroLayout.generate(chunk.getPos().x, chunk.getPos().z, layerSeed, slice.layerIndex());
                }
                try (ModPerformanceMonitor.Scope writeScope =
                             ModPerformanceMonitor.scope("level0.chunk_generator.write_chunk")) {
                    BLOCK_WRITER.writeChunk(chunk, layout, layerSeed, slice);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(ChunkRegion region,
                             StructureAccessor structures,
                             NoiseConfig noiseConfig,
                             Chunk chunk) {
    }

    @Override
    public void carve(ChunkRegion chunkRegion,
                      long seed,
                      NoiseConfig noiseConfig,
                      net.minecraft.world.biome.source.BiomeAccess biomeAccess,
                      StructureAccessor structureAccessor,
                      Chunk chunk,
                      GenerationStep.Carver carverStep) {
    }

    @Override
    public void populateEntities(ChunkRegion region) {
    }

    @Override
    public int getHeight(int x,
                         int z,
                         Heightmap.Type heightmap,
                         HeightLimitView world,
                         NoiseConfig noiseConfig) {
        return LevelZeroLayerStackLayout.recommendedHeightmapTopY();
    }

    @Override
    @SuppressWarnings("try")
    public VerticalBlockSample getColumnSample(int x,
                                               int z,
                                               HeightLimitView world,
                                               NoiseConfig noiseConfig) {
        try (ModPerformanceMonitor.Scope ignored = ModPerformanceMonitor.scope("level0.chunk_generator.column_sample")) {
            BlockState[] states = new BlockState[getWorldHeight()];
            boolean initialize = true;
            // Le sample vertical doit reconstruire exactement les memes slices que
            // populateNoise pour que heightmaps, debug et worldgen lisent la meme
            // pile logique.
            for (LevelZeroVerticalSlice slice : LevelZeroLayerStackLayout.defaultSlices()) {
                long layerSeed = LevelZeroSeedResolver.resolveLayerLayoutSeed(noiseConfig, slice.layerIndex());
                LevelZeroLayout layout;
                try (ModPerformanceMonitor.Scope layoutScope =
                             ModPerformanceMonitor.scope("level0.chunk_generator.column_sample.layout_generate")) {
                    layout = LevelZeroLayout.generate(
                            Math.floorDiv(x, LevelZeroLayout.CHUNK_SIZE),
                            Math.floorDiv(z, LevelZeroLayout.CHUNK_SIZE),
                            layerSeed,
                            slice.layerIndex());
                }
                try (ModPerformanceMonitor.Scope writeScope =
                             ModPerformanceMonitor.scope("level0.chunk_generator.column_sample.write_column")) {
                    BLOCK_WRITER.writeColumnSample(states, layout, layerSeed, x, z, slice, initialize);
                }
                initialize = false;
            }

            return new VerticalBlockSample(world.getBottomY(), states);
        }
    }

    @Override
    public int getWorldHeight() {
        return LevelZeroLayerStackLayout.recommendedDimensionHeight();
    }

    @Override
    public int getSeaLevel() {
        return LevelZeroLayerStackLayout.floorY(0);
    }

    @Override
    public int getMinimumY() {
        return LevelZeroLayerStackLayout.minimumY();
    }

    @Override
    public void getDebugHudText(List<String> text,
                                NoiseConfig noiseConfig,
                                BlockPos pos) {
        text.add("Backrooms : Level 0");
        text.add("ChunkGenerator : pile multi-layer seedee par layer.");
        ModPerformanceMonitor.appendDebugHudText(text);
    }
}
