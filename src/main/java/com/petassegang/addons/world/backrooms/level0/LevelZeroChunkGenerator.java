package com.petassegang.addons.world.backrooms.level0;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.world.backrooms.BackroomsConstants;

/**
 * Generateur monocouche du Level 0 des Backrooms.
 */
public final class LevelZeroChunkGenerator extends ChunkGenerator {

    /** Codec de serialization du generateur. */
    public static final MapCodec<LevelZeroChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Biome.CODEC.fieldOf("biome").forGetter(LevelZeroChunkGenerator::biome))
                    .apply(instance, LevelZeroChunkGenerator::new));

    private final Holder<Biome> biome;

    /**
     * Construit le generateur avec un biome fixe.
     *
     * @param biome biome unique de la dimension
     */
    public LevelZeroChunkGenerator(Holder<Biome> biome) {
        super(new FixedBiomeSource(biome));
        this.biome = biome;
    }

    private Holder<Biome> biome() {
        return biome;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender,
                                                        RandomState randomState,
                                                        StructureManager structureManager,
                                                        ChunkAccess chunk) {
        LevelZeroLayout layout = LevelZeroLayout.generate(
                chunk.getPos().x(),
                chunk.getPos().z(),
                resolveLayoutSeed(randomState));
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState subfloor = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState floor = ModBlocks.LEVEL_ZERO_DAMP_CARPET.get().defaultBlockState();
        BlockState wallpaper = ModBlocks.LEVEL_ZERO_WALLPAPER.get().defaultBlockState();
        BlockState ceiling = ModBlocks.LEVEL_ZERO_CEILING_TILE.get().defaultBlockState();
        BlockState light = ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT.get().defaultBlockState();

        for (int localX = 0; localX < LevelZeroLayout.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < LevelZeroLayout.CHUNK_SIZE; localZ++) {
                chunk.setBlockState(mutablePos.set(localX, BackroomsConstants.LEVEL_ZERO_BEDROCK_Y, localZ), bedrock, 0);
                chunk.setBlockState(mutablePos.set(localX, BackroomsConstants.LEVEL_ZERO_SUBFLOOR_Y, localZ), subfloor, 0);

                if (layout.isWalkable(localX, localZ)) {
                    chunk.setBlockState(mutablePos.set(localX, BackroomsConstants.LEVEL_ZERO_FLOOR_Y, localZ), floor, 0);
                    for (int y = BackroomsConstants.LEVEL_ZERO_AIR_MIN_Y; y <= BackroomsConstants.LEVEL_ZERO_AIR_MAX_Y; y++) {
                        chunk.setBlockState(mutablePos.set(localX, y, localZ), Blocks.AIR.defaultBlockState(), 0);
                    }
                    BlockState ceilingState = layout.hasLight(localX, localZ) ? light : ceiling;
                    chunk.setBlockState(mutablePos.set(localX, BackroomsConstants.LEVEL_ZERO_CEILING_Y, localZ), ceilingState, 0);
                } else {
                    chunk.setBlockState(mutablePos.set(localX, BackroomsConstants.LEVEL_ZERO_FLOOR_Y, localZ), floor, 0);
                    for (int y = BackroomsConstants.LEVEL_ZERO_AIR_MIN_Y; y <= BackroomsConstants.LEVEL_ZERO_CEILING_Y; y++) {
                        chunk.setBlockState(mutablePos.set(localX, y, localZ), wallpaper, 0);
                    }
                }
            }
        }

        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion level,
                             StructureManager structureManager,
                             RandomState randomState,
                             ChunkAccess chunk) {
    }

    @Override
    public void applyCarvers(WorldGenRegion level,
                             long seed,
                             RandomState randomState,
                             BiomeManager biomeManager,
                             StructureManager structureManager,
                             ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getBaseHeight(int x,
                             int z,
                             Heightmap.Types heightmap,
                             LevelHeightAccessor heightAccessor,
                             RandomState randomState) {
        return BackroomsConstants.LEVEL_ZERO_CEILING_Y + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x,
                                     int z,
                                     LevelHeightAccessor heightAccessor,
                                     RandomState randomState) {
        BlockState[] states = new BlockState[BackroomsConstants.LEVEL_ZERO_GEN_DEPTH];
        for (int y = 0; y < states.length; y++) {
            states[y] = Blocks.AIR.defaultBlockState();
        }

        setColumnState(states, BackroomsConstants.LEVEL_ZERO_BEDROCK_Y, Blocks.BEDROCK.defaultBlockState());
        setColumnState(states, BackroomsConstants.LEVEL_ZERO_SUBFLOOR_Y, Blocks.SMOOTH_STONE.defaultBlockState());
        setColumnState(states, BackroomsConstants.LEVEL_ZERO_FLOOR_Y, ModBlocks.LEVEL_ZERO_DAMP_CARPET.get().defaultBlockState());

        LevelZeroLayout layout = LevelZeroLayout.generate(
                Math.floorDiv(x, LevelZeroLayout.CHUNK_SIZE),
                Math.floorDiv(z, LevelZeroLayout.CHUNK_SIZE),
                resolveLayoutSeed(randomState));
        int localX = Math.floorMod(x, LevelZeroLayout.CHUNK_SIZE);
        int localZ = Math.floorMod(z, LevelZeroLayout.CHUNK_SIZE);
        boolean walkable = layout.isWalkable(localX, localZ);

        if (walkable) {
            setColumnState(states,
                    BackroomsConstants.LEVEL_ZERO_CEILING_Y,
                    layout.hasLight(localX, localZ)
                            ? ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT.get().defaultBlockState()
                            : ModBlocks.LEVEL_ZERO_CEILING_TILE.get().defaultBlockState());
        } else {
            for (int y = BackroomsConstants.LEVEL_ZERO_AIR_MIN_Y; y <= BackroomsConstants.LEVEL_ZERO_CEILING_Y; y++) {
                setColumnState(states, y, ModBlocks.LEVEL_ZERO_WALLPAPER.get().defaultBlockState());
            }
        }

        return new NoiseColumn(heightAccessor.getMinY(), states);
    }

    @Override
    public int getGenDepth() {
        return BackroomsConstants.LEVEL_ZERO_GEN_DEPTH;
    }

    @Override
    public int getSeaLevel() {
        return BackroomsConstants.LEVEL_ZERO_FLOOR_Y;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public void addDebugScreenInfo(List<String> result,
                                   RandomState randomState,
                                   BlockPos pos) {
        result.add("Backrooms : Level 0");
        result.add("ChunkGenerator : Traduction monocouche du script Python.");
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
    }

    private static void setColumnState(BlockState[] states, int y, BlockState state) {
        if (y >= 0 && y < states.length) {
            states[y] = state;
        }
    }

    private static long resolveLayoutSeed(RandomState randomState) {
        return randomState.getOrCreateRandomFactory(BackroomsConstants.LEVEL_ZERO_LAYOUT_RANDOM)
                .at(0, 0, 0)
                .nextLong();
    }
}
