package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Gaps de couloir du Level 0")
class LevelZeroCorridorGapsTest {

    private static final long SEED = 998877L;

    @Test
    @DisplayName("Des gaps apparaissent sur les couloirs droits et nulle part ailleurs")
    void gapsOnlyOnStraightCorridors() {
        LevelZeroCorridorGaps gaps = new LevelZeroCorridorGaps(true);
        boolean found = false;
        for (int x = -64; x <= 64 && !found; x++) {
            for (int z = -64; z <= 64 && !found; z++) {
                found = gaps.sampleLocal(new LevelZeroCellContext(x, z, SEED, 0), LevelZeroCellTopology.CORRIDOR,
                        LevelZeroCellConnections.NORTH_SOUTH) != LevelZeroGeometryFeature.NONE;
            }
        }
        assertTrue(found, "Un echantillon de couloirs droits doit produire au moins un gap.");

        LevelZeroCellContext context = new LevelZeroCellContext(12, -8, SEED, 0);
        assertEquals(LevelZeroGeometryFeature.NONE, gaps.sampleLocal(context, LevelZeroCellTopology.ROOM_LARGE, 0), "Grande piece.");
        assertEquals(LevelZeroGeometryFeature.NONE, gaps.sampleLocal(context, LevelZeroCellTopology.ANGLE,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.EAST), "Angle.");
        assertEquals(LevelZeroGeometryFeature.NONE, gaps.sampleLocal(context, LevelZeroCellTopology.DEAD_END,
                LevelZeroCellConnections.NORTH), "Cul-de-sac.");
        assertEquals(LevelZeroGeometryFeature.NONE, new LevelZeroCorridorGaps(false).sampleLocal(context,
                LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.NORTH_SOUTH), "Couche desactivee.");
    }

    @Test
    @DisplayName("Un gap exige un mur de chaque cote du couloir")
    void gapRequiresSideWalls() {
        LevelZeroCorridorGaps gaps = new LevelZeroCorridorGaps(true);
        LevelZeroCellContext candidate = findCandidate(gaps);
        LevelZeroRegionWalkability openSides = LevelZeroRegionWalkability.of(
                candidate.cellX() - 1, candidate.cellZ() - 1, candidate.cellX() + 1, candidate.cellZ() + 1,
                (x, z) -> x == candidate.cellX() || z == candidate.cellZ() ? 1 : 0);
        assertEquals(LevelZeroGeometryFeature.NONE,
                gaps.sample(candidate, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.NORTH_SOUTH, openSides),
                "Sans murs lateraux, le gap est refuse.");

        LevelZeroRegionWalkability walledSides = LevelZeroRegionWalkability.of(
                candidate.cellX() - 1, candidate.cellZ() - 1, candidate.cellX() + 1, candidate.cellZ() + 1,
                (x, z) -> x == candidate.cellX() ? 1 : 0);
        assertTrue(gaps.sample(candidate, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.NORTH_SOUTH, walledSides)
                        != LevelZeroGeometryFeature.NONE || hasWinningNeighbor(gaps, candidate, walledSides),
                "Avec des murs lateraux, le gap est accepte sauf arbitrage de voisin.");
    }

    @Test
    @DisplayName("Deux gaps ne se suivent jamais dans le sens du passage")
    void gapsNeverTouch() {
        LevelZeroCorridorGaps gaps = new LevelZeroCorridorGaps(true);
        LevelZeroRegionWalkability corridor = LevelZeroRegionWalkability.of(-2, -512, 2, 512, (x, z) -> x == 0 ? 1 : 0);
        boolean foundGap = false;
        int previous = LevelZeroGeometryFeature.NONE;
        for (int z = -511; z <= 511; z++) {
            int current = gaps.sample(new LevelZeroCellContext(0, z, SEED, 0), LevelZeroCellTopology.CORRIDOR,
                    LevelZeroCellConnections.NORTH_SOUTH, corridor);
            foundGap |= current != LevelZeroGeometryFeature.NONE;
            assertTrue(previous == LevelZeroGeometryFeature.NONE || current == LevelZeroGeometryFeature.NONE,
                    "Deux gaps consecutifs sont interdits.");
            previous = current;
        }
        assertTrue(foundGap, "Le couloir de test doit garder au moins un gap apres arbitrage.");
    }

