package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroCoords;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroVerticalSlice;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroLayout;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.noise.LevelZeroSeedResolver;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.write.LevelZeroBlockPalette;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.write.LevelZeroBlockWriter;
import com.gtgpuke.gtgsbackrooms.perf.ModPerformanceMonitor;

/**
 * Generateur de chunks du Level 0 : cinq layers de labyrinthe empiles.
 *
 * <p>Pour chaque layer, le generateur derive une seed propre, demande le layout
 * logique du chunk puis le fait ecrire par {@link LevelZeroBlockWriter}. Il ne
 * decide ni topologie, ni lumiere, ni details : il orchestre la pile verticale.
 */
public final class LevelZeroChunkGenerator extends ChunkGenerator {

    /** Codec de serialisation du generateur. */
    public static final MapCodec<LevelZeroChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Biome.REGISTRY_CODEC.fieldOf("biome").forGetter(generator -> generator.biome))
                    .apply(instance, LevelZeroChunkGenerator::new));

    private static final LevelZeroBlockWriter WRITER = new LevelZeroBlockWriter(LevelZeroBlockPalette.fromRegistry());

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

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    @SuppressWarnings("try")
    public CompletableFuture<Chunk> populateNoise(Blender blender, NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor, Chunk chunk) {
        try (ModPerformanceMonitor.Scope ignored = ModPerformanceMonitor.scope("level0.chunk.populate_noise")) {
            long layoutSeed = LevelZeroSeedResolver.resolveLayoutSeed(noiseConfig);
            int chunkX = chunk.getPos().x;
            int chunkZ = chunk.getPos().z;
            for (LevelZeroVerticalSlice slice : LevelZeroLayerStackLayout.slices()) {
                long layerSeed = LevelZeroSeedResolver.resolveLayerSeed(layoutSeed, slice.layerIndex());
                LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, chunkZ, layerSeed, slice.layerIndex());
                WRITER.writeChunk(chunk, layout, slice);
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        // Pas de surface : tout est ecrit dans populateNoise.
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess,
                      StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
        // Pas de grottes dans les Backrooms.
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        // Aucun spawn naturel : le Level 0 est vide.
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return LevelZeroLayerStackLayout.heightmapTopY();
    }

    @Override
    @SuppressWarnings("try")
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        try (ModPerformanceMonitor.Scope ignored = ModPerformanceMonitor.scope("level0.chunk.column_sample")) {
            BlockState[] states = new BlockState[getWorldHeight()];
            Arrays.fill(states, WRITER.palette().air());
            long layoutSeed = LevelZeroSeedResolver.resolveLayoutSeed(noiseConfig);
            int chunkX = Math.floorDiv(x, LevelZeroCoords.CHUNK_SIZE);
            int chunkZ = Math.floorDiv(z, LevelZeroCoords.CHUNK_SIZE);
            // Meme pile de layers que populateNoise pour que heightmap et worldgen lisent la meme chose.
            for (LevelZeroVerticalSlice slice : LevelZeroLayerStackLayout.slices()) {
                long layerSeed = LevelZeroSeedResolver.resolveLayerSeed(layoutSeed, slice.layerIndex());
                LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, chunkZ, layerSeed, slice.layerIndex());
                WRITER.writeColumnSample(states, layout, slice, x, z);
            }
            return new VerticalBlockSample(world.getBottomY(), states);
        }
    }

    @Override
    public int getWorldHeight() {
        return LevelZeroLayerStackLayout.DIMENSION_HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return LevelZeroLayerStackLayout.floorY(0);
    }

    @Override
    public int getMinimumY() {
        return LevelZeroLayerStackLayout.MIN_Y;
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        text.add("Backrooms : Level 0");
        if (LevelZeroLayerStackLayout.isInsideStack(pos.getY())) {
            text.add("Layer : " + LevelZeroLayerStackLayout.sliceAtY(pos.getY()).layerIndex()
                    + " / secteurs en cache : " + LevelZeroLayout.cachedSectorCount());
        }
        ModPerformanceMonitor.appendDebugHudText(text);
    }
}
