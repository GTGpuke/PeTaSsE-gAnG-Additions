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

import com.petassegang.addons.block.LevelZeroWallpaperBlock;
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.world.backrooms.BackroomsConstants;

/**
 * Generateur monocouche du Level 0 des Backrooms.
 */
public final class LevelZeroChunkGenerator extends ChunkGenerator {

    private static final int NORTH_MASK = 1;
    private static final int SOUTH_MASK = 1 << 1;
    private static final int WEST_MASK  = 1 << 2;
    private static final int EAST_MASK  = 1 << 3;
    private static final int FULL_MASK  = NORTH_MASK | SOUTH_MASK | WEST_MASK | EAST_MASK;

    /** Codec de serialisation du generateur. */
    public static final MapCodec<LevelZeroChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Biome.REGISTRY_CODEC.fieldOf("biome")
                            .forGetter(LevelZeroChunkGenerator::biome))
                    .apply(instance, LevelZeroChunkGenerator::new));

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
        long layoutSeed = resolveLayoutSeed(noiseConfig);
        LevelZeroLayout layout = LevelZeroLayout.generate(
                chunk.getPos().x,
                chunk.getPos().z,
                layoutSeed);
        LayoutSampler sampler = new LayoutSampler(layoutSeed);
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        BlockState bedrock         = Blocks.BEDROCK.getDefaultState();
        BlockState subfloor        = Blocks.SMOOTH_STONE.getDefaultState();
        BlockState wallpaper       = ModBlocks.LEVEL_ZERO_WALLPAPER.getDefaultState();
        BlockState altWallpaper    = ModBlocks.LEVEL_ZERO_WALLPAPER_AGED.getDefaultState();
        BlockState adaptWallpaper  = ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE.getDefaultState();
        BlockState ceiling         = ModBlocks.LEVEL_ZERO_CEILING_TILE.getDefaultState();
        BlockState light           = ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT.getDefaultState();
        int worldMinX = chunk.getPos().getStartX();
        int worldMinZ = chunk.getPos().getStartZ();

        for (int localX = 0; localX < LevelZeroLayout.CHUNK_SIZE; localX++) {
            int worldX = worldMinX + localX;
            for (int localZ = 0; localZ < LevelZeroLayout.CHUNK_SIZE; localZ++) {
                int worldZ = worldMinZ + localZ;
                BlockState floor = resolveFloorState(layout, localX, localZ);
                chunk.setBlockState(mutablePos.set(localX, BackroomsConstants.LEVEL_ZERO_BEDROCK_Y, localZ), bedrock, false);
                chunk.setBlockState(mutablePos.set(localX, BackroomsConstants.LEVEL_ZERO_SUBFLOOR_Y, localZ), subfloor, false);

                if (layout.isWalkable(localX, localZ)) {
                    chunk.setBlockState(mutablePos.set(localX, BackroomsConstants.LEVEL_ZERO_FLOOR_Y, localZ), floor, false);
                    for (int y = BackroomsConstants.LEVEL_ZERO_AIR_MIN_Y; y <= BackroomsConstants.LEVEL_ZERO_AIR_MAX_Y; y++) {
                        chunk.setBlockState(mutablePos.set(localX, y, localZ), Blocks.AIR.getDefaultState(), false);
                    }
                    BlockState ceilingState = layout.hasLight(localX, localZ) ? light : ceiling;
                    chunk.setBlockState(mutablePos.set(localX, BackroomsConstants.LEVEL_ZERO_CEILING_Y, localZ), ceilingState, false);
                } else {
                    chunk.setBlockState(mutablePos.set(localX, BackroomsConstants.LEVEL_ZERO_FLOOR_Y, localZ), floor, false);
                    boolean exposedWallpaper = sampler.isWallpaperExposed(worldX, worldZ);
                    int faceMask = exposedWallpaper ? sampler.sampleWallpaperFaceMask(worldX, worldZ) : 0;
                    // Le faceMask est encode directement dans le block state — pas de block entity.
                    BlockState wallState = resolveWallState(
                            exposedWallpaper, faceMask, wallpaper, altWallpaper, adaptWallpaper, bedrock);
                    for (int y = BackroomsConstants.LEVEL_ZERO_AIR_MIN_Y; y <= BackroomsConstants.LEVEL_ZERO_AIR_MAX_Y; y++) {
                        chunk.setBlockState(mutablePos.set(localX, y, localZ), wallState, false);
                    }
                    chunk.setBlockState(mutablePos.set(localX, BackroomsConstants.LEVEL_ZERO_CEILING_Y, localZ), ceiling, false);
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
        return BackroomsConstants.LEVEL_ZERO_CEILING_Y + 1;
    }

    @Override
    public VerticalBlockSample getColumnSample(int x,
                                               int z,
                                               HeightLimitView world,
                                               NoiseConfig noiseConfig) {
        BlockState[] states = new BlockState[128];
        for (int i = 0; i < states.length; i++) {
            states[i] = Blocks.AIR.getDefaultState();
        }

        setColumnState(states, BackroomsConstants.LEVEL_ZERO_BEDROCK_Y, Blocks.BEDROCK.getDefaultState());
        setColumnState(states, BackroomsConstants.LEVEL_ZERO_SUBFLOOR_Y, Blocks.SMOOTH_STONE.getDefaultState());

        long layoutSeed = resolveLayoutSeed(noiseConfig);
        LevelZeroLayout layout = LevelZeroLayout.generate(
                Math.floorDiv(x, LevelZeroLayout.CHUNK_SIZE),
                Math.floorDiv(z, LevelZeroLayout.CHUNK_SIZE),
                layoutSeed);
        LayoutSampler sampler = new LayoutSampler(layoutSeed);
        int localX = Math.floorMod(x, LevelZeroLayout.CHUNK_SIZE);
        int localZ = Math.floorMod(z, LevelZeroLayout.CHUNK_SIZE);
        boolean walkable = layout.isWalkable(localX, localZ);
        setColumnState(states, BackroomsConstants.LEVEL_ZERO_FLOOR_Y, resolveFloorState(layout, localX, localZ));

        if (walkable) {
            setColumnState(states,
                    BackroomsConstants.LEVEL_ZERO_CEILING_Y,
                    layout.hasLight(localX, localZ)
                            ? ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT.getDefaultState()
                            : ModBlocks.LEVEL_ZERO_CEILING_TILE.getDefaultState());
        } else {
            boolean exposedWallpaper = sampler.isWallpaperExposed(x, z);
            int faceMask = exposedWallpaper ? sampler.sampleWallpaperFaceMask(x, z) : 0;
            BlockState wallState = resolveWallState(
                    exposedWallpaper,
                    faceMask,
                    ModBlocks.LEVEL_ZERO_WALLPAPER.getDefaultState(),
                    ModBlocks.LEVEL_ZERO_WALLPAPER_AGED.getDefaultState(),
                    ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE.getDefaultState(),
                    Blocks.BEDROCK.getDefaultState());
            for (int y = BackroomsConstants.LEVEL_ZERO_AIR_MIN_Y; y <= BackroomsConstants.LEVEL_ZERO_AIR_MAX_Y; y++) {
                setColumnState(states, y, wallState);
            }
            setColumnState(states, BackroomsConstants.LEVEL_ZERO_CEILING_Y, ModBlocks.LEVEL_ZERO_CEILING_TILE.getDefaultState());
        }

        return new VerticalBlockSample(world.getBottomY(), states);
    }

    @Override
    public int getWorldHeight() {
        // Correspond au "height" du fichier backrooms_level_0_type.json.
        return 128;
    }

    @Override
    public int getSeaLevel() {
        return BackroomsConstants.LEVEL_ZERO_FLOOR_Y;
    }

    @Override
    public int getMinimumY() {
        return 0;
    }

    @Override
    public void getDebugHudText(List<String> text,
                                NoiseConfig noiseConfig,
                                BlockPos pos) {
        text.add("Backrooms : Level 0");
        text.add("ChunkGenerator : Traduction monocouche du script Python.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void setColumnState(BlockState[] states, int y, BlockState state) {
        if (y >= 0 && y < states.length) {
            states[y] = state;
        }
    }

    private static BlockState resolveFloorState(LevelZeroLayout layout, int localX, int localZ) {
        return switch (layout.floorVariant(localX, localZ)) {
            case LevelZeroLayout.SURFACE_VARIANT_ALTERNATE ->
                    ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED.getDefaultState();
            default -> ModBlocks.LEVEL_ZERO_DAMP_CARPET.getDefaultState();
        };
    }

    /**
     * Retourne le block state de mur adequat.
     *
     * <p>Pour les blocs adaptatifs (faceMask mixte), le masque est encode
     * directement dans la propriete {@link LevelZeroWallpaperBlock#FACE_MASK}
     * du block state — aucune block entity n'est creee.
     */
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
            return adaptiveWallpaper.with(LevelZeroWallpaperBlock.FACE_MASK, faceMask);
        }
        if (faceMask == FULL_MASK) {
            return alternateWallpaper;
        }
        return wallpaper;
    }

    private static boolean isMixedFaceMask(int faceMask) {
        return faceMask != 0 && faceMask != FULL_MASK;
    }

    private static long resolveLayoutSeed(NoiseConfig noiseConfig) {
        return noiseConfig.getOrCreateRandomDeriver(BackroomsConstants.LEVEL_ZERO_LAYOUT_RANDOM)
                .split(BlockPos.ORIGIN)
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
            faceMask |= sampleFace(worldX, worldZ, 0,  1, SOUTH_MASK);
            faceMask |= sampleFace(worldX, worldZ, -1, 0, WEST_MASK);
            faceMask |= sampleFace(worldX, worldZ,  1, 0, EAST_MASK);
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
