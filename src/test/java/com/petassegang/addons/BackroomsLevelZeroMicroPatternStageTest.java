package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellConnections;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellMicroPattern;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;
import com.petassegang.addons.world.backrooms.level0.stage.geometry.LevelZeroLegacyMicroPatternStage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifie que la projection de micro-geometrie reste neutre sans variante.
 */
@DisplayName("Micro-patterns du Level 0")
class BackroomsLevelZeroMicroPatternStageTest {

    @Test
    @DisplayName("Un mur garde un motif ferme")
    void testWallKeepsClosedPattern() {
        LevelZeroLegacyMicroPatternStage stage = new LevelZeroLegacyMicroPatternStage();

        int pattern = stage.sample(
                new LevelZeroCellContext(8, -4, 12345L),
                LevelZeroCellTopology.WALL,
                LevelZeroCellConnections.none(),
                LevelZeroGeometryMask.none());

        assertEquals(LevelZeroCellMicroPattern.FULL_CLOSED, pattern,
                "Un mur doit rester ferme dans la projection 3x3.");
    }

    @Test
    @DisplayName("Une cellule traversable garde un motif pleinement ouvert")
    void testWalkableCellKeepsFullOpenPattern() {
        LevelZeroLegacyMicroPatternStage stage = new LevelZeroLegacyMicroPatternStage();

        int pattern = stage.sample(
                new LevelZeroCellContext(8, -4, 12345L),
                LevelZeroCellTopology.CORRIDOR,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH,
                LevelZeroGeometryMask.none());

        assertEquals(LevelZeroCellMicroPattern.FULL_OPEN, pattern,
                "Sans variante geometrique, une cellule traversable doit rester pleinement ouverte.");
    }
}
