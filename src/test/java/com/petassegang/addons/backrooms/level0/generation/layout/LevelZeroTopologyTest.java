package com.petassegang.addons.backrooms.level0.generation.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Topologie des cellules du Level 0")
class LevelZeroTopologyTest {

    private static LevelZeroRegionWalkability cross(boolean north, boolean east, boolean south, boolean west) {
        return LevelZeroRegionWalkability.of(4, 8, 6, 10, (x, z) ->
                (x == 5 && z == 9)
                        || (north && x == 5 && z == 8)
                        || (east && x == 6 && z == 9)
                        || (south && x == 5 && z == 10)
                        || (west && x == 4 && z == 9) ? 1 : 0);
    }

    @Test
    @DisplayName("Le masque de connexions reflete les quatre voisins")
    void connectionMask() {
        assertEquals(LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH,
                LevelZeroTopology.connectionMask(5, 9, cross(true, false, true, false)), "Nord et sud ouverts.");
        assertEquals(LevelZeroCellConnections.EAST | LevelZeroCellConnections.WEST,
                LevelZeroTopology.connectionMask(5, 9, cross(false, true, false, true)), "Est et ouest ouverts.");
        assertEquals(LevelZeroCellConnections.NONE,
                LevelZeroTopology.connectionMask(5, 9, cross(false, false, false, false)), "Aucun voisin.");
    }

    @Test
    @DisplayName("La classification distingue couloir, angle, T, carrefour et cul-de-sac")
    void classify() {
        assertEquals(LevelZeroCellTopology.WALL, LevelZeroTopology.classify(false, false, 0), "Mur.");
        assertEquals(LevelZeroCellTopology.ROOM_LARGE, LevelZeroTopology.classify(true, true, 0), "Grande piece.");
        assertEquals(LevelZeroCellTopology.DEAD_END, LevelZeroTopology.classify(true, false, LevelZeroCellConnections.NORTH), "Une sortie.");
        assertEquals(LevelZeroCellTopology.DEAD_END, LevelZeroTopology.classify(true, false, 0), "Aucune sortie.");
        assertEquals(LevelZeroCellTopology.CORRIDOR, LevelZeroTopology.classify(true, false, LevelZeroCellConnections.NORTH_SOUTH), "Couloir droit.");
        assertEquals(LevelZeroCellTopology.ANGLE, LevelZeroTopology.classify(true, false,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.EAST), "Angle.");
        assertEquals(LevelZeroCellTopology.T_JUNCTION, LevelZeroTopology.classify(true, false,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.EAST | LevelZeroCellConnections.SOUTH), "T.");
        assertEquals(LevelZeroCellTopology.CROSSROAD, LevelZeroTopology.classify(true, false,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.EAST
                        | LevelZeroCellConnections.SOUTH | LevelZeroCellConnections.WEST), "Carrefour.");
    }

    @Test
    @DisplayName("Les helpers de connexions comptent et detectent les couloirs droits")
    void connectionHelpers() {
        assertEquals(2, LevelZeroCellConnections.count(LevelZeroCellConnections.EAST_WEST), "Deux sorties.");
        assertEquals(true, LevelZeroCellConnections.isStraight(LevelZeroCellConnections.NORTH_SOUTH), "Nord-sud est droit.");
        assertEquals(false, LevelZeroCellConnections.isStraight(LevelZeroCellConnections.NORTH | LevelZeroCellConnections.EAST),
                "Un angle n'est pas droit.");
        assertEquals(true, LevelZeroCellConnections.has(LevelZeroCellConnections.EAST_WEST, LevelZeroCellConnections.WEST),
                "Ouest present.");
    }
}
