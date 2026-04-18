package com.petassegang.addons.world.backrooms.level0.stage;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionWalkability;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroWalkabilitySampler;

/**
 * Etape regionale legacy produisant la traversabilite canonique d'une region.
 */
public final class LevelZeroLegacyRegionWalkabilityStage implements LevelZeroRegionStage<LevelZeroRegionWalkability> {

    private final LevelZeroWalkabilitySampler walkabilitySampler;

    /**
     * Construit l'etape regionale de traversabilite legacy.
     *
     * @param walkabilitySampler source de traversabilite des cellules
     */
    public LevelZeroLegacyRegionWalkabilityStage(LevelZeroWalkabilitySampler walkabilitySampler) {
        this.walkabilitySampler = walkabilitySampler;
    }

    @Override
    public LevelZeroRegionWalkability sample(LevelZeroRegionContext context) {
        int minCellX = context.window().minCellX();
        int minCellZ = context.window().minCellZ();
        int maxCellX = context.window().maxCellX();
        int maxCellZ = context.window().maxCellZ();
        int width = context.window().width();
        int height = context.window().height();
        boolean[] walkable = new boolean[width * height];

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                walkable[(cellZ - minCellZ) * width + (cellX - minCellX)] =
                        walkabilitySampler.sampleWalkableCell(cellX, cellZ);
            }
        }

        return new LevelZeroRegionWalkability(minCellX, minCellZ, maxCellX, maxCellZ, walkable);
    }
}
