package com.petassegang.addons.world.backrooms.level0;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroVerticalLayout;
import com.petassegang.addons.world.backrooms.level0.noise.LevelZeroSeedResolver;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroBlockPalette;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroBlockWriter;

/**
 * Generateur monocouche du Level 0 des Backrooms.
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
    public CompletableFuture<Chunk> populateNoise(Blender blender,
                                                  NoiseConfig noiseConfig,
                                                  StructureAccessor structureAccessor,
                                                  Chunk chunk) {
        long layoutSeed = LevelZeroSeedResolver.resolveLayoutSeed(noiseConfig);
        LevelZeroLayout layout = LevelZeroLayout.generate(chunk.getPos().x, chunk.getPos().z, layoutSeed);
        BLOCK_WRITER.writeChunk(chunk, layout, layoutSeed);

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
        return LevelZeroVerticalLayout.heightmapTopY();
    }

    @Override
    public VerticalBlockSample getColumnSample(int x,
                                               int z,
                                               HeightLimitView world,
                                               NoiseConfig noiseConfig) {
        BlockState[] states = new BlockState[LevelZeroVerticalLayout.COLUMN_SAMPLE_HEIGHT];
        long layoutSeed = LevelZeroSeedResolver.resolveLayoutSeed(noiseConfig);
        LevelZeroLayout layout = LevelZeroLayout.generate(
                Math.floorDiv(x, LevelZeroLayout.CHUNK_SIZE),
                Math.floorDiv(z, LevelZeroLayout.CHUNK_SIZE),
                layoutSeed);
        BLOCK_WRITER.writeColumnSample(states, layout, layoutSeed, x, z);

        return new VerticalBlockSample(world.getBottomY(), states);
    }

    @Override
    public int getWorldHeight() {
        // Correspond au "height" du fichier backrooms_level_0_type.json.
        return LevelZeroVerticalLayout.worldHeight();
    }

    @Override
    public int getSeaLevel() {
        return LevelZeroVerticalLayout.seaLevel();
    }

    @Override
    public int getMinimumY() {
        return LevelZeroVerticalLayout.minimumY();
    }

    @Override
    public void getDebugHudText(List<String> text,
                                NoiseConfig noiseConfig,
                                BlockPos pos) {
        text.add("Backrooms : Level 0");
        text.add("ChunkGenerator : Traduction monocouche du script Python.");
    }
}
