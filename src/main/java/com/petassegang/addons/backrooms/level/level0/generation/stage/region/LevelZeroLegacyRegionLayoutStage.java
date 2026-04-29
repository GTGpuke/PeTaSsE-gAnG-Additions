package com.petassegang.addons.backrooms.level.level0.generation.stage.region;

import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroRegionLayout;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroRegionLayoutBuilder;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroRegionWalkability;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroRegionContext;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroRegionStage;

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
        // On separe volontairement les deux temps :
        // 1. calcul de la walkability regionale canonique
        // 2. evaluation semantique complete de la region
        // Cette decomposition permet de garder la pipeline explicite et testable.
        return regionLayoutBuilder.build(context.layoutSeed(), context.layerIndex(), walkabilityStage.sample(context));
    }
}
