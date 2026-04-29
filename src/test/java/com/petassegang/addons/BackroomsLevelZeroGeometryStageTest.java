package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellConnections;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellTopology;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroGeometryMask;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroRegionWalkability;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorRoomKind;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroCellContext;
import com.petassegang.addons.backrooms.level.level0.generation.stage.geometry.LevelZeroLegacyGeometryStage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie que la couche de geometry noise reste limitee aux variantes validees.
 */
@DisplayName("Geometry stage du Level 0")
class BackroomsLevelZeroGeometryStageTest {

    @Test
    @DisplayName("Les gaps peuvent apparaitre sur des couloirs droits")
    void testGeometryStageCanEmitGapOnStraightCorridor() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(true);
        boolean foundGap = false;

        for (int cellX = -64; cellX <= 64 && !foundGap; cellX++) {
            for (int cellZ = -64; cellZ <= 64 && !foundGap; cellZ++) {
                int mask = stage.sample(
                        new LevelZeroCellContext(cellX, cellZ, 998877L),
                        LevelZeroCellTopology.CORRIDOR,
                        LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH);

                foundGap = LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.GAP_LEFT)
                        || LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.GAP_MIDDLE)
                        || LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.GAP_RIGHT);
            }
        }

        assertTrue(foundGap,
                "Un echantillon de couloirs droits doit pouvoir produire au moins un gap.");
    }

    @Test
    @DisplayName("Les gaps restent interdits hors couloir droit")
    void testGeometryStageKeepsNonStraightCorridorsEmpty() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(true);
        LevelZeroCellContext context = new LevelZeroCellContext(12, -8, 998877L);

        assertEquals(LevelZeroGeometryMask.none(),
                stage.sample(context, LevelZeroCellTopology.ROOM_LARGE, LevelZeroCellConnections.none()),
                "Une grande piece ne doit jamais recevoir de gap.");
        assertEquals(LevelZeroGeometryMask.none(),
                stage.sample(context, LevelZeroCellTopology.ANGLE, LevelZeroCellConnections.NORTH | LevelZeroCellConnections.EAST),
                "Un angle ne doit jamais recevoir de gap.");
        assertEquals(LevelZeroGeometryMask.none(),
                stage.sample(context, LevelZeroCellTopology.T_JUNCTION,
                        LevelZeroCellConnections.NORTH | LevelZeroCellConnections.EAST | LevelZeroCellConnections.SOUTH),
                "Une jonction en T ne doit jamais recevoir de gap.");
        assertEquals(LevelZeroGeometryMask.none(),
                stage.sample(context, LevelZeroCellTopology.DEAD_END, LevelZeroCellConnections.NORTH),
                "Un dead-end ne doit jamais recevoir de gap.");
    }

    @Test
    @DisplayName("Les gaps regionaux exigent deux murs lateraux")
    void testRegionalGapRequiresSideWalls() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(true);
        LevelZeroCellContext context = findGapCandidate(stage);
        LevelZeroRegionWalkability openSides = region(
                context.cellX() - 1,
                context.cellZ() - 1,
                context.cellX() + 1,
                context.cellZ() + 1,
                (x, z) -> x == context.cellX() || z == context.cellZ());

        int mask = stage.sample(
                context,
                LevelZeroCellTopology.CORRIDOR,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH,
                openSides);

        assertEquals(LevelZeroGeometryMask.none(), mask,
                "Un gap vertical doit etre refuse si ses cotes gauche et droit ne sont pas des murs.");
    }

    @Test
    @DisplayName("Deux gaps ne peuvent pas etre colles dans le sens du passage")
    void testRegionalGapsCannotTouch() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(true);
        LevelZeroRegionWalkability corridor = region(
                -2,
                -512,
                2,
                512,
                (x, z) -> x == 0);
        boolean foundRegionalGap = false;
        int previousMask = LevelZeroGeometryMask.none();

        for (int cellZ = -511; cellZ <= 511; cellZ++) {
            LevelZeroCellContext context = new LevelZeroCellContext(0, cellZ, 998877L);
            int currentMask = stage.sample(context, LevelZeroCellTopology.CORRIDOR,
                    LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH, corridor);
            if (currentMask != LevelZeroGeometryMask.none()) {
                foundRegionalGap = true;
            }
            assertTrue(previousMask == LevelZeroGeometryMask.none()
                            || currentMask == LevelZeroGeometryMask.none(),
                    "Deux gaps regionaux ne doivent jamais etre colles dans le sens du passage.");
            previousMask = currentMask;
        }

        assertTrue(foundRegionalGap,
                "L'echantillon de test doit conserver au moins un gap valide apres arbitrage.");
    }

    @Test
    @DisplayName("La desactivation explicite garde aussi un masque vide")
    void testDisabledGeometryStageReturnsEmptyMask() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(false);
        LevelZeroCellContext context = new LevelZeroCellContext(-4, 11, 24680L);

        int mask = stage.sample(context, LevelZeroCellTopology.CORRIDOR);

        assertEquals(LevelZeroGeometryMask.none(), mask,
                "Sans noise geometry, le masque doit etre vide.");
    }

    private LevelZeroCellContext findGapCandidate(LevelZeroLegacyGeometryStage stage) {
        for (int cellX = -128; cellX <= 128; cellX++) {
            for (int cellZ = -128; cellZ <= 128; cellZ++) {
                LevelZeroCellContext context = new LevelZeroCellContext(cellX, cellZ, 998877L);
                int mask = stage.sample(context, LevelZeroCellTopology.CORRIDOR,
                        LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH);
                if (mask != LevelZeroGeometryMask.none()) {
                    return context;
                }
            }
        }
        throw new AssertionError("Aucun candidat gap trouve dans l'echantillon deterministe.");
    }

    private LevelZeroRegionWalkability region(int minCellX,
                                              int minCellZ,
                                              int maxCellX,
                                              int maxCellZ,
                                              WalkabilityRule rule) {
        int width = maxCellX - minCellX + 1;
        int height = maxCellZ - minCellZ + 1;
        boolean[] walkable = new boolean[width * height];
        LevelZeroSectorRoomKind[] roomKinds = new LevelZeroSectorRoomKind[walkable.length];
        for (int z = minCellZ; z <= maxCellZ; z++) {
            for (int x = minCellX; x <= maxCellX; x++) {
                int index = (z - minCellZ) * width + (x - minCellX);
                walkable[index] = rule.walkable(x, z);
                roomKinds[index] = LevelZeroSectorRoomKind.NONE;
            }
        }
        return new LevelZeroRegionWalkability(minCellX, minCellZ, maxCellX, maxCellZ, walkable, roomKinds);
    }

    private interface WalkabilityRule {
        boolean walkable(int cellX, int cellZ);
    }
}
