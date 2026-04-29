package com.petassegang.addons.backrooms.level.level0.generation.stage.biome;

import com.petassegang.addons.backrooms.level.level0.biome.LevelZeroSurfaceBiome;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroCellContext;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroCellStage;

/**
 * Etape de derivation du biome cosmetique de surface.
 *
 * <p>Elle reste volontairement triviale : le vrai catalogue des biomes et leur
 * logique de layer vivent dans {@code LevelZeroSurfaceBiome}, cette classe ne
 * fait que brancher cette lecture dans la pipeline.
 */
public final class LevelZeroSurfaceBiomeStage implements LevelZeroCellStage<LevelZeroSurfaceBiome> {

    @Override
    public LevelZeroSurfaceBiome sample(LevelZeroCellContext context) {
        return LevelZeroSurfaceBiome.sampleAtCell(context.cellX(), context.cellZ(), context.layerIndex());
    }
}
