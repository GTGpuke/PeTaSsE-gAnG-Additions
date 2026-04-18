package com.petassegang.addons.world.backrooms.level0.write;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import com.petassegang.addons.block.backrooms.LevelZeroWallpaperBlock;
import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;
import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellState;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroLayoutSampler;

/**
 * Palette de blocs du Level 0 et traduction des variantes de layout vers les
 * {@link BlockState} places dans les chunks.
 */
public final class LevelZeroBlockPalette {

    /**
     * Retourne l'etat du bloc de sol a la position demandee.
     *
     * @param layout layout local du chunk
     * @param localX coordonnee X locale
     * @param localZ coordonnee Z locale
     * @return bloc de sol correspondant au biome de surface
     */
    public BlockState floor(LevelZeroLayout layout, int localX, int localZ) {
        return floor(layout.cellState(localX, localZ));
    }

    /**
     * Retourne l'etat du bloc de sol a partir d'un etat semantique de cellule.
     *
     * @param cellState etat semantique de la cellule
     * @return bloc de sol correspondant au biome de surface
     */
    public BlockState floor(LevelZeroCellState cellState) {
        if (ModConfig.DEBUG_LEVEL_ZERO_MICRO_GEOMETRY && hasDebugGeometry(cellState)) {
            return debugFloor(cellState);
        }
        return floor(cellState.surfaceBiome());
    }

    /**
     * Retourne l'etat du bloc de sol a partir d'un biome de surface.
     *
     * @param surfaceBiome biome cosmetique de surface
     * @return bloc de sol associe
     */
    public BlockState floor(LevelZeroSurfaceBiome surfaceBiome) {
        return switch (surfaceBiome.floorVariant()) {
            case LevelZeroLayout.SURFACE_VARIANT_ALTERNATE ->
                    ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED.getDefaultState();
            default -> ModBlocks.LEVEL_ZERO_DAMP_CARPET.getDefaultState();
        };
    }

    /**
     * Retourne l'etat du plafond pour une colonne traversable.
     *
     * @param layout layout local du chunk
     * @param localX coordonnee X locale
     * @param localZ coordonnee Z locale
     * @return dalle de plafond ou neon fluorescent
     */
    public BlockState walkableCeiling(LevelZeroLayout layout, int localX, int localZ) {
        return walkableCeiling(layout.cellState(localX, localZ));
    }

    /**
     * Retourne l'etat du plafond pour une cellule traversable.
     *
     * @param cellState etat semantique de la cellule
     * @return dalle de plafond ou neon fluorescent
     */
    public BlockState walkableCeiling(LevelZeroCellState cellState) {
        return cellState.lighted()
                ? ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT.getDefaultState()
                : ModBlocks.LEVEL_ZERO_CEILING_TILE.getDefaultState();
    }

    /**
     * Construit la traduction bloc minimale d'une colonne a partir de son etat
     * semantique et de la visibilite du mur.
     *
     * @param cellState etat semantique de la cellule
     * @param exposedWallpaper indique si le mur est expose a l'air
     * @param faceMask masque des faces alternatives
     * @return materiaux de la colonne finale
     */
    public LevelZeroColumnMaterial column(LevelZeroCellState cellState,
                                          boolean exposedWallpaper,
                                          int faceMask) {
        if (cellState.isLocallyWalkable()) {
            return new LevelZeroColumnMaterial(
                    true,
                    floor(cellState),
                    Blocks.AIR.getDefaultState(),
                    walkableCeiling(cellState));
        }
        return new LevelZeroColumnMaterial(
                false,
                floor(cellState),
                debugInterior(cellState, exposedWallpaper, faceMask),
                wallCeiling());
    }

    /**
     * Retourne l'etat de mur approprie pour une colonne non traversable.
     *
     * @param exposedWallpaper indique si le mur est expose a l'air
     * @param faceMask masque des faces alternatives
     * @return bloc de mur final
     */
    public BlockState wall(boolean exposedWallpaper, int faceMask) {
        if (!exposedWallpaper) {
            return Blocks.BEDROCK.getDefaultState();
        }
        if (isMixedFaceMask(faceMask)) {
            return ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE.getDefaultState()
                    .with(LevelZeroWallpaperBlock.FACE_MASK, faceMask);
        }
        if (faceMask == LevelZeroLayoutSampler.FULL_MASK) {
            return ModBlocks.LEVEL_ZERO_WALLPAPER_AGED.getDefaultState();
        }
        return ModBlocks.LEVEL_ZERO_WALLPAPER.getDefaultState();
    }

