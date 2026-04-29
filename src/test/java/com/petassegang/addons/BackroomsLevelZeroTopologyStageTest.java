package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellConnections;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellTopology;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroRegionWalkability;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorRoomKind;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroCellContext;
import com.petassegang.addons.backrooms.level.level0.generation.stage.topology.LevelZeroLegacyTopologyStage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifie la classification topologique fine des cellules du Level 0.
 */
@DisplayName("Topology stage du Level 0")
class BackroomsLevelZeroTopologyStageTest {

    @Test
    @DisplayName("Un couloir droit reste un corridor")
    void testStraightCorridorClassification() {
        LevelZeroLegacyTopologyStage stage = new LevelZeroLegacyTopologyStage();
        LevelZeroRegionWalkability walkability = walkability((x, z) -> x == 5 && z >= 8 && z <= 10);

        assertEquals(LevelZeroCellTopology.CORRIDOR,
                stage.sample(new LevelZeroCellContext(5, 9, 12345L), walkability, false));
        assertEquals(LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH,
                stage.sampleConnectionMask(new LevelZeroCellContext(5, 9, 12345L), walkability));
    }

    @Test
    @DisplayName("Un angle est distingue d'une vraie jonction")
    void testAngleClassification() {
        LevelZeroLegacyTopologyStage stage = new LevelZeroLegacyTopologyStage();
        LevelZeroRegionWalkability walkability = walkability((x, z) -> (x == 5 && z == 9) || (x == 5 && z == 8) || (x == 6 && z == 9));

        assertEquals(LevelZeroCellTopology.ANGLE,
                stage.sample(new LevelZeroCellContext(5, 9, 12345L), walkability, false));
    }

    @Test
    @DisplayName("Une jonction en T est distinguee d'un carrefour")
    void testTJunctionClassification() {
        LevelZeroLegacyTopologyStage stage = new LevelZeroLegacyTopologyStage();
        LevelZeroRegionWalkability walkability = walkability((x, z) -> (x == 5 && z == 9)
                || (x == 5 && z == 8)
                || (x == 5 && z == 10)
                || (x == 6 && z == 9));

        assertEquals(LevelZeroCellTopology.T_JUNCTION,
                stage.sample(new LevelZeroCellContext(5, 9, 12345L), walkability, false));
    }

    @Test
    @DisplayName("Un carrefour a quatre voies reste un crossroad")
    void testCrossroadClassification() {
        LevelZeroLegacyTopologyStage stage = new LevelZeroLegacyTopologyStage();
        LevelZeroRegionWalkability walkability = walkability((x, z) -> (x == 5 && z == 9)
                || (x == 5 && z == 8)
                || (x == 5 && z == 10)
                || (x == 4 && z == 9)
                || (x == 6 && z == 9));

        assertEquals(LevelZeroCellTopology.CROSSROAD,
                stage.sample(new LevelZeroCellContext(5, 9, 12345L), walkability, false));
    }

    private static LevelZeroRegionWalkability walkability(CellPredicate predicate) {
        int min = 0;
        int max = 12;
        boolean[] cells = new boolean[(max - min + 1) * (max - min + 1)];
        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                cells[(z - min) * (max - min + 1) + (x - min)] = predicate.test(x, z);
            }
        }
        LevelZeroSectorRoomKind[] roomKinds = new LevelZeroSectorRoomKind[cells.length];
        java.util.Arrays.fill(roomKinds, LevelZeroSectorRoomKind.NONE);
        return new LevelZeroRegionWalkability(min, min, max, max, cells, roomKinds);
    }

    @FunctionalInterface
    private interface CellPredicate {
        boolean test(int cellX, int cellZ);
    }
}