    @Test
    @DisplayName("Un couloir est-ouest suit les memes regles qu'un couloir nord-sud")
    void eastWestCorridorFollowsSameRules() {
        LevelZeroCorridorGaps gaps = new LevelZeroCorridorGaps(true);
        LevelZeroRegionWalkability corridor = LevelZeroRegionWalkability.of(-512, -2, 512, 2, (x, z) -> z == 0 ? 1 : 0);
        LevelZeroRegionWalkability openSides = LevelZeroRegionWalkability.of(-512, -2, 512, 2, (x, z) -> 1);
        boolean foundGap = false;
        int previous = LevelZeroGeometryFeature.NONE;
        for (int x = -511; x <= 511; x++) {
            LevelZeroCellContext context = new LevelZeroCellContext(x, 0, SEED, 0);
            int current = gaps.sample(context, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.EAST_WEST, corridor);
            foundGap |= current != LevelZeroGeometryFeature.NONE;
            assertTrue(previous == LevelZeroGeometryFeature.NONE || current == LevelZeroGeometryFeature.NONE,
                    "Deux gaps consecutifs sont interdits sur un couloir est-ouest.");
            assertEquals(LevelZeroGeometryFeature.NONE,
                    gaps.sample(context, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.EAST_WEST, openSides),
                    "Sans mur au nord et au sud, aucun gap est-ouest.");
            previous = current;
        }
        assertTrue(foundGap, "Le couloir est-ouest de test doit garder au moins un gap.");
    }

    @Test
    @DisplayName("Le motif 3x3 d'un gap ne laisse qu'une ligne d'un bloc dans le sens du couloir")
    void microPatternProjection() {
        int vertical = LevelZeroCorridorGaps.microPattern(LevelZeroCellConnections.NORTH_SOUTH,
                LevelZeroGeometryFeature.GAP_MIDDLE.bit());
        assertEquals(LevelZeroCellMicroPattern.bit(1, 0) | LevelZeroCellMicroPattern.bit(1, 1) | LevelZeroCellMicroPattern.bit(1, 2),
                vertical, "Gap central nord-sud : colonne du milieu.");

        int horizontal = LevelZeroCorridorGaps.microPattern(LevelZeroCellConnections.EAST_WEST,
                LevelZeroGeometryFeature.GAP_RIGHT.bit());
        assertEquals(LevelZeroCellMicroPattern.bit(0, 2) | LevelZeroCellMicroPattern.bit(1, 2) | LevelZeroCellMicroPattern.bit(2, 2),
                horizontal, "Gap droit est-ouest : ligne du bas.");

        assertEquals(LevelZeroCellMicroPattern.FULL_OPEN,
                LevelZeroCorridorGaps.microPattern(LevelZeroCellConnections.NORTH_SOUTH, LevelZeroGeometryFeature.NONE),
                "Sans feature, la cellule reste ouverte.");
        assertEquals(LevelZeroCellMicroPattern.FULL_OPEN,
                LevelZeroCorridorGaps.microPattern(LevelZeroCellConnections.NORTH | LevelZeroCellConnections.EAST,
                        LevelZeroGeometryFeature.GAP_LEFT.bit()),
                "Un gap sur un angle n'a pas d'orientation : la cellule reste ouverte.");
    }

    private static LevelZeroCellContext findCandidate(LevelZeroCorridorGaps gaps) {
        for (int x = -128; x <= 128; x++) {
            for (int z = -128; z <= 128; z++) {
                LevelZeroCellContext context = new LevelZeroCellContext(x, z, SEED, 0);
                if (gaps.sampleLocal(context, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.NORTH_SOUTH)
                        != LevelZeroGeometryFeature.NONE) {
                    return context;
                }
            }
        }
        throw new AssertionError("Aucun candidat gap dans l'echantillon deterministe.");
    }

    private static boolean hasWinningNeighbor(LevelZeroCorridorGaps gaps, LevelZeroCellContext context,
                                              LevelZeroRegionWalkability region) {
        return gaps.sampleLocal(context.offset(0, -1), LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.NORTH_SOUTH)
                != LevelZeroGeometryFeature.NONE
                || gaps.sampleLocal(context.offset(0, 1), LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.NORTH_SOUTH)
                != LevelZeroGeometryFeature.NONE;
    }

    @Test
    @DisplayName("Un gap est-ouest exige un mur au nord et au sud, comme un gap nord-sud exige l'est et l'ouest")
    void eastWestGapRequiresNorthAndSouthWalls() {
        LevelZeroCorridorGaps gaps = new LevelZeroCorridorGaps(true);
        LevelZeroCellContext candidate = null;
        for (int x = -4096; x <= 4096 && candidate == null; x++) {
            LevelZeroCellContext context = new LevelZeroCellContext(x, 7, SEED, 0);
            if (gaps.sampleLocal(context, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.EAST_WEST) != LevelZeroGeometryFeature.NONE) {
                candidate = context;
            }
        }
        assertNotNull(candidate, "Un couloir est-ouest a un gap local dans l'echantillon.");
        int cz = candidate.cellZ();
        int local = gaps.sampleLocal(candidate, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.EAST_WEST);
        LevelZeroRegionWalkability corridor = LevelZeroRegionWalkability.of(candidate.cellX() - 2, cz - 1, candidate.cellX() + 2, cz + 1,
                (x, z) -> z == cz ? 1 : 0);
        assertEquals(local, gaps.sample(candidate, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.EAST_WEST, corridor),
                "Murs au nord et au sud : le gap est garde.");
        LevelZeroRegionWalkability openSides = LevelZeroRegionWalkability.of(candidate.cellX() - 2, cz - 1, candidate.cellX() + 2, cz + 1,
                (x, z) -> 1);
        assertEquals(LevelZeroGeometryFeature.NONE,
                gaps.sample(candidate, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.EAST_WEST, openSides),
                "Sans mur au nord et au sud, pas de gap est-ouest.");
    }

