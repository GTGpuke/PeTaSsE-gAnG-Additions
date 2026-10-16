package com.petassegang.addons.backrooms.level0.generation.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Masques de sorties des cellules du Level 0")
class LevelZeroCellConnectionsTest {

    @Test
    @DisplayName("Un masque dit quelles sorties il contient et combien")
    void containsAndCounts() {
        int mask = LevelZeroCellConnections.NORTH | LevelZeroCellConnections.EAST;
        assertTrue(LevelZeroCellConnections.has(mask, LevelZeroCellConnections.NORTH), "Sortie nord presente.");
        assertTrue(LevelZeroCellConnections.has(mask, LevelZeroCellConnections.EAST), "Sortie est presente.");
        assertFalse(LevelZeroCellConnections.has(mask, LevelZeroCellConnections.SOUTH), "Pas de sortie sud.");
        assertFalse(LevelZeroCellConnections.has(0, LevelZeroCellConnections.WEST), "Un masque vide n'a aucune sortie.");
        assertEquals(2, LevelZeroCellConnections.count(mask), "Deux sorties.");
        assertEquals(0, LevelZeroCellConnections.count(0), "Aucune sortie.");
    }

    @Test
    @DisplayName("Seuls les couloirs droits sont droits")
    void straightCorridors() {
        assertTrue(LevelZeroCellConnections.isStraight(LevelZeroCellConnections.NORTH_SOUTH), "Nord-sud.");
        assertTrue(LevelZeroCellConnections.isStraight(LevelZeroCellConnections.EAST_WEST), "Est-ouest.");
        assertFalse(LevelZeroCellConnections.isStraight(LevelZeroCellConnections.NORTH | LevelZeroCellConnections.EAST), "Angle.");
        assertFalse(LevelZeroCellConnections.isStraight(LevelZeroCellConnections.NORTH), "Cul-de-sac.");
        assertFalse(LevelZeroCellConnections.isStraight(0), "Mur.");
    }
}
