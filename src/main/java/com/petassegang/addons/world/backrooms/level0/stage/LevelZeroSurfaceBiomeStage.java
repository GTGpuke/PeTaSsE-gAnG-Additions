package com.petassegang.addons.world.backrooms.level0.stage;

import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;

/**
 * Etape historique d'echantillonnage des biomes cosmetiques de surface.
 */
public final class LevelZeroSurfaceBiomeStage implements LevelZeroCellStage<LevelZeroSurfaceBiome> {

    @Override
    public LevelZeroSurfaceBiome sample(LevelZeroCellContext context) {
        return LevelZeroSurfaceBiome.sampleAtCell(context.cellX(), context.cellZ());
    }
}
