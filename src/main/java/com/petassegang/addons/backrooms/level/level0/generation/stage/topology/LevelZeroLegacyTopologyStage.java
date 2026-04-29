package com.petassegang.addons.backrooms.level.level0.generation.stage.topology;

import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellConnections;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellTopology;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroRegionWalkability;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroCellContext;

/**
 * Etape de derivation de la topologie semantique fine d'une cellule.
 *
 * <p>Le tag principal dit seulement si l'on parle d'un mur, d'un couloir ou
 * d'une grande piece. Cette classe affine ensuite cette information en formes
 * lisibles par le reste de la pipeline : angle, T, carrefour, cul-de-sac,
 * grande room, etc.
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

        int connectionMask = sampleConnectionMask(context, regionWalkability);
        int degree = LevelZeroCellConnections.count(connectionMask);

        if (degree <= 1) {
            return LevelZeroCellTopology.DEAD_END;
        }
        if (degree == 2 && (LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.NORTH)
                && LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.SOUTH)
                || LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.EAST)
                && LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.WEST))) {
            return LevelZeroCellTopology.CORRIDOR;
        }
        if (degree == 2) {
            return LevelZeroCellTopology.ANGLE;
        }
        if (degree == 3) {
            return LevelZeroCellTopology.T_JUNCTION;
        }
        return LevelZeroCellTopology.CROSSROAD;
    }

    /**
     * Derive le masque de connexions cardinales d'une cellule.
     *
     * @param context contexte canonique de cellule
     * @param regionWalkability carte regionale de walkability
     * @return masque cardinal N/E/S/W
     */
    public int sampleConnectionMask(LevelZeroCellContext context,
                                    LevelZeroRegionWalkability regionWalkability) {
        int mask = LevelZeroCellConnections.none();
        boolean north = isWalkable(regionWalkability, context.cellX(), context.cellZ() - 1);
        boolean east = isWalkable(regionWalkability, context.cellX() + 1, context.cellZ());
        boolean south = isWalkable(regionWalkability, context.cellX(), context.cellZ() + 1);
        boolean west = isWalkable(regionWalkability, context.cellX() - 1, context.cellZ());
        if (north) {
            mask = LevelZeroCellConnections.with(mask, LevelZeroCellConnections.NORTH);
        }
        if (east) {
            mask = LevelZeroCellConnections.with(mask, LevelZeroCellConnections.EAST);
        }
        if (south) {
            mask = LevelZeroCellConnections.with(mask, LevelZeroCellConnections.SOUTH);
        }
        if (west) {
            mask = LevelZeroCellConnections.with(mask, LevelZeroCellConnections.WEST);
        }
        return mask;
    }

    private static boolean isWalkable(LevelZeroRegionWalkability regionWalkability, int cellX, int cellZ) {
        if (cellX < regionWalkability.minCellX() || cellX > regionWalkability.maxCellX()
                || cellZ < regionWalkability.minCellZ() || cellZ > regionWalkability.maxCellZ()) {
            return false;
        }
        return regionWalkability.sampleWalkableCell(cellX, cellZ);
    }
}
