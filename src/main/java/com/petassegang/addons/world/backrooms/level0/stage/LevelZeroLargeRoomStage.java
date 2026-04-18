package com.petassegang.addons.world.backrooms.level0.stage;

import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;

/**
 * Etape historique de marquage des grandes pieces.
 */
public final class LevelZeroLargeRoomStage implements LevelZeroCellStage<Boolean> {

    @Override
    public Boolean sample(LevelZeroCellContext context) {
        long hash = StageRandom.mixLegacy(
                context.layoutSeed(),
                StageRandom.Stage.LARGE_ROOMS,
                context.cellX(),
                context.cellZ());
        return Math.floorMod(hash, 4) == 0;
    }
}
