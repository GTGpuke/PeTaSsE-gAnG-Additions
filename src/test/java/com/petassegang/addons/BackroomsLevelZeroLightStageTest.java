package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.level.level0.biome.LevelZeroSurfaceBiome;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorRoomKind;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroCellContext;
import com.petassegang.addons.backrooms.level.level0.generation.stage.light.LevelZeroLightStage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie la trame lumineuse globale simplifiee du Level 0.
 */
@DisplayName("Light stage du Level 0")
class BackroomsLevelZeroLightStageTest {

    @Test
    @DisplayName("La premiere ligne alterne lampe puis vide")
    void testFirstPatternRowAlternatesStartingLit() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);

        assertEquals(true, stage.sample(new LevelZeroCellContext(0, 0, 12345L), LevelZeroSurfaceBiome.BASE, false),
                "La premiere ligne doit commencer par une lampe.");
        assertEquals(false, stage.sample(new LevelZeroCellContext(1, 0, 12345L), LevelZeroSurfaceBiome.BASE, false),
                "La premiere ligne doit alterner un vide apres une lampe.");
        assertEquals(true, stage.sample(new LevelZeroCellContext(2, 0, 12345L), LevelZeroSurfaceBiome.BASE, false),
                "La premiere ligne doit reprendre une lampe un bloc plus loin.");
    }

    @Test
    @DisplayName("La ligne suivante reste entierement eteinte")
    void testSecondPatternRowIsFullyDark() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);

        for (int cellX = -6; cellX <= 6; cellX++) {
            assertEquals(false,
                    stage.sample(new LevelZeroCellContext(cellX, 1, 12345L), LevelZeroSurfaceBiome.BASE, false),
                    "La ligne intermediaire du motif ne doit contenir aucune lampe.");
        }
    }

    @Test
    @DisplayName("La troisieme ligne inverse l'alternance de la premiere")
    void testThirdPatternRowUsesInvertedAlternation() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);

        assertEquals(false, stage.sample(new LevelZeroCellContext(0, 2, 12345L), LevelZeroSurfaceBiome.BASE, false),
                "La troisieme ligne doit inverser le demarrage de la premiere.");
        assertEquals(true, stage.sample(new LevelZeroCellContext(1, 2, 12345L), LevelZeroSurfaceBiome.BASE, false),
                "La troisieme ligne doit allumer la case suivante.");
        assertEquals(false, stage.sample(new LevelZeroCellContext(2, 2, 12345L), LevelZeroSurfaceBiome.BASE, false),
                "La troisieme ligne doit continuer l'alternance inversee.");
    }

    @Test
    @DisplayName("Le biome standard conserve le motif maximal sur tous les quatre rangs")
    void testBaseBiomeKeepsMaximumPattern() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);

        for (int cellX = -8; cellX <= 8; cellX++) {
            for (int cellZ = -8; cellZ <= 8; cellZ++) {
                boolean current = stage.sample(new LevelZeroCellContext(cellX, cellZ, 424242L),
                        LevelZeroSurfaceBiome.BASE,
                        false);
                boolean repeated = stage.sample(new LevelZeroCellContext(cellX, cellZ + 4, 424242L),
                        LevelZeroSurfaceBiome.BASE,
                        false,
                        LevelZeroSectorRoomKind.PILLAR_ROOM);
                assertEquals(current, repeated,
                        "Le biome standard doit conserver la trame maximale sans attenuation supplementaire.");
            }
        }
    }

    @Test
    @DisplayName("La trame globale ne colle jamais deux lampes orthogonales")
    void testPatternAvoidsOrthogonalAdjacency() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);

        for (int cellX = -12; cellX <= 12; cellX++) {
            for (int cellZ = -12; cellZ <= 12; cellZ++) {
                if (!stage.sample(new LevelZeroCellContext(cellX, cellZ, 98765L), LevelZeroSurfaceBiome.BASE, false)) {
                    continue;
                }
                assertEquals(false,
                        stage.sample(new LevelZeroCellContext(cellX + 1, cellZ, 98765L), LevelZeroSurfaceBiome.BASE, false),
                        "Deux lampes ne doivent pas etre collees horizontalement.");
                assertEquals(false,
                        stage.sample(new LevelZeroCellContext(cellX, cellZ + 1, 98765L), LevelZeroSurfaceBiome.BASE, false),
                        "Deux lampes ne doivent pas etre collees verticalement.");
            }
        }
    }

    @Test
    @DisplayName("La quatrieme ligne reste entierement eteinte")
    void testFourthPatternRowIsFullyDark() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);

        for (int cellX = -6; cellX <= 6; cellX++) {
            assertEquals(false,
                    stage.sample(new LevelZeroCellContext(cellX, 3, 12345L), LevelZeroSurfaceBiome.BASE, false),
                    "La quatrieme ligne du motif ne doit contenir aucune lampe.");
        }
    }

    @Test
    @DisplayName("Le biome rouge eclaircit la trame sans casser son alignement")
    void testRedBiomeReducesLightDensity() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);
        int baseLights = 0;
        int redLights = 0;

        for (int cellX = -20; cellX <= 20; cellX++) {
            for (int cellZ = -20; cellZ <= 20; cellZ++) {
                LevelZeroCellContext context = new LevelZeroCellContext(cellX, cellZ, 13579L, 1);
                boolean base = stage.sample(context, LevelZeroSurfaceBiome.BASE, false);
                boolean red = stage.sample(context, LevelZeroSurfaceBiome.RED, false);
                if (red) {
                    assertEquals(true, base,
                            "Un biome attenue ne doit jamais creer une lampe hors de la trame de base.");
                    redLights++;
                }
                if (base) {
                    baseLights++;
                }
            }
        }

        assertTrue(redLights < baseLights,
                "Le biome rouge doit etre globalement moins dense en neons que le biome de base.");
    }

    @Test
    @DisplayName("Certaines grandes pieces peuvent devenir entierement sombres de maniere rare")
    void testLargeRoomBlackoutExists() {
        LevelZeroLightStage stage = new LevelZeroLightStage(7);
        boolean blackoutFound = false;

        for (int cellX = -256; cellX <= 256 && !blackoutFound; cellX++) {
            for (int cellZ = -256; cellZ <= 256 && !blackoutFound; cellZ++) {
                LevelZeroCellContext context = new LevelZeroCellContext(cellX, cellZ, 24680L);
                boolean corridorLighting = stage.sample(context, LevelZeroSurfaceBiome.BASE, false);
                boolean largeRoomLighting = stage.sample(context, LevelZeroSurfaceBiome.BASE, true);
                if (corridorLighting && !largeRoomLighting) {
                    blackoutFound = true;
                }
            }
        }

        assertEquals(true, blackoutFound,
                "Le biome standard doit pouvoir produire tres rarement une grande piece sans neon.");
    }
}
