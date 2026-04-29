package com.petassegang.addons.backrooms.level.level0.generation.stage;

import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroChunkCellWindow;

/**
 * Contexte canonique d'une region logique du Level 0.
 *
 * <p>Ce contexte regroupe la fenetre de cellules a echantillonner, la seed de
 * layout et le layer vise. Il sert aux etapes regionales qui reconstruisent
 * une vue deterministe plus large avant l'extraction par chunk.</p>
 */
public record LevelZeroRegionContext(
        LevelZeroChunkCellWindow window,
        long layoutSeed,
        int layerIndex) {

    /**
     * Construit un contexte regional sur le layer legacy implicite.
     *
     * @param window fenetre logique de cellules a echantillonner
     * @param layoutSeed seed deterministe du layout
     */
    public LevelZeroRegionContext(LevelZeroChunkCellWindow window, long layoutSeed) {
        this(window, layoutSeed, 0);
    }
}
