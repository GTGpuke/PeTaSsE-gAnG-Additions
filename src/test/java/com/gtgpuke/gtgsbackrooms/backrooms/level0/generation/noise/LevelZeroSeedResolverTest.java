package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;

@DisplayName("Seeds de layer du Level 0")
class LevelZeroSeedResolverTest {

    @Test
    @DisplayName("Chaque layer recoit une seed distincte et stable")
    void layerSeedsAreDistinctAndStable() {
        Set<Long> seeds = new HashSet<>();
        for (int layer = 0; layer < LevelZeroLayerStackLayout.LAYER_COUNT; layer++) {
            long seed = LevelZeroSeedResolver.resolveLayerSeed(42L, layer);
            assertEquals(seed, LevelZeroSeedResolver.resolveLayerSeed(42L, layer), "Seed stable pour le layer " + layer + ".");
            seeds.add(seed);
        }
        assertEquals(LevelZeroLayerStackLayout.LAYER_COUNT, seeds.size(),
                "Deux layers ne doivent jamais partager le meme labyrinthe.");
    }

    @Test
    @DisplayName("La seed de layer suit le mix LAYER_LAYOUT")
    void layerSeedUsesLayerLayoutStage() {
        assertEquals(StageRandom.mix(42L, StageRandom.Stage.LAYER_LAYOUT, 2, 0L),
                LevelZeroSeedResolver.resolveLayerSeed(42L, 2),
                "La derivation de layer est fixee par le sel LAYER_LAYOUT.");
        assertNotEquals(LevelZeroSeedResolver.resolveLayerSeed(42L, 0), LevelZeroSeedResolver.resolveLayerSeed(43L, 0),
                "Deux mondes donnent des layers differents.");
    }
}
