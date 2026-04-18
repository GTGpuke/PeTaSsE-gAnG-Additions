package com.petassegang.addons.world.backrooms.level0.stage;

import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;

/**
 * Etape historique de placement des neons du Level 0.
 */
public final class LevelZeroLightStage implements LevelZeroCellStage<Boolean> {

    private final int lightInterval;

    /**
     * Construit l'etape historique de placement des neons.
     *
     * @param lightInterval modulo historique des neons
     */
    public LevelZeroLightStage(int lightInterval) {
        this.lightInterval = lightInterval;
    }

    @Override
    public Boolean sample(LevelZeroCellContext context) {
        long seed = StageRandom.mixLegacy(
                context.layoutSeed(),
                StageRandom.Stage.LIGHTS,
                context.cellX(),
                context.cellZ());
        return Math.floorMod(seed, lightInterval) == 0;
    }
}
