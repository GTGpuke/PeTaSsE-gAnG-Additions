package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
import com.petassegang.addons.world.backrooms.level0.layout.sector.LevelZeroSectorRoomKind;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;
import com.petassegang.addons.world.backrooms.level0.stage.light.LevelZeroLightStage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie la logique de densite lumineuse du Level 0.
 */
@DisplayName("Light stage du Level 0")
class BackroomsLevelZeroLightStageTest {

    @Test
    @DisplayName("Une grande piece utilise une grille lumineuse dediee et deterministe")
    void testLargeRoomLightingUsesDedicatedPattern() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);
        LevelZeroCellContext context = new LevelZeroCellContext(12, 14, 12345L);

        boolean first = stage.sample(context, LevelZeroSurfaceBiome.BASE, true, LevelZeroSectorRoomKind.RECT_ROOM);
        boolean second = stage.sample(context, LevelZeroSurfaceBiome.BASE, true, LevelZeroSectorRoomKind.RECT_ROOM);

        assertEquals(first, second,
                "Le mode lumineux des grandes pieces doit rester deterministe.");
    }

    @Test
    @DisplayName("Les salles a piliers utilisent une grille plus large que les grandes rooms standard")
    void testPillarRoomLightingUsesWiderGrid() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);
        int rectLit = 0;
        int pillarLit = 0;

        for (int cellX = 0; cellX < 12; cellX++) {
            for (int cellZ = 0; cellZ < 12; cellZ++) {
                LevelZeroCellContext context = new LevelZeroCellContext(cellX, cellZ, 12345L);
                if (stage.sample(context, LevelZeroSurfaceBiome.BASE, true, LevelZeroSectorRoomKind.RECT_ROOM)) {
                    rectLit++;
                }
                if (stage.sample(context, LevelZeroSurfaceBiome.BASE, true, LevelZeroSectorRoomKind.PILLAR_ROOM)) {
                    pillarLit++;
                }
            }
        }

        assertTrue(rectLit > pillarLit,
                "Une salle a piliers doit utiliser une trame lumineuse plus aeree qu'une grande room standard.");
    }

    @Test
    @DisplayName("Une grande room suit un pattern unique de lignes ou de diagonales par groupe")
    void testLargeRoomUsesSingleReadablePatternPerGroup() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);
        int minCellX = 16;
        int minCellZ = 24;
        int maxCellX = minCellX + 7;
        int maxCellZ = minCellZ + 7;
        int litCount = 0;
        boolean constantX = false;
        boolean constantZ = false;
        boolean constantDiff = false;
        boolean constantSum = false;
        Integer lineX = null;
        Integer lineZ = null;
        Integer diagonalDiff = null;
        Integer diagonalSum = null;

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                LevelZeroCellContext context = new LevelZeroCellContext(cellX, cellZ, 12345L);
                if (!stage.sample(context, LevelZeroSurfaceBiome.BASE, true, LevelZeroSectorRoomKind.RECT_ROOM)) {
                    continue;
                }
                litCount++;
                if (lineX == null) {
                    lineX = cellX;
                    lineZ = cellZ;
                    diagonalDiff = cellX - cellZ;
                    diagonalSum = cellX + cellZ;
                    constantX = true;
                    constantZ = true;
                    constantDiff = true;
                    constantSum = true;
                } else {
                    constantX &= lineX == cellX;
                    constantZ &= lineZ == cellZ;
                    constantDiff &= diagonalDiff == cellX - cellZ;
                    constantSum &= diagonalSum == cellX + cellZ;
                }
            }
        }

        assertTrue(litCount > 0,
                "Le groupe de grande room doit produire au moins quelques neons hors blackout.");
        assertTrue(constantX || constantZ || constantDiff || constantSum,
                "Une grande room doit suivre un pattern unique lisible : ligne ou diagonale.");
    }

    @Test
    @DisplayName("Les grandes rooms n'autorisent pas non plus deux cellules lighted orthogonales collees")
    void testLargeRoomLightingAvoidsOrthogonalAdjacentCells() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);

        for (int cellX = 0; cellX < 24; cellX++) {
            for (int cellZ = 0; cellZ < 24; cellZ++) {
                LevelZeroCellContext context = new LevelZeroCellContext(cellX, cellZ, 13579L);
                if (!stage.sample(context, LevelZeroSurfaceBiome.BASE, true, LevelZeroSectorRoomKind.RECT_ROOM)) {
                    continue;
                }
                assertEquals(false,
                        stage.sample(new LevelZeroCellContext(cellX + 1, cellZ, 13579L),
                                LevelZeroSurfaceBiome.BASE,
                                true,
                                LevelZeroSectorRoomKind.RECT_ROOM),
                        "Deux cellules lighted d'une grande room ne doivent pas etre collees sur X.");
                assertEquals(false,
                        stage.sample(new LevelZeroCellContext(cellX, cellZ + 1, 13579L),
                                LevelZeroSurfaceBiome.BASE,
                                true,
                                LevelZeroSectorRoomKind.RECT_ROOM),
                        "Deux cellules lighted d'une grande room ne doivent pas etre collees sur Z.");
            }
        }
    }

    @Test
    @DisplayName("Le biome rouge garde une trame lumineuse plus eparse que le biome de base")
    void testRedBiomeUsesSparserLightingGrid() {
        assertTrue(LevelZeroSurfaceBiome.RED.lightGridSpacing() > LevelZeroSurfaceBiome.BASE.lightGridSpacing(),
                "Le biome rouge doit utiliser une grille lumineuse plus large.");
        assertTrue(LevelZeroSurfaceBiome.RED.lightDropoutModulo() < LevelZeroSurfaceBiome.BASE.lightDropoutModulo(),
                "Le biome rouge doit aussi supprimer differemment les points de trame pour conserver une ambiance distincte.");
    }

    @Test
    @DisplayName("Les regions full dark du biome rouge restent detectables de maniere deterministe")
    void testRedBiomeFullDarkRegionIsDeterministic() {
        boolean found = false;

        for (int cellX = -256; cellX <= 256 && !found; cellX += 24) {
            for (int cellZ = -256; cellZ <= 256 && !found; cellZ += 24) {
                boolean first = LevelZeroSurfaceBiome.RED.isFullDarkRegion(cellX, cellZ, 998877L);
                boolean second = LevelZeroSurfaceBiome.RED.isFullDarkRegion(cellX, cellZ, 998877L);
                assertEquals(first, second,
                        "La detection de region full dark doit rester deterministe.");
                found = first;
            }
        }

        assertEquals(true, found,
                "Sur un echantillon raisonnable, on doit pouvoir rencontrer au moins une region rouge totalement sombre.");
    }

    @Test
    @DisplayName("La lumiere normale reste alignee sur une trame globale fixe")
    void testBiomeLightingUsesGlobalAlignedGrid() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);
        Integer litModX = null;
        Integer litModZ = null;
        int spacing = LevelZeroSurfaceBiome.BASE.lightGridSpacing();

        for (int cellX = 0; cellX < 12; cellX++) {
            for (int cellZ = 0; cellZ < 12; cellZ++) {
                if (!stage.sample(new LevelZeroCellContext(cellX, cellZ, 12345L), LevelZeroSurfaceBiome.BASE, false)) {
                    continue;
                }
                int modX = Math.floorMod(cellX, spacing);
                int modZ = Math.floorMod(cellZ, spacing);
                if (litModX == null) {
                    litModX = modX;
                    litModZ = modZ;
                } else {
                    assertEquals(litModX, modX,
                            "Dans un meme groupe, les neons d'un biome doivent partager le meme alignement X.");
                    assertEquals(litModZ, modZ,
                            "Dans un meme groupe, les neons d'un biome doivent partager le meme alignement Z.");
                }
            }
        }

        for (int cellX = 24; cellX < 36; cellX++) {
            for (int cellZ = 24; cellZ < 36; cellZ++) {
                if (!stage.sample(new LevelZeroCellContext(cellX, cellZ, 12345L), LevelZeroSurfaceBiome.BASE, false)) {
                    continue;
                }
                assertEquals(litModX, Math.floorMod(cellX, spacing),
                        "L'alignement X doit rester globalement stable meme loin d'un autre paquet.");
                assertEquals(litModZ, Math.floorMod(cellZ, spacing),
                        "L'alignement Z doit rester globalement stable meme loin d'un autre paquet.");
            }
        }

        assertTrue(litModX != null,
                "Sur la trame de biome, on doit rencontrer au moins un point lumineux.");
    }

    @Test
    @DisplayName("La trame lumineuse evite les doublons orthogonaux adjacents")
    void testLightingAvoidsOrthogonalAdjacentLights() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);

        for (int cellX = 0; cellX < 24; cellX++) {
            for (int cellZ = 0; cellZ < 24; cellZ++) {
                boolean lit = stage.sample(new LevelZeroCellContext(cellX, cellZ, 12345L), LevelZeroSurfaceBiome.BASE, false);
                if (!lit) {
                    continue;
                }
                assertEquals(false,
                        stage.sample(new LevelZeroCellContext(cellX + 1, cellZ, 12345L), LevelZeroSurfaceBiome.BASE, false),
                        "Deux cellules lumineuses normales ne doivent pas rester cote a cote sur X.");
                assertEquals(false,
                        stage.sample(new LevelZeroCellContext(cellX, cellZ + 1, 12345L), LevelZeroSurfaceBiome.BASE, false),
                        "Deux cellules lumineuses normales ne doivent pas rester cote a cote sur Z.");
            }
        }
    }

    @Test
    @DisplayName("La regle anti-proximite des cellules normales reste independante des grandes rooms")
    void testNormalLightingProximityRuleRemainsIsolated() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);

        for (int cellX = 0; cellX < 24; cellX++) {
            for (int cellZ = 0; cellZ < 24; cellZ++) {
                LevelZeroCellContext context = new LevelZeroCellContext(cellX, cellZ, 98765L);
                if (!stage.sample(context, LevelZeroSurfaceBiome.BASE, false, LevelZeroSectorRoomKind.NONE)) {
                    continue;
                }
                assertEquals(false,
                        stage.sample(new LevelZeroCellContext(cellX + 1, cellZ, 98765L),
                                LevelZeroSurfaceBiome.BASE,
                                false,
                                LevelZeroSectorRoomKind.NONE),
                        "Deux cellules normales lumineuses ne doivent pas etre collees sur X.");
                assertEquals(false,
                        stage.sample(new LevelZeroCellContext(cellX, cellZ + 1, 98765L),
                                LevelZeroSurfaceBiome.BASE,
                                false,
                                LevelZeroSectorRoomKind.NONE),
                        "Deux cellules normales lumineuses ne doivent pas etre collees sur Z.");
            }
        }
    }

    @Test
    @DisplayName("Les cellules normales lighted ne doivent pas non plus se toucher en diagonale")
    void testNormalLightingAvoidsDiagonalAdjacency() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);

        for (int cellX = 0; cellX < 24; cellX++) {
            for (int cellZ = 0; cellZ < 24; cellZ++) {
                LevelZeroCellContext context = new LevelZeroCellContext(cellX, cellZ, 45678L);
                if (!stage.sample(context, LevelZeroSurfaceBiome.BASE, false, LevelZeroSectorRoomKind.NONE)) {
                    continue;
                }
                assertEquals(false,
                        stage.sample(new LevelZeroCellContext(cellX + 1, cellZ + 1, 45678L),
                                LevelZeroSurfaceBiome.BASE,
                                false,
                                LevelZeroSectorRoomKind.NONE),
                        "Deux cellules normales lumineuses ne doivent pas se toucher en diagonale.");
            }
        }
    }
}
