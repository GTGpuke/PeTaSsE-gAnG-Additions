package com.petassegang.addons.backrooms.level0.biome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Biomes cosmetiques du Level 0")
class LevelZeroSurfaceBiomeTest {

    @Test
    @DisplayName("Le biome est constant sur toute une region de 48 cellules")
    void biomeIsStablePerRegion() {
        LevelZeroSurfaceBiome reference = LevelZeroSurfaceBiome.sampleAtCell(0, 0, 1);
        for (int cellX = 0; cellX < LevelZeroSurfaceBiome.REGION_SIZE_CELLS; cellX += 7) {
            for (int cellZ = 0; cellZ < LevelZeroSurfaceBiome.REGION_SIZE_CELLS; cellZ += 5) {
                assertEquals(reference, LevelZeroSurfaceBiome.sampleAtCell(cellX, cellZ, 1), "Meme region, meme biome.");
            }
        }
        assertEquals(reference, LevelZeroSurfaceBiome.sampleAtWorld(96, 96, 1), "La lecture monde rejoint la lecture cellule.");
        assertEquals(LevelZeroSurfaceBiome.sampleAtRegion(0, 0, 1), reference, "La lecture region est la source.");
    }

    @Test
    @DisplayName("Le biome rouge n'apparait que sur les layers 1 et 3")
    void redBiomeLayerRestriction() {
        assertTrue(LevelZeroSurfaceBiome.BASE.supportsLayer(0), "Le biome de base est partout.");
        assertTrue(LevelZeroSurfaceBiome.BASE.supportsLayer(4), "Le biome de base est partout.");
        assertFalse(LevelZeroSurfaceBiome.RED.supportsLayer(0), "Pas de rouge au layer 0.");
        assertTrue(LevelZeroSurfaceBiome.RED.supportsLayer(1), "Rouge possible au layer 1.");
        assertFalse(LevelZeroSurfaceBiome.RED.supportsLayer(2), "Pas de rouge au layer 2.");
        assertTrue(LevelZeroSurfaceBiome.RED.supportsLayer(3), "Rouge possible au layer 3.");
        for (int regionX = -64; regionX <= 64; regionX++) {
            for (int regionZ = -64; regionZ <= 64; regionZ++) {
                assertEquals(LevelZeroSurfaceBiome.BASE, LevelZeroSurfaceBiome.sampleAtRegion(regionX, regionZ, 0),
                        "Le layer 0 est toujours en biome de base.");
            }
        }
    }

    @Test
    @DisplayName("Le biome rouge existe sur un layer autorise et varie d'un layer a l'autre")
    void redBiomeExistsAndVariesByLayer() {
        boolean foundRed = false;
        boolean foundDifference = false;
        for (int regionX = -32; regionX <= 32; regionX++) {
            for (int regionZ = -32; regionZ <= 32; regionZ++) {
                LevelZeroSurfaceBiome layerOne = LevelZeroSurfaceBiome.sampleAtRegion(regionX, regionZ, 1);
                LevelZeroSurfaceBiome layerThree = LevelZeroSurfaceBiome.sampleAtRegion(regionX, regionZ, 3);
                foundRed |= layerOne == LevelZeroSurfaceBiome.RED;
                foundDifference |= layerOne != layerThree;
            }
        }
        assertTrue(foundRed, "Le biome rouge doit etre tirable au layer 1.");
        assertTrue(foundDifference, "Les layers 1 et 3 ne partagent pas la meme carte de biomes.");
    }

    @Test
    @DisplayName("Seul le biome rouge porte des regions sombres et une densite reduite")
    void lightingProperties() {
        assertFalse(LevelZeroSurfaceBiome.BASE.supportsFullDarkRegions(), "Pas de region sombre en base.");
        assertTrue(LevelZeroSurfaceBiome.RED.supportsFullDarkRegions(), "Regions sombres possibles en rouge.");
        assertFalse(LevelZeroSurfaceBiome.BASE.isFullDarkRegion(10, 10, 42L), "Le biome de base n'est jamais sombre.");
        assertTrue(LevelZeroSurfaceBiome.BASE.keepsStripedLight(3, 4, 42L), "Le biome de base garde tous ses neons.");
        boolean darkFound = false;
        for (int cell = 0; cell < 48 * 64 && !darkFound; cell += 48) {
            darkFound = LevelZeroSurfaceBiome.RED.isFullDarkRegion(cell, 0, 42L);
        }
        assertTrue(darkFound, "Le biome rouge doit produire une region sombre sur 64 regions.");
    }

    @Test
    @DisplayName("Les neons pleins gardent chaque neon, les neons rares en gardent une partie")
    void stripedLightDensity() {
        int keptRed = 0;
        for (int cell = 0; cell < 256; cell++) {
            assertTrue(LevelZeroSurfaceBiome.BASE.keepsStripedLight(cell, -cell, 42L), "Le biome de base garde tous ses neons.");
            if (LevelZeroSurfaceBiome.RED.keepsStripedLight(cell, -cell, 42L)) {
                keptRed++;
            }
        }
        assertEquals(0, keptRed, "Le biome rouge n'a pas de neon raye.");
    }

    @Test
    @DisplayName("Le blackout des grandes pieces est rare et decide par zone de 24 cellules")
    void largeRoomBlackoutIsRareAndMacro() {
        for (LevelZeroSurfaceBiome biome : LevelZeroSurfaceBiome.values()) {
            int dark = 0;
            int zones = 0;
            for (int cellX = -2400; cellX <= 2400; cellX += 24) {
                boolean reference = biome.isLargeRoomBlackout(cellX, 0, 7L);
                assertEquals(reference, biome.isLargeRoomBlackout(cellX + 5, 11, 7L), "Meme zone, meme decision.");
                zones++;
                if (reference) {
                    dark++;
                }
            }
            assertTrue(dark > 0 && dark < zones / 4, biome + " : blackouts rares mais presents : " + dark + "/" + zones);
        }
    }
}
