package com.petassegang.addons.backrooms.level0.generation.layout;

/**
 * Anomalies geometriques fines appliquees a une cellule 3x3, stockees en masque de bits.
 */
public enum LevelZeroGeometryFeature {

    /** Couloir resserre a une ligne sur le bord gauche de la cellule. */
    GAP_LEFT(1),
    /** Couloir resserre a une ligne au centre de la cellule. */
    GAP_MIDDLE(1 << 1),
    /** Couloir resserre a une ligne sur le bord droit de la cellule. */
    GAP_RIGHT(1 << 2);

    /** Masque sans aucune feature. */
    public static final int NONE = 0;

    private final int bit;

    LevelZeroGeometryFeature(int bit) {
        this.bit = bit;
    }

    /** Bit de cette feature dans le masque. */
    public int bit() {
        return bit;
    }

    /** {@code true} si le masque contient cette feature. */
    public boolean isIn(int mask) {
        return (mask & bit) != 0;
    }
}
