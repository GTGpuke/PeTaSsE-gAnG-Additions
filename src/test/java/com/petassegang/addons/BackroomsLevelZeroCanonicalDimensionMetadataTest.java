package com.petassegang.addons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroLayerStackLayout;

class BackroomsLevelZeroCanonicalDimensionMetadataTest {

    @Test
    @DisplayName("La metadata verticale canonique cible 5 layers et 50 blocs de hauteur")
    void shouldExposeRecommendedCanonicalDimensionMetadata() {
        assertEquals(5, LevelZeroLayerStackLayout.defaultLayerCount());
        assertEquals(0, LevelZeroLayerStackLayout.minimumY());
        assertEquals(50, LevelZeroLayerStackLayout.recommendedWorldHeight());
        assertEquals(50, LevelZeroLayerStackLayout.recommendedContentHeight());
        assertEquals(64, LevelZeroLayerStackLayout.recommendedLogicalHeight());
        assertEquals(64, LevelZeroLayerStackLayout.recommendedDimensionHeight());
        assertEquals(46, LevelZeroLayerStackLayout.recommendedHeightmapTopY());
    }
}
