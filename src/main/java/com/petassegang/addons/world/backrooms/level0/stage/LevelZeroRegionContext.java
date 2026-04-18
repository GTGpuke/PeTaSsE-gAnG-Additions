package com.petassegang.addons.world.backrooms.level0.stage;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroChunkCellWindow;

/**
 * Contexte canonique d'une region logique du Level 0.
 */
public record LevelZeroRegionContext(
        LevelZeroChunkCellWindow window,
        long layoutSeed) {
}
