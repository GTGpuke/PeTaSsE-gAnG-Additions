package com.petassegang.addons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroLayerStackLayout;

class BackroomsLevelZeroLayerStackLayoutTest {

    @Test
    @DisplayName("Le schema vertical canonique respecte les offsets de la spec")
    void shouldRespectCanonicalLayerOffsets() {
        assertEquals(0, LevelZeroLayerStackLayout.baseY(0));
        assertEquals(0, LevelZeroLayerStackLayout.floorY(0));
        assertEquals(1, LevelZeroLayerStackLayout.airMinY(0));
        assertEquals(4, LevelZeroLayerStackLayout.airMaxY(0));
        assertEquals(5, LevelZeroLayerStackLayout.ceilingY(0));
        assertEquals(6, LevelZeroLayerStackLayout.interLayerBedrockMinY(0));
        assertEquals(9, LevelZeroLayerStackLayout.interLayerBedrockMaxY(0));
    }

    @Test
    @DisplayName("Chaque layer avance bien d'un pitch de 10 blocs")
    void shouldUsePitchOfTenBetweenLayers() {
        assertEquals(10, LevelZeroLayerStackLayout.baseY(1));
        assertEquals(20, LevelZeroLayerStackLayout.baseY(2));
        assertEquals(25, LevelZeroLayerStackLayout.ceilingY(2));
        assertEquals(26, LevelZeroLayerStackLayout.interLayerBedrockMinY(2));
        assertEquals(29, LevelZeroLayerStackLayout.interLayerBedrockMaxY(2));
    }

    @Test
    @DisplayName("La hauteur recommandee reste bornee a 3-5 layers")
    void shouldBoundRecommendedLayerCounts() {
        assertEquals(30, LevelZeroLayerStackLayout.worldHeight(3));
        assertEquals(40, LevelZeroLayerStackLayout.worldHeight(4));
        assertEquals(50, LevelZeroLayerStackLayout.worldHeight(5));
        assertEquals(26, LevelZeroLayerStackLayout.heightmapTopY(3));
        assertEquals(46, LevelZeroLayerStackLayout.heightmapTopY(5));
    }

    @Test
    @DisplayName("Les helpers d'interieur et de bedrock inter-layer restent cohérents")
    void shouldExposeInteriorAndInterLayerRanges() {
        assertTrue(LevelZeroLayerStackLayout.isInteriorY(1, 11));
        assertTrue(LevelZeroLayerStackLayout.isInteriorY(1, 14));
        assertFalse(LevelZeroLayerStackLayout.isInteriorY(1, 10));
        assertFalse(LevelZeroLayerStackLayout.isInteriorY(1, 15));

        assertTrue(LevelZeroLayerStackLayout.isInterLayerBedrockY(1, 16));
        assertTrue(LevelZeroLayerStackLayout.isInterLayerBedrockY(1, 19));
        assertFalse(LevelZeroLayerStackLayout.isInterLayerBedrockY(1, 15));
        assertFalse(LevelZeroLayerStackLayout.isInterLayerBedrockY(1, 20));
    }

    @Test
    @DisplayName("Les index et comptes de layers invalides sont rejetes")
    void shouldRejectInvalidIndicesAndCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> LevelZeroLayerStackLayout.baseY(-1));
        assertThrows(IllegalArgumentException.class,
                () -> LevelZeroLayerStackLayout.worldHeight(2));
        assertThrows(IllegalArgumentException.class,
                () -> LevelZeroLayerStackLayout.worldHeight(6));
    }
}
