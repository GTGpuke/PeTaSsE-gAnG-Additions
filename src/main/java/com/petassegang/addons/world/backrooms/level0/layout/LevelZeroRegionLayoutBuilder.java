package com.petassegang.addons.world.backrooms.level0.layout;

import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellEvaluation;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroLegacyLayoutPipeline;

/**
 * Builder canonique de region layout pour le Level 0.
 */
public final class LevelZeroRegionLayoutBuilder {

    private final LevelZeroLegacyLayoutPipeline layoutPipeline;

    /**
     * Construit un builder de region base sur la pipeline legacy.
     *
     * @param layoutPipeline pipeline explicite du layout legacy
     */
    public LevelZeroRegionLayoutBuilder(LevelZeroLegacyLayoutPipeline layoutPipeline) {
        this.layoutPipeline = layoutPipeline;
    }

    /**
     * Construit une region canonique a partir d'une fenetre de cellules.
     *
     * @param layoutSeed seed de layout
     * @param regionWalkability carte canonique de traversabilite regionale
     * @return region layout canonique
     */
    public LevelZeroRegionLayout build(long layoutSeed,
                                       LevelZeroRegionWalkability regionWalkability) {
        int minCellX = regionWalkability.minCellX();
        int minCellZ = regionWalkability.minCellZ();
        int maxCellX = regionWalkability.maxCellX();
        int maxCellZ = regionWalkability.maxCellZ();
        int width = maxCellX - minCellX + 1;
        int height = maxCellZ - minCellZ + 1;
        LevelZeroCellEvaluation[] evaluations = new LevelZeroCellEvaluation[width * height];

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                LevelZeroCellContext cellContext = new LevelZeroCellContext(cellX, cellZ, layoutSeed);
                evaluations[(cellZ - minCellZ) * width + (cellX - minCellX)] =
                        layoutPipeline.evaluateCell(cellContext, regionWalkability);
            }
        }

        return new LevelZeroRegionLayout(
                minCellX,
                minCellZ,
                maxCellX,
                maxCellZ,
                evaluations);
    }
}
