package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellMicroPattern;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroLegacyMicroPatternStage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie que la micro-geometrie est bien derivee a l'echelle du bloc.
 */
@DisplayName("Micro-patterns du Level 0")
class BackroomsLevelZeroMicroPatternStageTest {

    @Test
    @DisplayName("Un couloir sans anomalie garde un motif pleinement ouvert")
    void testPlainCorridorKeepsFullOpenPattern() {
        LevelZeroLegacyMicroPatternStage stage = new LevelZeroLegacyMicroPatternStage();
        LevelZeroCellContext context = new LevelZeroCellContext(8, -4, 12345L);

        int pattern = stage.sample(context, LevelZeroCellTopology.CORRIDOR, LevelZeroGeometryMask.none());

        assertEquals(LevelZeroCellMicroPattern.FULL_OPEN, pattern,
                "Une cellule sans feature geometrique doit garder son motif pleinement ouvert.");
    }

    @Test
    @DisplayName("Les renfoncements et offsets sont resolves bloc par bloc")
    void testBlockScalePatternsAreNotTemplateLocked() {
        LevelZeroLegacyMicroPatternStage stage = new LevelZeroLegacyMicroPatternStage();
        LevelZeroCellContext context = new LevelZeroCellContext(11, 3, 998877L);

        int recessMask = LevelZeroGeometryMask.with(LevelZeroGeometryMask.none(), LevelZeroGeometryFeature.RECESS);
        int offsetMask = LevelZeroGeometryMask.with(LevelZeroGeometryMask.none(), LevelZeroGeometryFeature.OFFSET_WALL);
        int recessPattern = stage.sample(context, LevelZeroCellTopology.CORRIDOR, recessMask);
        int offsetPattern = stage.sample(context, LevelZeroCellTopology.CORRIDOR, offsetMask);

        assertNotEquals(LevelZeroCellMicroPattern.RECESS_NORTH, recessPattern,
                "Le renfoncement ne doit plus etre force sur un template fixe predefini.");
        assertNotEquals(LevelZeroCellMicroPattern.RECESS_EAST, recessPattern,
                "Le renfoncement doit maintenant se resoudre via des decisions bloc par bloc.");
        assertNotEquals(LevelZeroCellMicroPattern.RECESS_SOUTH, recessPattern,
                "Le renfoncement ne doit plus etre un simple motif cellule-fixe.");
        assertNotEquals(LevelZeroCellMicroPattern.RECESS_WEST, recessPattern,
                "Le renfoncement doit varier a l'echelle 1x1.");
        assertNotEquals(LevelZeroCellMicroPattern.OFFSET_EAST, offsetPattern,
                "Le decalage de mur ne doit plus etre un template unique precompose.");
        assertNotEquals(LevelZeroCellMicroPattern.OFFSET_WEST, offsetPattern,
                "Le decalage de mur doit se construire bloc par bloc.");
        assertTrue(LevelZeroCellMicroPattern.openCount(recessPattern) < LevelZeroCellMicroPattern.openCount(LevelZeroCellMicroPattern.FULL_OPEN),
                "Un renfoncement doit effectivement fermer au moins un bloc local.");
        assertTrue(LevelZeroCellMicroPattern.openCount(offsetPattern) < LevelZeroCellMicroPattern.openCount(LevelZeroCellMicroPattern.FULL_OPEN),
                "Un decalage de mur doit effectivement fermer au moins un bloc local.");
    }

    @Test
    @DisplayName("Un etranglement 1-wide garde exactement une ligne ouverte")
    void testPinchKeepsSingleBlockWidePassage() {
        LevelZeroLegacyMicroPatternStage stage = new LevelZeroLegacyMicroPatternStage();
        LevelZeroCellContext context = new LevelZeroCellContext(-2, 14, 556677L);
        int pinchMask = LevelZeroGeometryMask.with(LevelZeroGeometryMask.none(), LevelZeroGeometryFeature.PINCH_1WIDE);

        int pattern = stage.sample(context, LevelZeroCellTopology.CORRIDOR, pinchMask);

        assertEquals(3, LevelZeroCellMicroPattern.openCount(pattern),
                "Un etranglement 1-wide doit laisser exactement trois blocs ouverts.");
        assertTrue(pattern == LevelZeroCellMicroPattern.PINCH_VERTICAL || pattern == LevelZeroCellMicroPattern.PINCH_HORIZONTAL,
                "Un etranglement 1-wide doit rester un passage simple d'un bloc de large.");
    }
}
