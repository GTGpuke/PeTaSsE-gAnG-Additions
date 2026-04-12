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

import com.petassegang.addons.block.entity.LevelZeroWallpaperBlockEntity;
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.world.backrooms.BackroomsConstants;

/**
 * Generateur monocouche du Level 0 des Backrooms.
 */
public final class LevelZeroChunkGenerator extends ChunkGenerator {

    private static final int NORTH_MASK = 1;
    private static final int SOUTH_MASK = 1 << 1;
    private static final int WEST_MASK = 1 << 2;
    private static final int EAST_MASK = 1 << 3;
    private static final int FULL_MASK = NORTH_MASK | SOUTH_MASK | WEST_MASK | EAST_MASK;

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
        long layoutSeed = resolveLayoutSeed(randomState);
        LevelZeroLayout layout = LevelZeroLayout.generate(
                chunk.getPos().x(),
                chunk.getPos().z(),
                layoutSeed);
        LayoutSampler sampler = new LayoutSampler(layoutSeed);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState subfloor = Blocks.SMOOTH_STONE.defaultBlockState();
        BlockState wallpaper = ModBlocks.LEVEL_ZERO_WALLPAPER.get().defaultBlockState();
        BlockState alternateWallpaper = ModBlocks.LEVEL_ZERO_WALLPAPER_AGED.get().defaultBlockState();
        BlockState adaptiveWallpaper = ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE.get().defaultBlockState();
        BlockState ceiling = ModBlocks.LEVEL_ZERO_CEILING_TILE.get().defaultBlockState();
        BlockState light = ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT.get().defaultBlockState();
        int worldMinX = chunk.getPos().getMinBlockX();
        int worldMinZ = chunk.getPos().getMinBlockZ();