    @Test
    @DisplayName("Le melange de hash interdit deux gaps locaux consecutifs le long d'un couloir")
    void adjacentLocalGapsNeverHappen() {
        LevelZeroCorridorGaps gaps = new LevelZeroCorridorGaps(true);
        int found = 0;
        for (int i = -20000; i < 20000; i++) {
            boolean eastWest = gaps.sampleLocal(new LevelZeroCellContext(i, 3, SEED, 0), LevelZeroCellTopology.CORRIDOR,
                    LevelZeroCellConnections.EAST_WEST) != LevelZeroGeometryFeature.NONE;
            boolean northSouth = gaps.sampleLocal(new LevelZeroCellContext(3, i, SEED, 0), LevelZeroCellTopology.CORRIDOR,
                    LevelZeroCellConnections.NORTH_SOUTH) != LevelZeroGeometryFeature.NONE;
            found += (eastWest ? 1 : 0) + (northSouth ? 1 : 0);
            assertFalse(eastWest && gaps.sampleLocal(new LevelZeroCellContext(i + 1, 3, SEED, 0), LevelZeroCellTopology.CORRIDOR,
                    LevelZeroCellConnections.EAST_WEST) != LevelZeroGeometryFeature.NONE, "Gaps est-ouest voisins en " + i + ".");
            assertFalse(northSouth && gaps.sampleLocal(new LevelZeroCellContext(3, i + 1, SEED, 0), LevelZeroCellTopology.CORRIDOR,
                    LevelZeroCellConnections.NORTH_SOUTH) != LevelZeroGeometryFeature.NONE, "Gaps nord-sud voisins en " + i + ".");
        }
        assertTrue(found > 100, "L'echantillon contient de nombreux gaps locaux : " + found);
    }

    @Test
    @DisplayName("Un couloir qui tourne n'a pas de gap, et un seul mur lateral ne suffit pas")
    void bentCorridorsAndSingleWalls() {
        LevelZeroCorridorGaps gaps = new LevelZeroCorridorGaps(true);
        LevelZeroCellContext bent = new LevelZeroCellContext(12, -8, SEED, 0);
        assertEquals(LevelZeroGeometryFeature.NONE,
                gaps.sampleLocal(bent, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.NORTH | LevelZeroCellConnections.EAST),
                "Un couloir coude n'est pas droit.");
        LevelZeroCellContext northSouth = null;
        LevelZeroCellContext eastWest = null;
        for (int i = -4096; i <= 4096 && (northSouth == null || eastWest == null); i++) {
            LevelZeroCellContext column = new LevelZeroCellContext(5, i, SEED, 0);
            if (northSouth == null && gaps.sampleLocal(column, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.NORTH_SOUTH) != LevelZeroGeometryFeature.NONE) {
                northSouth = column;
            }
            LevelZeroCellContext row = new LevelZeroCellContext(i, 5, SEED, 0);
            if (eastWest == null && gaps.sampleLocal(row, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.EAST_WEST) != LevelZeroGeometryFeature.NONE) {
                eastWest = row;
            }
        }
        assertNotNull(northSouth, "Un gap local nord-sud existe.");
        assertNotNull(eastWest, "Un gap local est-ouest existe.");
        int nsX = northSouth.cellX();
        int nsZ = northSouth.cellZ();
        LevelZeroRegionWalkability westWallOnly = LevelZeroRegionWalkability.of(nsX - 1, nsZ - 1, nsX + 1, nsZ + 1, (x, z) -> x == nsX - 1 ? 0 : 1);
        assertEquals(LevelZeroGeometryFeature.NONE,
                gaps.sample(northSouth, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.NORTH_SOUTH, westWallOnly),
                "Mur a l'ouest mais passage a l'est : pas de gap nord-sud.");
        int ewX = eastWest.cellX();
        int ewZ = eastWest.cellZ();
        LevelZeroRegionWalkability northWallOnly = LevelZeroRegionWalkability.of(ewX - 1, ewZ - 1, ewX + 1, ewZ + 1, (x, z) -> z == ewZ - 1 ? 0 : 1);
        assertEquals(LevelZeroGeometryFeature.NONE,
                gaps.sample(eastWest, LevelZeroCellTopology.CORRIDOR, LevelZeroCellConnections.EAST_WEST, northWallOnly),
                "Mur au nord mais passage au sud : pas de gap est-ouest.");
    }
}
