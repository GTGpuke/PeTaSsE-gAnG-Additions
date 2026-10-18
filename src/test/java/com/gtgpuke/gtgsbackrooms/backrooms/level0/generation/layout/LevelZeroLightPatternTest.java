package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gtgpuke.gtgsbackrooms.backrooms.level0.biome.LevelZeroSurfaceBiome;

@DisplayName("Trame lumineuse du Level 0")
class LevelZeroLightPatternTest {

    @Test
    @DisplayName("La trame alterne sur les rangs 0 et 2 et reste eteinte sur les rangs 1 et 3")
    void stripePattern() {
        assertTrue(LevelZeroLightPattern.isOnGlobalStripe(0, 0), "Rang 0, colonne paire : allume.");
        assertFalse(LevelZeroLightPattern.isOnGlobalStripe(1, 0), "Rang 0, colonne impaire : eteint.");
        assertFalse(LevelZeroLightPattern.isOnGlobalStripe(0, 1), "Rang 1 : eteint.");
        assertFalse(LevelZeroLightPattern.isOnGlobalStripe(0, 2), "Rang 2, colonne paire : eteint.");
        assertTrue(LevelZeroLightPattern.isOnGlobalStripe(1, 2), "Rang 2, colonne impaire : allume.");
        assertFalse(LevelZeroLightPattern.isOnGlobalStripe(1, 3), "Rang 3 : eteint.");
        assertTrue(LevelZeroLightPattern.isOnGlobalStripe(-2, -4), "La trame se prolonge en negatif.");
    }

    @Test
    @DisplayName("Deux neons de la trame ne sont jamais adjacents, meme en diagonale")
    void noAdjacentLights() {
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                if (!LevelZeroLightPattern.isOnGlobalStripe(x, z)) {
                    continue;
                }
                for (int offsetX = -1; offsetX <= 1; offsetX++) {
                    for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        if (offsetX != 0 || offsetZ != 0) {
                            assertFalse(LevelZeroLightPattern.isOnGlobalStripe(x + offsetX, z + offsetZ),
                                    "Deux neons ne doivent pas se toucher.");
                        }
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("Le biome de base garde toute la trame, le biome rouge l'eclaircit sans en sortir")
    void biomesFilterTheStripe() {
        int baseLights = 0;
        int redLights = 0;
        for (int x = -40; x <= 40; x++) {
            for (int z = -40; z <= 40; z++) {
                LevelZeroCellContext context = new LevelZeroCellContext(x, z, 13579L, 1);
                boolean stripe = LevelZeroLightPattern.isOnGlobalStripe(x, z);
                boolean base = LevelZeroLightPattern.isLit(context, LevelZeroSurfaceBiome.BASE, false);
                boolean red = LevelZeroLightPattern.isLit(context, LevelZeroSurfaceBiome.RED, false);
                assertTrue(base == stripe, "Le biome de base conserve exactement la trame.");
                assertTrue(!red || stripe, "Le biome rouge n'ajoute jamais de neon hors trame.");
                baseLights += base ? 1 : 0;
                redLights += red ? 1 : 0;
            }
        }
        assertTrue(redLights < baseLights, "Le biome rouge est moins dense : " + redLights + " < " + baseLights);
    }

    @Test
    @DisplayName("Une grande piece peut tomber en blackout, rarement")
    void largeRoomBlackoutExists() {
        boolean blackoutFound = false;
        for (int x = -256; x <= 256 && !blackoutFound; x++) {
            for (int z = -256; z <= 256 && !blackoutFound; z++) {
                LevelZeroCellContext context = new LevelZeroCellContext(x, z, 24680L, 0);
                blackoutFound = LevelZeroLightPattern.isLit(context, LevelZeroSurfaceBiome.BASE, false)
                        && !LevelZeroLightPattern.isLit(context, LevelZeroSurfaceBiome.BASE, true);
            }
        }
        assertTrue(blackoutFound, "Le biome de base doit produire au moins une grande piece sans neon.");
    }
}