        for (int localX = 0; localX < LevelZeroLayout.CHUNK_SIZE; localX++) {
            int worldX = worldMinX + localX;
            for (int localZ = 0; localZ < LevelZeroLayout.CHUNK_SIZE; localZ++) {
                int worldZ = worldMinZ + localZ;
                BlockState floor = resolveFloorState(layout, localX, localZ);
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
                    boolean exposedWallpaper = sampler.isWallpaperExposed(worldX, worldZ);
                    int faceMask = exposedWallpaper ? sampler.sampleWallpaperFaceMask(worldX, worldZ) : 0;
                    BlockState wallState = resolveWallState(
                            exposedWallpaper, faceMask, wallpaper, alternateWallpaper, adaptiveWallpaper, bedrock);
                    boolean needsBlockEntity = exposedWallpaper && isMixedFaceMask(faceMask);
                    for (int y = BackroomsConstants.LEVEL_ZERO_AIR_MIN_Y; y <= BackroomsConstants.LEVEL_ZERO_AIR_MAX_Y; y++) {
                        chunk.setBlockState(mutablePos.set(localX, y, localZ), wallState, 0);
                        if (needsBlockEntity) {
                            chunk.setBlockEntity(new LevelZeroWallpaperBlockEntity(
                                    new BlockPos(worldX, y, worldZ),
                                    adaptiveWallpaper,
                                    faceMask));
                        }
                    }
                    chunk.setBlockState(mutablePos.set(localX, BackroomsConstants.LEVEL_ZERO_CEILING_Y, localZ), ceiling, 0);
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

        LevelZeroLayout layout = LevelZeroLayout.generate(
                Math.floorDiv(x, LevelZeroLayout.CHUNK_SIZE),
                Math.floorDiv(z, LevelZeroLayout.CHUNK_SIZE),
                resolveLayoutSeed(randomState));
        long layoutSeed = resolveLayoutSeed(randomState);
        LayoutSampler sampler = new LayoutSampler(layoutSeed);
        int localX = Math.floorMod(x, LevelZeroLayout.CHUNK_SIZE);
        int localZ = Math.floorMod(z, LevelZeroLayout.CHUNK_SIZE);
        boolean walkable = layout.isWalkable(localX, localZ);
        setColumnState(states, BackroomsConstants.LEVEL_ZERO_FLOOR_Y, resolveFloorState(layout, localX, localZ));

        if (walkable) {
            setColumnState(states,
                    BackroomsConstants.LEVEL_ZERO_CEILING_Y,
                    layout.hasLight(localX, localZ)
                            ? ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT.get().defaultBlockState()
                            : ModBlocks.LEVEL_ZERO_CEILING_TILE.get().defaultBlockState());
        } else {
            boolean exposedWallpaper = sampler.isWallpaperExposed(x, z);
            int faceMask = exposedWallpaper ? sampler.sampleWallpaperFaceMask(x, z) : 0;
            BlockState wallState = resolveWallState(
                    exposedWallpaper,
                    faceMask,
                    ModBlocks.LEVEL_ZERO_WALLPAPER.get().defaultBlockState(),
                    ModBlocks.LEVEL_ZERO_WALLPAPER_AGED.get().defaultBlockState(),
                    ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE.get().defaultBlockState(),
                    Blocks.BEDROCK.defaultBlockState());
            for (int y = BackroomsConstants.LEVEL_ZERO_AIR_MIN_Y; y <= BackroomsConstants.LEVEL_ZERO_AIR_MAX_Y; y++) {
                setColumnState(states, y, wallState);
            }
            setColumnState(states, BackroomsConstants.LEVEL_ZERO_CEILING_Y, ModBlocks.LEVEL_ZERO_CEILING_TILE.get().defaultBlockState());
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

    private static BlockState resolveFloorState(LevelZeroLayout layout, int localX, int localZ) {
        return switch (layout.floorVariant(localX, localZ)) {
            case LevelZeroLayout.SURFACE_VARIANT_ALTERNATE ->
                    ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED.get().defaultBlockState();
            default -> ModBlocks.LEVEL_ZERO_DAMP_CARPET.get().defaultBlockState();
        };
    }

    private static BlockState resolveWallState(boolean exposedWallpaper,
                                               int faceMask,
                                               BlockState wallpaper,
                                               BlockState alternateWallpaper,
                                               BlockState adaptiveWallpaper,
                                               BlockState wallInsulation) {
        if (!exposedWallpaper) {
            return wallInsulation;
        }
        if (isMixedFaceMask(faceMask)) {
            return adaptiveWallpaper;
        }
        if (faceMask == FULL_MASK) {
            return alternateWallpaper;
        }
        return wallpaper;
    }

    private static boolean isMixedFaceMask(int faceMask) {
        return faceMask != 0 && faceMask != FULL_MASK;
    }

    private static long resolveLayoutSeed(RandomState randomState) {
        return randomState.getOrCreateRandomFactory(BackroomsConstants.LEVEL_ZERO_LAYOUT_RANDOM)
                .at(0, 0, 0)
                .nextLong();
    }

    /**
     * Echantillonneur local de layout pour eviter de regenerer les memes chunks
     * a chaque sonde de mur adaptatif.
     */
    private static final class LayoutSampler {

        private final long layoutSeed;

        private LayoutSampler(long layoutSeed) {
            this.layoutSeed = layoutSeed;
        }

        private boolean isWallpaperExposed(int worldX, int worldZ) {
            return isWalkableAt(worldX + 1, worldZ)
                    || isWalkableAt(worldX - 1, worldZ)
                    || isWalkableAt(worldX, worldZ + 1)
                    || isWalkableAt(worldX, worldZ - 1);
        }

        private int sampleWallpaperFaceMask(int worldX, int worldZ) {
            int faceMask = 0;
            faceMask |= sampleFace(worldX, worldZ, 0, -1, NORTH_MASK);
            faceMask |= sampleFace(worldX, worldZ, 0, 1, SOUTH_MASK);
            faceMask |= sampleFace(worldX, worldZ, -1, 0, WEST_MASK);
            faceMask |= sampleFace(worldX, worldZ, 1, 0, EAST_MASK);
            return faceMask;
        }

        private int sampleFace(int worldX, int worldZ, int stepX, int stepZ, int maskBit) {
            for (int distance = 1; distance <= BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 4; distance++) {
                int sampleX = worldX + stepX * distance;
                int sampleZ = worldZ + stepZ * distance;
                if (!isWalkableAt(sampleX, sampleZ)) {
                    continue;
                }
                return LevelZeroSurfaceBiome.sampleAtWorld(sampleX, sampleZ) == LevelZeroSurfaceBiome.RED ? maskBit : 0;
            }

            int fallbackX = worldX + stepX * BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 2;
            int fallbackZ = worldZ + stepZ * BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 2;
            return LevelZeroSurfaceBiome.sampleAtWorld(fallbackX, fallbackZ) == LevelZeroSurfaceBiome.RED ? maskBit : 0;
        }

        private boolean isWalkableAt(int worldX, int worldZ) {
            return LevelZeroLayout.isWalkableAtWorld(worldX, worldZ, layoutSeed);
        }
    }
}
