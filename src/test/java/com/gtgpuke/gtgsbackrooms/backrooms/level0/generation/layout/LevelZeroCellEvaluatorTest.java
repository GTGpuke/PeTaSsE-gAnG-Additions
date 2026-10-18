package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gtgpuke.gtgsbackrooms.backrooms.level0.biome.LevelZeroSurfaceBiome;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.sector.LevelZeroSectorRoomKind;

@DisplayName("Evaluateur de cellules du Level 0")
class LevelZeroCellEvaluatorTest {

    @Test
    @DisplayName("Un couloir droit isole est evalue comme CORRIDOR nord-sud pleinement ouvert")
    void straightCorridor() {
        LevelZeroCellEvaluator evaluator = new LevelZeroCellEvaluator(true);
        LevelZeroRegionWalkability region = LevelZeroRegionWalkability.of(3, 6, 7, 12, (x, z) -> x == 5 && z >= 7 && z <= 11 ? 1 : 0);
        LevelZeroCellContext context = findNonLargeRoom(5, 7, 11, 24680L);
        LevelZeroCellEvaluation evaluation = evaluator.evaluate(context, region);

        assertEquals(context, evaluation.context(), "Le contexte est conserve.");
        assertTrue(evaluation.walkable(), "La cellule est ouverte.");
        assertEquals(LevelZeroCellTopology.CORRIDOR, evaluation.topology(), "Couloir droit.");
        assertEquals(LevelZeroCellConnections.NORTH_SOUTH, evaluation.connectionMask(), "Sorties nord et sud.");
        assertEquals(LevelZeroSectorRoomKind.NONE, evaluation.roomKind(), "Pas de salle.");
        assertEquals(LevelZeroSurfaceBiome.sampleAtCell(context.cellX(), context.cellZ(), 0), evaluation.surfaceBiome(), "Biome.");
        assertFalse(evaluation.largeRoom(), "Pas une grande piece.");
        assertTrue(evaluation.isLocallyWalkable(1, 1) || evaluation.hasGeometryFeature(),
                "Le centre est ouvert sauf si un gap lateral ferme la colonne centrale.");
    }

    @Test
    @DisplayName("Un mur est ferme, sans connexion ni lumiere")
    void wallCell() {
        LevelZeroCellEvaluator evaluator = new LevelZeroCellEvaluator(true);
        LevelZeroRegionWalkability region = LevelZeroRegionWalkability.of(0, 0, 4, 4, (x, z) -> 0);
        LevelZeroCellEvaluation evaluation = evaluator.evaluate(new LevelZeroCellContext(2, 2, 1L, 0), region);

        assertFalse(evaluation.walkable(), "Ferme.");
        assertEquals(LevelZeroCellTopology.WALL, evaluation.topology(), "Mur.");
        assertEquals(LevelZeroCellConnections.NONE, evaluation.connectionMask(), "Aucune connexion.");
        assertEquals(LevelZeroCellMicroPattern.FULL_CLOSED, evaluation.microPattern(), "Motif ferme.");
        assertFalse(evaluation.lighted(), "Aucun neon dans un mur.");
        assertFalse(evaluation.isLocallyWalkable(1, 1), "Aucun bloc traversable.");
    }

    @Test
    @DisplayName("Une cellule eclairee ne recoit jamais de micro-geometrie")
    void lightedCellsHaveNoGaps() {
        LevelZeroCellEvaluator evaluator = new LevelZeroCellEvaluator(true);
        LevelZeroRegionWalkability region = LevelZeroRegionWalkability.of(16, -8, 20, 8, (x, z) -> x == 18 ? 1 : 0);
        boolean foundLighted = false;
        for (int z = -6; z <= 6; z++) {
            LevelZeroCellEvaluation evaluation = evaluator.evaluate(new LevelZeroCellContext(18, z, 778899L, 0), region);
            if (!evaluation.lighted()) {
                continue;
            }
            foundLighted = true;
            assertEquals(LevelZeroGeometryFeature.NONE, evaluation.geometryMask(), "Pas de gap sous un neon.");
            assertEquals(LevelZeroCellMicroPattern.FULL_OPEN, evaluation.microPattern(), "Motif ouvert sous un neon.");
        }
        assertTrue(foundLighted, "Le couloir de test doit contenir au moins un neon.");
    }

    @Test
    @DisplayName("Desactiver la micro-geometrie rend toute cellule ouverte pleinement traversable")
    void disablingGeometryOpensEverything() {
        LevelZeroCellEvaluator disabled = new LevelZeroCellEvaluator(false);
        LevelZeroRegionWalkability region = LevelZeroRegionWalkability.of(-2, -512, 2, 512, (x, z) -> x == 0 ? 1 : 0);
        for (int z = -500; z <= 500; z++) {
            LevelZeroCellEvaluation evaluation = disabled.evaluate(new LevelZeroCellContext(0, z, 778899L, 0), region);
            assertEquals(LevelZeroGeometryFeature.NONE, evaluation.geometryMask(), "Aucune feature sans micro-geometrie.");
            assertEquals(LevelZeroCellMicroPattern.FULL_OPEN, evaluation.microPattern(), "Toujours ouvert.");
        }
    }

    @Test
    @DisplayName("Une grande piece ouverte est classee ROOM_LARGE sans masque de connexions")
    void largeRoomCell() {
        LevelZeroCellEvaluator evaluator = new LevelZeroCellEvaluator(true);
        LevelZeroRegionWalkability region = LevelZeroRegionWalkability.of(-70, -70, 70, 70, (x, z) -> 1);
        for (int x = -64; x <= 64; x++) {
            for (int z = -64; z <= 64; z++) {
                LevelZeroCellContext context = new LevelZeroCellContext(x, z, 12345L, 0);
                if (!LevelZeroLargeRooms.isLargeRoom(context)) {
                    continue;
                }
                LevelZeroCellEvaluation evaluation = evaluator.evaluate(context, region);
                assertEquals(LevelZeroCellTopology.ROOM_LARGE, evaluation.topology(), "Grande piece.");
                assertEquals(LevelZeroCellConnections.NONE, evaluation.connectionMask(), "Pas de masque pour une grande piece.");
                assertTrue(evaluation.largeRoom(), "Marquee grande piece.");
                return;
            }
        }
        throw new AssertionError("Aucune grande piece dans l'echantillon.");
    }

    private static LevelZeroCellContext findNonLargeRoom(int cellX, int minZ, int maxZ, long seed) {
        for (int z = minZ + 1; z < maxZ; z++) {
            LevelZeroCellContext context = new LevelZeroCellContext(cellX, z, seed, 0);
            if (!LevelZeroLargeRooms.isLargeRoom(context)) {
                return context;
            }
        }
        throw new AssertionError("Toutes les cellules du couloir sont des grandes pieces.");
    }
}
