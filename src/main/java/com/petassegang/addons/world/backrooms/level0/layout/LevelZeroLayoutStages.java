package com.petassegang.addons.world.backrooms.level0.layout;

import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;

/**
 * Mini-stages de la version actuelle du layout Level 0.
 *
 * <p>Cette couche garde le comportement historique, mais isole les derives
 * logiques avant un futur decoupage plus fin de la pipeline.
 */
public final class LevelZeroLayoutStages {

    private LevelZeroLayoutStages() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Echantillonne la presence d'un neon pour une cellule logique.
     *
     * @param cellX coordonnee cellule X
     * @param cellZ coordonnee cellule Z
     * @param layoutSeed seed de layout
     * @param lightInterval modulo historique des neons
     * @return {@code true} si la cellule porte un neon
     */
    public static boolean sampleLightCell(int cellX, int cellZ, long layoutSeed, int lightInterval) {
        long seed = StageRandom.mixLegacy(layoutSeed, StageRandom.Stage.LIGHTS, cellX, cellZ);
        return Math.floorMod(seed, lightInterval) == 0;
    }

    /**
     * Echantillonne le biome cosmetique de surface d'une cellule logique.
     *
     * @param cellX coordonnee cellule X
     * @param cellZ coordonnee cellule Z
     * @return biome cosmetique de surface
     */
    public static LevelZeroSurfaceBiome sampleSurfaceBiomeCell(int cellX, int cellZ) {
        return LevelZeroSurfaceBiome.sampleAtCell(cellX, cellZ);
    }

    /**
     * Echantillonne le marquage de grande piece d'une cellule logique.
     *
     * @param cellX coordonnee cellule X
     * @param cellZ coordonnee cellule Z
     * @param layoutSeed seed de layout
     * @return {@code true} si la cellule appartient a une grande piece
     */
    public static boolean sampleLargeRoomCell(int cellX, int cellZ, long layoutSeed) {
        long hash = StageRandom.mixLegacy(layoutSeed, StageRandom.Stage.LARGE_ROOMS, cellX, cellZ);
        return Math.floorMod(hash, 4) == 0;
    }

    /**
     * Retourne la cle de cache historique d'un secteur.
     *
     * @param sectorX coordonnee secteur X
     * @param sectorZ coordonnee secteur Z
     * @param layoutSeed seed de layout
     * @return cle de cache deterministe
     */
    public static long sectorCacheKey(int sectorX, int sectorZ, long layoutSeed) {
        return StageRandom.mixLegacy(layoutSeed, StageRandom.Stage.SECTOR_CACHE, sectorX, sectorZ);
    }
}