    /**
     * Retourne le bloc de plafond standard des colonnes de mur.
     *
     * @return dalle de plafond standard
     */
    public BlockState wallCeiling() {
        return ModBlocks.LEVEL_ZERO_CEILING_TILE.getDefaultState();
    }

    /**
     * Retourne la couche de bedrock basse.
     *
     * @return etat bedrock
     */
    public BlockState bedrock() {
        return Blocks.BEDROCK.getDefaultState();
    }

    /**
     * Retourne le sous-sol technique.
     *
     * @return etat smooth stone
     */
    public BlockState subfloor() {
        return Blocks.SMOOTH_STONE.getDefaultState();
    }

    private static boolean isMixedFaceMask(int faceMask) {
        return faceMask != 0 && faceMask != LevelZeroLayoutSampler.FULL_MASK;
    }

    private BlockState debugInterior(LevelZeroCellState cellState, boolean exposedWallpaper, int faceMask) {
        if (ModConfig.DEBUG_LEVEL_ZERO_MICRO_GEOMETRY && cellState.walkable() && !cellState.isMicroOpen()) {
            return debugWall(cellState);
        }
        return wall(exposedWallpaper, faceMask);
    }

    private static boolean hasDebugGeometry(LevelZeroCellState cellState) {
        return cellState.hasGeometryFeature(LevelZeroGeometryFeature.OFFSET_WALL)
                || cellState.hasGeometryFeature(LevelZeroGeometryFeature.HALF_WALL)
                || cellState.hasGeometryFeature(LevelZeroGeometryFeature.RECESS)
                || cellState.hasGeometryFeature(LevelZeroGeometryFeature.ALCOVE)
                || cellState.hasGeometryFeature(LevelZeroGeometryFeature.PINCH_1WIDE);
    }

    private static BlockState debugFloor(LevelZeroCellState cellState) {
        return switch (primaryFeature(cellState)) {
            case OFFSET_WALL -> Blocks.LIGHT_BLUE_CONCRETE.getDefaultState();
            case HALF_WALL -> Blocks.ORANGE_CONCRETE.getDefaultState();
            case RECESS -> Blocks.LIME_CONCRETE.getDefaultState();
            case ALCOVE -> Blocks.MAGENTA_CONCRETE.getDefaultState();
            case PINCH_1WIDE -> Blocks.RED_CONCRETE.getDefaultState();
            default -> Blocks.WHITE_CONCRETE.getDefaultState();
        };
    }

    private static BlockState debugWall(LevelZeroCellState cellState) {
        return switch (primaryFeature(cellState)) {
            case OFFSET_WALL -> Blocks.BLUE_CONCRETE.getDefaultState();
            case HALF_WALL -> Blocks.ORANGE_TERRACOTTA.getDefaultState();
            case RECESS -> Blocks.GREEN_CONCRETE.getDefaultState();
            case ALCOVE -> Blocks.PURPLE_CONCRETE.getDefaultState();
            case PINCH_1WIDE -> Blocks.RED_TERRACOTTA.getDefaultState();
            default -> Blocks.GRAY_CONCRETE.getDefaultState();
        };
    }

    private static LevelZeroGeometryFeature primaryFeature(LevelZeroCellState cellState) {
        if (cellState.hasGeometryFeature(LevelZeroGeometryFeature.PINCH_1WIDE)) {
            return LevelZeroGeometryFeature.PINCH_1WIDE;
        }
        if (cellState.hasGeometryFeature(LevelZeroGeometryFeature.ALCOVE)) {
            return LevelZeroGeometryFeature.ALCOVE;
        }
        if (cellState.hasGeometryFeature(LevelZeroGeometryFeature.RECESS)) {
            return LevelZeroGeometryFeature.RECESS;
        }
        if (cellState.hasGeometryFeature(LevelZeroGeometryFeature.HALF_WALL)) {
            return LevelZeroGeometryFeature.HALF_WALL;
        }
        if (cellState.hasGeometryFeature(LevelZeroGeometryFeature.OFFSET_WALL)) {
            return LevelZeroGeometryFeature.OFFSET_WALL;
        }
        return LevelZeroGeometryFeature.NONE;
    }
}
