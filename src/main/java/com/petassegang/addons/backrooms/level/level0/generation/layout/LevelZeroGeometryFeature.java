package com.petassegang.addons.backrooms.level.level0.generation.layout;

/**
 * Features geometriques fines appliquees localement au-dessus de la grille 3x3.
 *
 * <p>Chaque variante doit rester petite, testable et ajoutee une par une afin
 * d'eviter de melanger plusieurs problemes visuels dans le meme pass.
 */
public enum LevelZeroGeometryFeature {

    /** Aucun ajustement geometrique local. */
    NONE(0),
    /** Passage resserre sur le bord gauche de la cellule 3x3. */
    GAP_LEFT(1),
    /** Passage resserre au centre de la cellule 3x3. */
    GAP_MIDDLE(1 << 1),
    /** Passage resserre sur le bord droit de la cellule 3x3. */
    GAP_RIGHT(1 << 2);

    private final int bit;

    LevelZeroGeometryFeature(int bit) {
        this.bit = bit;
    }

    /**
     * Retourne le bit associe a cette feature.
     *
     * @return bit du masque geometrique
     */
    public int bit() {
        return bit;
    }
}
