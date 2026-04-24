package com.petassegang.addons.world.backrooms.level0.write;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import com.petassegang.addons.block.backrooms.LevelZeroWallpaperBlock;
import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;
import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellState;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;

/**
 * Palette de blocs du Level 0 et traduction des variantes de layout vers les
 * {@link BlockState} places dans les chunks.
 *
 * <p>Cette classe repond a la question : "quels blocs utiliser pour cet etat
 * semantique ?". Elle choisit les materiaux, mais ne les pose jamais elle-meme.
 */
public final class LevelZeroBlockPalette {

    private final BlockState baseFloorState;
    private final BlockState alternateFloorState;
    private final BlockState ceilingTileState;
    private final BlockState fluorescentLightState;
    private final BlockState exposedWallpaperState;
    private final BlockState airState;
    private final BlockState bedrockState;
    private final BlockState subfloorState;

    /**
     * Construit la palette runtime du Level 0 en s'appuyant sur les registres
     * reels du mod.
     */
    public LevelZeroBlockPalette() {
        this(
                ModBlocks.LEVEL_ZERO_DAMP_CARPET.getDefaultState(),
                ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED.getDefaultState(),
                ModBlocks.LEVEL_ZERO_CEILING_TILE.getDefaultState(),
                ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT.getDefaultState(),
                ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE.getDefaultState(),
                Blocks.AIR.getDefaultState(),
                Blocks.BEDROCK.getDefaultState(),
                Blocks.SMOOTH_STONE.getDefaultState());
    }

    /**
     * Construit une palette injectable pour les tests et les echantillons hors
     * runtime Minecraft complet.
     *
     * @param baseFloorState bloc de sol du biome de base
     * @param alternateFloorState bloc de sol du biome alternatif
     * @param ceilingTileState dalle de plafond standard
     * @param fluorescentLightState bloc lumineux de plafond
     * @param exposedWallpaperState bloc de mur expose a l'air
     * @param airState bloc d'air interne pour les colonnes traversables
     * @param bedrockState bloc de bedrock interne
     * @param subfloorState bloc du sous-sol technique
     */
    public LevelZeroBlockPalette(BlockState baseFloorState,
                                 BlockState alternateFloorState,
                                 BlockState ceilingTileState,
                                 BlockState fluorescentLightState,
                                 BlockState exposedWallpaperState,
                                 BlockState airState,
                                 BlockState bedrockState,
                                 BlockState subfloorState) {
        this.baseFloorState = baseFloorState;
        this.alternateFloorState = alternateFloorState;
        this.ceilingTileState = ceilingTileState;
        this.fluorescentLightState = fluorescentLightState;
        this.exposedWallpaperState = exposedWallpaperState;
        this.airState = airState;
        this.bedrockState = bedrockState;
        this.subfloorState = subfloorState;
    }

    /**
     * Construit une palette injectable en reutilisant l'air vanilla.
     *
     * @param baseFloorState bloc de sol du biome de base
     * @param alternateFloorState bloc de sol du biome alternatif
     * @param ceilingTileState dalle de plafond standard
     * @param fluorescentLightState bloc lumineux de plafond
     * @param exposedWallpaperState bloc de mur expose a l'air
     * @param bedrockState bloc de bedrock interne
     * @param subfloorState bloc du sous-sol technique
     */
    public LevelZeroBlockPalette(BlockState baseFloorState,
                                 BlockState alternateFloorState,
                                 BlockState ceilingTileState,
                                 BlockState fluorescentLightState,
                                 BlockState exposedWallpaperState,
                                 BlockState bedrockState,
                                 BlockState subfloorState) {
        this(
                baseFloorState,
                alternateFloorState,
                ceilingTileState,
                fluorescentLightState,
                exposedWallpaperState,
                Blocks.AIR.getDefaultState(),
                bedrockState,
                subfloorState);
    }

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
            case LevelZeroLayout.SURFACE_VARIANT_ALTERNATE -> alternateFloorState;
            default -> baseFloorState;
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
     * @return dalle de plafond standard
     */
    public BlockState walkableCeiling(LevelZeroCellState cellState) {
        return ceilingTileState;
    }

    /**
     * Retourne le bloc de neon fluorescent du Level 0.
     *
     * @return bloc lumineux de plafond
     */
    public BlockState lightFixture() {
        return fluorescentLightState;
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
                    airState,
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
            return bedrockState;
        }
        if (!exposedWallpaperState.contains(LevelZeroWallpaperBlock.FACE_MASK)) {
            return exposedWallpaperState;
        }
        return exposedWallpaperState
                .with(LevelZeroWallpaperBlock.FACE_MASK, faceMask);
    }

    /**
     * Retourne le bloc de plafond standard des colonnes de mur.
     *
     * @return dalle de plafond standard
     */
    public BlockState wallCeiling() {
        return ceilingTileState;
    }

    /**
     * Retourne la couche de bedrock basse.
     *
     * @return etat bedrock
     */
    public BlockState bedrock() {
        return bedrockState;
    }

    /**
     * Retourne le sous-sol technique.
     *
     * @return etat smooth stone
     */
    public BlockState subfloor() {
        return subfloorState;
    }

    private BlockState debugInterior(LevelZeroCellState cellState, boolean exposedWallpaper, int faceMask) {
        if (ModConfig.DEBUG_LEVEL_ZERO_MICRO_GEOMETRY && cellState.walkable() && !cellState.isMicroOpen()) {
            return debugWall(cellState);
        }
        return wall(exposedWallpaper, faceMask);
    }

    private static boolean hasDebugGeometry(LevelZeroCellState cellState) {
        return cellState.geometryMask() != LevelZeroGeometryMask.none();
    }

    private static BlockState debugFloor(LevelZeroCellState cellState) {
        return Blocks.WHITE_CONCRETE.getDefaultState();
    }

    private static BlockState debugWall(LevelZeroCellState cellState) {
        return Blocks.GRAY_CONCRETE.getDefaultState();
    }
}
