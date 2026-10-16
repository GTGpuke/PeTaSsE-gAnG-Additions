package com.petassegang.addons.backrooms.level0.biome;

import com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroCoords;
import com.petassegang.addons.backrooms.level0.generation.noise.StageRandom;

/**
 * Biomes cosmetiques du Level 0 : palette de surfaces et densite lumineuse par grande region.
 *
 * <p>Un biome ne change jamais la topologie du labyrinthe. Il est tire par
 * region de {@value #REGION_SIZE_CELLS} cellules et depend du layer, sans
 * dependre du seed monde : client et serveur retrouvent la meme palette sans
 * synchronisation.
 */
public enum LevelZeroSurfaceBiome {

    /** Palette classique : papier peint jauni, moquette humide, neons pleins. */
    BASE(0, 17, 1, 1, 31, false, -1),
    /** Palette alternative : murs blancs, moquette rouge, neons rares, regions sombres possibles. */
    RED(1, 1, 0, 1, 19, true, layerMask(1, 3));

    /** Taille d'une region cosmetique en cellules logiques. */
    public static final int REGION_SIZE_CELLS = 48;

    private static final int FULL_DARK_REGION_MODULO = 4;
    private static final int LARGE_ROOM_BLACKOUT_MACRO_CELLS = 24;
    private static final LevelZeroSurfaceBiome[] VALUES = values();

    private final int id;
    private final int regionSpawnWeight;
    private final int lightDensityNumerator;
    private final int lightDensityDenominator;
    private final int largeRoomBlackoutModulo;
    private final boolean supportsFullDarkRegions;
    private final int allowedLayerMask;

    LevelZeroSurfaceBiome(int id,
                          int regionSpawnWeight,
                          int lightDensityNumerator,
                          int lightDensityDenominator,
                          int largeRoomBlackoutModulo,
                          boolean supportsFullDarkRegions,
                          int allowedLayerMask) {
        this.id = id;
        this.regionSpawnWeight = regionSpawnWeight;
        this.lightDensityNumerator = lightDensityNumerator;
        this.lightDensityDenominator = lightDensityDenominator;
        this.largeRoomBlackoutModulo = largeRoomBlackoutModulo;
        this.supportsFullDarkRegions = supportsFullDarkRegions;
        this.allowedLayerMask = allowedLayerMask;
    }

    /** {@code true} si ce biome peut produire des regions entierement sombres. */
    public boolean supportsFullDarkRegions() {
        return supportsFullDarkRegions;
    }

    /** {@code true} si ce biome peut apparaitre sur le layer donne. */
    public boolean supportsLayer(int layerIndex) {
        return allowedLayerMask < 0 || ((allowedLayerMask >>> layerIndex) & 1) != 0;
    }

    /**
     * {@code true} si ce biome conserve le neon candidat de la trame globale a cette cellule.
     *
     * @param cellX coordonnee X en cellules
     * @param cellZ coordonnee Z en cellules
     * @param layoutSeed seed de layout
     * @return {@code true} si le neon candidat est garde
     */
    public boolean keepsStripedLight(int cellX, int cellZ, long layoutSeed) {
        // Densite pleine (1/1) : toujours vrai ; nulle (0/1) : toujours faux ; entre les deux, le hash tranche.
        long hash = StageRandom.mix(layoutSeed ^ id, StageRandom.Stage.LIGHTS, cellX, cellZ);
        return Math.floorMod(hash, lightDensityDenominator) < lightDensityNumerator;
    }

