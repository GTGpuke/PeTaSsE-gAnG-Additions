package com.petassegang.addons.world.backrooms.level0.stage;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionLayout;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionLayoutBuilder;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionWalkability;

/**
 * Etape regionale legacy produisant une region canonique de cellules.
 */
public final class LevelZeroLegacyRegionLayoutStage implements LevelZeroRegionStage<LevelZeroRegionLayout> {

    private final LevelZeroRegionLayoutBuilder regionLayoutBuilder;
    private final LevelZeroRegionStage<LevelZeroRegionWalkability> walkabilityStage;

    /**
     * Construit l'etape regionale legacy.
     *
     * @param regionLayoutBuilder builder canonique de region
     * @param walkabilityStage etape regionale de traversabilite
     */
    public LevelZeroLegacyRegionLayoutStage(LevelZeroRegionLayoutBuilder regionLayoutBuilder,
                                            LevelZeroRegionStage<LevelZeroRegionWalkability> walkabilityStage) {
        this.regionLayoutBuilder = regionLayoutBuilder;
        this.walkabilityStage = walkabilityStage;
    }

    @Override
    public LevelZeroRegionLayout sample(LevelZeroRegionContext context) {
        return regionLayoutBuilder.build(context.layoutSeed(), walkabilityStage.sample(context));
    }
}
