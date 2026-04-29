package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellConnections;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellMicroPattern;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellTopology;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroGeometryMask;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroCellContext;
import com.petassegang.addons.backrooms.level.level0.generation.stage.geometry.LevelZeroLegacyMicroPatternStage;

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

    @Test
    @DisplayName("Un gap vertical garde uniquement une colonne ouverte")
    void testVerticalGapKeepsSingleOpenColumn() {
        LevelZeroLegacyMicroPatternStage stage = new LevelZeroLegacyMicroPatternStage();
        int mask = LevelZeroGeometryMask.with(LevelZeroGeometryMask.none(), LevelZeroGeometryFeature.GAP_MIDDLE);

        int pattern = stage.sample(
                new LevelZeroCellContext(8, -4, 12345L),
                LevelZeroCellTopology.CORRIDOR,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH,
                mask);

        int expected = LevelZeroCellMicroPattern.bit(1, 0)
                | LevelZeroCellMicroPattern.bit(1, 1)
                | LevelZeroCellMicroPattern.bit(1, 2);
        assertEquals(expected, pattern,
                "Un gap vertical doit ouvrir une seule colonne de 1x3.");
    }

    @Test
    @DisplayName("Un gap horizontal garde uniquement une ligne ouverte")
    void testHorizontalGapKeepsSingleOpenRow() {
        LevelZeroLegacyMicroPatternStage stage = new LevelZeroLegacyMicroPatternStage();
        int mask = LevelZeroGeometryMask.with(LevelZeroGeometryMask.none(), LevelZeroGeometryFeature.GAP_RIGHT);

        int pattern = stage.sample(
                new LevelZeroCellContext(8, -4, 12345L),
                LevelZeroCellTopology.CORRIDOR,
                LevelZeroCellConnections.EAST | LevelZeroCellConnections.WEST,
                mask);

        int expected = LevelZeroCellMicroPattern.bit(0, 2)
                | LevelZeroCellMicroPattern.bit(1, 2)
                | LevelZeroCellMicroPattern.bit(2, 2);
        assertEquals(expected, pattern,
                "Un gap horizontal doit ouvrir une seule ligne de 1x3.");
    }
}
