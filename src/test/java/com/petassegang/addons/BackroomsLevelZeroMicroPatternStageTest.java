package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellConnections;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellMicroPattern;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;
import com.petassegang.addons.world.backrooms.level0.stage.geometry.LevelZeroLegacyMicroPatternStage;

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

        int pattern = stage.sample(
                context,
                LevelZeroCellTopology.CORRIDOR,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH,
                LevelZeroGeometryMask.none());

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
        int recessPattern = stage.sample(
                context,
                LevelZeroCellTopology.CORRIDOR,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH,
                recessMask);
        int offsetPattern = stage.sample(
                context,
                LevelZeroCellTopology.CORRIDOR,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH,
                offsetMask);

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

        int pattern = stage.sample(
                context,
                LevelZeroCellTopology.CORRIDOR,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH,
                pinchMask);

        assertEquals(3, LevelZeroCellMicroPattern.openCount(pattern),
                "Un etranglement 1-wide doit laisser exactement trois blocs ouverts.");
        assertEquals(LevelZeroCellMicroPattern.PINCH_VERTICAL, pattern,
                "Un couloir vertical doit produire un pinch vertical aligne sur ses connexions.");
    }

    @Test
    @DisplayName("Un demi-mur de couloir s'aligne sur l'axe reel du passage")
    void testHalfWallUsesCorridorAxis() {
        LevelZeroLegacyMicroPatternStage stage = new LevelZeroLegacyMicroPatternStage();
        LevelZeroCellContext context = new LevelZeroCellContext(9, -7, 112233L);
        int halfWallMask = LevelZeroGeometryMask.with(LevelZeroGeometryMask.none(), LevelZeroGeometryFeature.HALF_WALL);

        int pattern = stage.sample(
                context,
                LevelZeroCellTopology.CORRIDOR,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH,
                halfWallMask);

        assertEquals(LevelZeroCellMicroPattern.HALF_WALL_VERTICAL, pattern,
                "Un couloir vertical doit produire un demi-mur aligne sur son axe.");
    }

    @Test
    @DisplayName("Une alcove de dead end s'oriente sur la face opposee a l'ouverture")
    void testDeadEndAlcoveUsesConnectionDirection() {
        LevelZeroLegacyMicroPatternStage stage = new LevelZeroLegacyMicroPatternStage();
        LevelZeroCellContext context = new LevelZeroCellContext(4, 6, 424242L);
        int alcoveMask = LevelZeroGeometryMask.with(LevelZeroGeometryMask.none(), LevelZeroGeometryFeature.ALCOVE);

        int pattern = stage.sample(
                context,
                LevelZeroCellTopology.DEAD_END,
                LevelZeroCellConnections.NORTH,
                alcoveMask);

        assertEquals(LevelZeroCellMicroPattern.ALCOVE_SOUTH, pattern,
                "Un dead end ouvert au nord doit creuser son alcove sur la face sud.");
    }

    @Test
    @DisplayName("Un renfoncement d'angle ferme le coin bloque sans toucher les sorties")
    void testAngleRecessUsesBlockedCorner() {
        LevelZeroLegacyMicroPatternStage stage = new LevelZeroLegacyMicroPatternStage();
        LevelZeroCellContext context = new LevelZeroCellContext(5, 9, 12345L);
        int recessMask = LevelZeroGeometryMask.with(LevelZeroGeometryMask.none(), LevelZeroGeometryFeature.RECESS);

        int pattern = stage.sample(
                context,
                LevelZeroCellTopology.ANGLE,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.EAST,
                recessMask);

        assertEquals(false, LevelZeroCellMicroPattern.isOpen(pattern, 0, 2),
                "Un angle nord-est doit fermer son coin bloque sud-ouest.");
        assertEquals(true, LevelZeroCellMicroPattern.isOpen(pattern, 1, 0),
                "La sortie nord doit rester ouverte.");
        assertEquals(true, LevelZeroCellMicroPattern.isOpen(pattern, 2, 1),
                "La sortie est doit rester ouverte.");
    }

    @Test
    @DisplayName("Un decalage d'angle se projette sur les murs bloques, pas sur les sorties")
    void testAngleOffsetAvoidsOpenSides() {
        LevelZeroLegacyMicroPatternStage stage = new LevelZeroLegacyMicroPatternStage();
        LevelZeroCellContext context = new LevelZeroCellContext(7, 11, 777L);
        int offsetMask = LevelZeroGeometryMask.with(LevelZeroGeometryMask.none(), LevelZeroGeometryFeature.OFFSET_WALL);

        int pattern = stage.sample(
                context,
                LevelZeroCellTopology.ANGLE,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.EAST,
                offsetMask);

        assertEquals(true, LevelZeroCellMicroPattern.isOpen(pattern, 1, 0),
                "Un decalage d'angle ne doit pas mordre sur la sortie nord.");
        assertEquals(true, LevelZeroCellMicroPattern.isOpen(pattern, 2, 1),
                "Un decalage d'angle ne doit pas mordre sur la sortie est.");
        assertEquals(false,
                LevelZeroCellMicroPattern.isOpen(pattern, 0, 1)
                        && LevelZeroCellMicroPattern.isOpen(pattern, 1, 2)
                        && LevelZeroCellMicroPattern.isOpen(pattern, 0, 2),
                "Le decalage doit fermer au moins un bloc sur les faces bloquees de l'angle.");
    }
}
