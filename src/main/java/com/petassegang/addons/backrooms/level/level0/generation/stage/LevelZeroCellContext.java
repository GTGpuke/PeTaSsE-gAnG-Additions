package com.petassegang.addons.backrooms.level.level0.generation.stage;

/**
 * Contexte canonique d'une cellule logique du Level 0.
 *
 * <p>Ce contexte represente l'unite de travail minimale de la pipeline legacy :
 * une cellule de la grille logique 3x3, evaluee de maniere deterministe a
 * partir de sa position, de la seed de layout et du layer courant.</p>
 */
public record LevelZeroCellContext(
        int cellX,
        int cellZ,
        long layoutSeed,
        int layerIndex) {

    /**
     * Construit un contexte de cellule sur le layer legacy implicite.
     *
     * @param cellX coordonnee logique X de la cellule
     * @param cellZ coordonnee logique Z de la cellule
     * @param layoutSeed seed deterministe du layout
     */
    public LevelZeroCellContext(int cellX, int cellZ, long layoutSeed) {
        this(cellX, cellZ, layoutSeed, 0);
    }
}