    /**
     * {@code true} si la grande piece a cette cellule doit rester sans neon.
     *
     * <p>Decide a l'echelle macro (24 cellules) pour obtenir des coupures nettes et rares.
     *
     * @param cellX coordonnee X en cellules
     * @param cellZ coordonnee Z en cellules
     * @param layoutSeed seed de layout
     * @return {@code true} si la zone est en blackout
     */
    public boolean isLargeRoomBlackout(int cellX, int cellZ, long layoutSeed) {
        long hash = StageRandom.mix(
                layoutSeed ^ (id * 31L),
                StageRandom.Stage.LARGE_ROOM_LIGHTING,
                Math.floorDiv(cellX, LARGE_ROOM_BLACKOUT_MACRO_CELLS),
                Math.floorDiv(cellZ, LARGE_ROOM_BLACKOUT_MACRO_CELLS));
        return Math.floorMod(hash, largeRoomBlackoutModulo) == 0;
    }

    /**
     * {@code true} si la cellule appartient a une region cosmetique entierement sombre.
     *
     * @param cellX coordonnee X en cellules
     * @param cellZ coordonnee Z en cellules
     * @param layoutSeed seed de layout
     * @return {@code true} si la region est sans neon
     */
    public boolean isFullDarkRegion(int cellX, int cellZ, long layoutSeed) {
        if (!supportsFullDarkRegions) {
            return false;
        }
        long hash = StageRandom.mix(
                layoutSeed,
                StageRandom.Stage.BIOME_LIGHTING,
                Math.floorDiv(cellX, REGION_SIZE_CELLS),
                Math.floorDiv(cellZ, REGION_SIZE_CELLS));
        return Math.floorMod(hash, FULL_DARK_REGION_MODULO) == 0;
    }

    /**
     * Biome d'une position monde sur un layer donne.
     *
     * @param worldX coordonnee monde X
     * @param worldZ coordonnee monde Z
     * @param layerIndex index du layer
     * @return biome cosmetique
     */
    public static LevelZeroSurfaceBiome sampleAtWorld(int worldX, int worldZ, int layerIndex) {
        return sampleAtCell(LevelZeroCoords.worldToCell(worldX), LevelZeroCoords.worldToCell(worldZ), layerIndex);
    }

    /**
     * Biome d'une cellule logique sur un layer donne.
     *
     * @param cellX coordonnee X en cellules
     * @param cellZ coordonnee Z en cellules
     * @param layerIndex index du layer
     * @return biome cosmetique
     */
    public static LevelZeroSurfaceBiome sampleAtCell(int cellX, int cellZ, int layerIndex) {
        return sampleAtRegion(
                Math.floorDiv(cellX, REGION_SIZE_CELLS),
                Math.floorDiv(cellZ, REGION_SIZE_CELLS),
                layerIndex);
    }

    /**
     * Biome d'une region cosmetique sur un layer donne.
     *
     * @param regionX coordonnee X de la region
     * @param regionZ coordonnee Z de la region
     * @param layerIndex index du layer
     * @return biome cosmetique
     */
    public static LevelZeroSurfaceBiome sampleAtRegion(int regionX, int regionZ, int layerIndex) {
        // Le seed monde n'entre pas dans le hash : le client peut retrouver la palette seul.
        long hash = StageRandom.mix(
                0L,
                StageRandom.Stage.SURFACE_BIOME,
                regionX,
                ((((long) regionZ) & 0xffffffffL) << 8) ^ layerIndex);
        // Le biome de base est admis sur tous les layers : le poids total n'est jamais nul.
        int totalWeight = 0;
        for (LevelZeroSurfaceBiome biome : VALUES) {
            if (biome.supportsLayer(layerIndex)) {
                totalWeight += biome.regionSpawnWeight;
            }
        }
        int pick = Math.floorMod(hash, totalWeight);
        LevelZeroSurfaceBiome picked = BASE;
        for (LevelZeroSurfaceBiome biome : VALUES) {
            if (biome.supportsLayer(layerIndex)) {
                if (pick < biome.regionSpawnWeight) {
                    picked = biome;
                    break;
                }
                pick -= biome.regionSpawnWeight;
            }
        }
        return picked;
    }

    private static int layerMask(int... layers) {
        int mask = 0;
        for (int layer : layers) {
            mask |= 1 << layer;
        }
        return mask;
    }
}
