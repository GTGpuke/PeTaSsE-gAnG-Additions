package com.petassegang.addons.world.backrooms.level0.stage.geometry;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellConnections;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellMicroPattern;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;

/**
 * Etape legacy de projection bloc-par-bloc des anomalies geometriques.
 *
 * <p>La projection transforme les features logiques en masque 3x3. Le gap de
 * couloir ferme toute la cellule sauf une ligne droite de 1 bloc de large sur
 * 3 blocs de long, orientee selon les connexions du couloir.
 */
public final class LevelZeroLegacyMicroPatternStage {

    /**
     * Projette le masque geometrique sur un motif local 3x3.
     *
     * @param context contexte canonique de cellule
     * @param topology topologie semantique fine
     * @param connectionMask masque des connexions cardinales
     * @param geometryMask masque de features geometriques
     * @return motif 3x3 bloc-par-bloc
     */
    public int sample(LevelZeroCellContext context, LevelZeroCellTopology topology, int connectionMask, int geometryMask) {
        if (topology == LevelZeroCellTopology.WALL) {
            return LevelZeroCellMicroPattern.FULL_CLOSED;
        }
        if (LevelZeroGeometryMask.has(geometryMask, LevelZeroGeometryFeature.GAP_LEFT)) {
            return gapPattern(connectionMask, 0);
        }
        if (LevelZeroGeometryMask.has(geometryMask, LevelZeroGeometryFeature.GAP_MIDDLE)) {
            return gapPattern(connectionMask, 1);
        }
        if (LevelZeroGeometryMask.has(geometryMask, LevelZeroGeometryFeature.GAP_RIGHT)) {
            return gapPattern(connectionMask, 2);
        }
        return LevelZeroCellMicroPattern.FULL_OPEN;
    }

    private int gapPattern(int connectionMask, int laneIndex) {
        if (connectionMask == (LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH)) {
            return LevelZeroCellMicroPattern.bit(laneIndex, 0)
                    | LevelZeroCellMicroPattern.bit(laneIndex, 1)
                    | LevelZeroCellMicroPattern.bit(laneIndex, 2);
        }
        if (connectionMask == (LevelZeroCellConnections.EAST | LevelZeroCellConnections.WEST)) {
            return LevelZeroCellMicroPattern.bit(0, laneIndex)
                    | LevelZeroCellMicroPattern.bit(1, laneIndex)
                    | LevelZeroCellMicroPattern.bit(2, laneIndex);
        }
        return LevelZeroCellMicroPattern.FULL_OPEN;
    }
}
