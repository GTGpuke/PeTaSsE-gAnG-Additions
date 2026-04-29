package com.petassegang.addons.backrooms.level.level0.generation.stage.region;

import com.petassegang.addons.backrooms.level.level0.generation.noise.StageRandom;

/**
 * Etape historique de derivation des cles de cache de secteurs.
 */
public final class LevelZeroSectorCacheKeyStage {

    /**
     * Derive une cle de cache deterministe pour un secteur logique.
     *
     * @param sectorX coordonnee secteur X
     * @param sectorZ coordonnee secteur Z
     * @param layoutSeed seed de layout
     * @return cle de cache deterministe
     */
    public long sample(int sectorX, int sectorZ, long layoutSeed) {
        return StageRandom.mixLegacy(layoutSeed, StageRandom.Stage.SECTOR_CACHE, sectorX, sectorZ);
    }
}
