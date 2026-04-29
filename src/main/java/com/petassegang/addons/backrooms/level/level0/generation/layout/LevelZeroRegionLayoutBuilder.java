package com.petassegang.addons.backrooms.level.level0.generation.layout;

import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroCellContext;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroCellEvaluation;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroLegacyLayoutPipeline;

/**
 * Constructeur de {@code LevelZeroRegionLayout} a partir de la walkability
 * regionale.
 *
 * <p>Son travail est volontairement simple : iterer sur la fenetre regionale
 * et demander a la pipeline legacy d'evaluer chaque cellule, sans poser de
 * blocs et sans extraire encore le chunk final.
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

    public LevelZeroRegionLayout build(long layoutSeed,
                                       LevelZeroRegionWalkability regionWalkability) {
        return build(layoutSeed, 0, regionWalkability);
    }

    /**
     * Construit une region canonique a partir d'une fenetre de cellules.
     *
     * @param layoutSeed seed de layout
     * @param regionWalkability carte canonique de traversabilite regionale
     * @return region layout canonique
     */
    public LevelZeroRegionLayout build(long layoutSeed,
                                       int layerIndex,
                                       LevelZeroRegionWalkability regionWalkability) {
        int minCellX = regionWalkability.minCellX();
        int minCellZ = regionWalkability.minCellZ();
        int maxCellX = regionWalkability.maxCellX();
        int maxCellZ = regionWalkability.maxCellZ();
        int width = maxCellX - minCellX + 1;
        int height = maxCellZ - minCellZ + 1;
        LevelZeroCellEvaluation[] evaluations = new LevelZeroCellEvaluation[width * height];

        // La region est evaluee cellule par cellule pour garder un etat logique
        // complet avant l'extraction du chunk : on ne pose rien ici, on
        // accumule seulement des evaluations semantiques stables.
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                LevelZeroCellContext cellContext = new LevelZeroCellContext(cellX, cellZ, layoutSeed, layerIndex);
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
