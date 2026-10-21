package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.write;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import com.gtgpuke.gtgsbackrooms.backrooms.level0.biome.LevelZeroSurfaceBiome;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.block.LevelZeroWallpaperBlock;
import com.gtgpuke.gtgsbackrooms.core.registry.ModBlocks;

/**
 * Blocs utilises par le writer du Level 0, choisis une fois pour toutes.
 *
 * <p>La palette repond a "quel bloc pour cet etat ?" sans jamais poser de bloc.
 * Les seize variantes de mur sont precalculees pour eviter un {@code with()}
 * par colonne.
 */
public final class LevelZeroBlockPalette {

    private final BlockState baseFloor;
    private final BlockState alternateFloor;
    private final BlockState ceilingTile;
    private final BlockState light;
    private final BlockState bedrock;
    private final BlockState air;
    private final BlockState[] wallpaperByFaceMask;

    /**
     * Construit une palette explicite (tests, benchmark).
     *
     * @param baseFloor sol du biome de base
     * @param alternateFloor sol du biome alternatif
     * @param ceilingTile dalle de plafond
     * @param light neon de plafond
     * @param wallpaper mur visible ; sa propriete face_mask est renseignee si elle existe
     * @param bedrock bloc des murs enfouis et de la bedrock inter-layer
     */
    public LevelZeroBlockPalette(BlockState baseFloor,
                                 BlockState alternateFloor,
                                 BlockState ceilingTile,
                                 BlockState light,
                                 BlockState wallpaper,
                                 BlockState bedrock) {
        this.baseFloor = baseFloor;
        this.alternateFloor = alternateFloor;
        this.ceilingTile = ceilingTile;
        this.light = light;
        this.bedrock = bedrock;
        this.air = Blocks.AIR.getDefaultState();
        this.wallpaperByFaceMask = new BlockState[LevelZeroWallpaperBlock.FULL_MASK + 1];
        for (int mask = 0; mask <= LevelZeroWallpaperBlock.FULL_MASK; mask++) {
            wallpaperByFaceMask[mask] = wallpaper.contains(LevelZeroWallpaperBlock.FACE_MASK)
                    ? wallpaper.with(LevelZeroWallpaperBlock.FACE_MASK, mask)
                    : wallpaper;
        }
    }

    /**
     * Palette runtime basee sur les blocs du mod.
     *
     * @return palette du Level 0
     */
    public static LevelZeroBlockPalette fromRegistry() {
        return new LevelZeroBlockPalette(
                ModBlocks.LEVEL_ZERO_DAMP_CARPET.getDefaultState(),
                ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED.getDefaultState(),
                ModBlocks.LEVEL_ZERO_CEILING_TILE.getDefaultState(),
                ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT.getDefaultState(),
                ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE.getDefaultState(),
                Blocks.BEDROCK.getDefaultState());
    }

    /** Sol du biome donne. */
    public BlockState floor(LevelZeroSurfaceBiome biome) {
        return biome == LevelZeroSurfaceBiome.RED ? alternateFloor : baseFloor;
    }

    /** Dalle de plafond standard. */
    public BlockState ceilingTile() {
        return ceilingTile;
    }

    /** Neon de plafond. */
    public BlockState light() {
        return light;
    }

    /** Bedrock des murs enfouis et des separations inter-layer. */
    public BlockState bedrock() {
        return bedrock;
    }

    /** Air des colonnes traversables. */
    public BlockState air() {
        return air;
    }

    /**
     * Mur visible avec ses faces blanches.
     *
     * @param wallpaperFaceMask faces affichant la variante blanche
     * @return etat de mur
     */
    public BlockState wallpaper(int wallpaperFaceMask) {
        return wallpaperByFaceMask[wallpaperFaceMask & LevelZeroWallpaperBlock.FULL_MASK];
    }
}
