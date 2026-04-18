package com.petassegang.addons.world.backrooms.level0.stage;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionWalkability;

/**
 * Etape legacy de derivation de la topologie semantique d'une cellule.
 */
public final class LevelZeroLegacyTopologyStage {

    /**
     * Derive la topologie d'une cellule a partir de la walkability regionale.
     *
     * @param context contexte canonique de cellule
     * @param regionWalkability carte regionale de walkability
     * @param largeRoom marquage historique de grande piece
     * @return topologie semantique derivee
     */
    public LevelZeroCellTopology sample(LevelZeroCellContext context,
                                        LevelZeroRegionWalkability regionWalkability,
                                        boolean largeRoom) {
        if (!regionWalkability.sampleWalkableCell(context.cellX(), context.cellZ())) {
            return LevelZeroCellTopology.WALL;
        }
        if (largeRoom) {
            return LevelZeroCellTopology.ROOM_LARGE;
        }

        boolean north = isWalkable(regionWalkability, context.cellX(), context.cellZ() - 1);
        boolean east = isWalkable(regionWalkability, context.cellX() + 1, context.cellZ());
        boolean south = isWalkable(regionWalkability, context.cellX(), context.cellZ() + 1);
        boolean west = isWalkable(regionWalkability, context.cellX() - 1, context.cellZ());
        int degree = count(north) + count(east) + count(south) + count(west);

        if (degree <= 1) {
            return LevelZeroCellTopology.DEAD_END;
        }
        if (degree == 2 && ((north && south) || (east && west))) {
            return LevelZeroCellTopology.CORRIDOR;
        }
        return LevelZeroCellTopology.JUNCTION;
    }

    private static boolean isWalkable(LevelZeroRegionWalkability regionWalkability, int cellX, int cellZ) {
        if (cellX < regionWalkability.minCellX() || cellX > regionWalkability.maxCellX()
                || cellZ < regionWalkability.minCellZ() || cellZ > regionWalkability.maxCellZ()) {
            return false;
        }
        return regionWalkability.sampleWalkableCell(cellX, cellZ);
    }

    private static int count(boolean value) {
        return value ? 1 : 0;
    }
}
