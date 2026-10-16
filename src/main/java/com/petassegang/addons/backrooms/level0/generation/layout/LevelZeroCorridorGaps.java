package com.petassegang.addons.backrooms.level0.generation.layout;

import com.petassegang.addons.backrooms.level0.generation.noise.StageRandom;

/**
 * Micro-geometrie des couloirs : resserrement rare d'un couloir droit a une ligne d'un bloc.
 *
 * <p>Un gap n'apparait que sur un couloir droit borde de murs des deux cotes,
 * jamais sur une cellule eclairee, et deux gaps ne peuvent pas se suivre dans
 * le sens du passage (le melange de hash de StageRandom l'interdit par construction).
 */
public final class LevelZeroCorridorGaps {

    private static final int GAP_FREQUENCY = 48;

    private final boolean enabled;

    /**
     * Construit la couche de gaps.
     *
     * @param enabled {@code false} pour ne jamais produire de gap
     */
    public LevelZeroCorridorGaps(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Masque de features geometriques d'une cellule, avec arbitrage regional.
     *
     * @param context cellule evaluee
     * @param topology forme locale de la cellule
     * @param connectionMask sorties cardinales
     * @param walkability fenetre de traversabilite
     * @return masque de {@link LevelZeroGeometryFeature}
     */
    public int sample(LevelZeroCellContext context,
                      LevelZeroCellTopology topology,
                      int connectionMask,
                      LevelZeroRegionWalkability walkability) {
        // Deux cellules consecutives d'un couloir ne passent jamais toutes deux le test de frequence (le melange de
        // StageRandom l'interdit par construction) : aucun arbitrage entre voisins n'est necessaire.
        int mask = sampleLocal(context, topology, connectionMask);
        if (mask == LevelZeroGeometryFeature.NONE
                || !hasWallOnBothSides(context.cellX(), context.cellZ(), connectionMask, walkability)) {
            return LevelZeroGeometryFeature.NONE;
        }
        return mask;
    }

    /**
     * Masque de features d'une cellule sans tenir compte de ses voisins.
     *
     * @param context cellule evaluee
     * @param topology forme locale de la cellule
     * @param connectionMask sorties cardinales
     * @return masque de {@link LevelZeroGeometryFeature}
     */
    public int sampleLocal(LevelZeroCellContext context, LevelZeroCellTopology topology, int connectionMask) {
        if (!enabled
                || topology != LevelZeroCellTopology.CORRIDOR
                || !LevelZeroCellConnections.isStraight(connectionMask)) {
            return LevelZeroGeometryFeature.NONE;
        }
        long hash = gapScore(context);
        if (Math.floorMod(hash, GAP_FREQUENCY) != 0) {
            return LevelZeroGeometryFeature.NONE;
        }
        return switch (Math.floorMod(Long.rotateLeft(hash, 23), 3)) {
            case 0 -> LevelZeroGeometryFeature.GAP_LEFT.bit();
            case 1 -> LevelZeroGeometryFeature.GAP_MIDDLE.bit();
            default -> LevelZeroGeometryFeature.GAP_RIGHT.bit();
        };
    }

    /**
     * Projette un masque de features sur le motif 3x3 de la cellule.
     *
     * @param connectionMask sorties cardinales (donne l'orientation du couloir)
     * @param geometryMask masque de features
     * @return motif de {@link LevelZeroCellMicroPattern}
     */
    public static int microPattern(int connectionMask, int geometryMask) {
        if (LevelZeroGeometryFeature.GAP_LEFT.isIn(geometryMask)) {
            return gapPattern(connectionMask, 0);
        }
        if (LevelZeroGeometryFeature.GAP_MIDDLE.isIn(geometryMask)) {
            return gapPattern(connectionMask, 1);
        }
        if (LevelZeroGeometryFeature.GAP_RIGHT.isIn(geometryMask)) {
            return gapPattern(connectionMask, 2);
        }
        return LevelZeroCellMicroPattern.FULL_OPEN;
    }

    private static int gapPattern(int connectionMask, int lane) {
        if (connectionMask == LevelZeroCellConnections.NORTH_SOUTH) {
            return LevelZeroCellMicroPattern.bit(lane, 0)
                    | LevelZeroCellMicroPattern.bit(lane, 1)
                    | LevelZeroCellMicroPattern.bit(lane, 2);
        }
        if (connectionMask == LevelZeroCellConnections.EAST_WEST) {
            return LevelZeroCellMicroPattern.bit(0, lane)
                    | LevelZeroCellMicroPattern.bit(1, lane)
                    | LevelZeroCellMicroPattern.bit(2, lane);
        }
        return LevelZeroCellMicroPattern.FULL_OPEN;
    }

    /** Le masque est un couloir droit, garanti par {@link #sampleLocal} : nord-sud, sinon est-ouest. */
    private static boolean hasWallOnBothSides(int cellX, int cellZ, int connectionMask,
                                              LevelZeroRegionWalkability walkability) {
        if (connectionMask == LevelZeroCellConnections.NORTH_SOUTH) {
            return !walkability.isWalkable(cellX - 1, cellZ) && !walkability.isWalkable(cellX + 1, cellZ);
        }
        return !walkability.isWalkable(cellX, cellZ - 1) && !walkability.isWalkable(cellX, cellZ + 1);
    }

    private static long gapScore(LevelZeroCellContext context) {
        return StageRandom.mix(
                context.layoutSeed(),
                StageRandom.Stage.NOISE_GEOMETRY,
                context.cellX(),
                context.cellZ());
    }
}
