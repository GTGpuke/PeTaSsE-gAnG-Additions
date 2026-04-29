package com.petassegang.addons.backrooms.level.level0.generation.layout;

/**
 * Helpers de masque cardinal pour les cellules logiques.
 *
 * <p>Cette classe sert surtout a exprimer la forme locale d'un couloir :
 * nord, est, sud, ouest, et leurs combinaisons.
 */
public final class LevelZeroCellConnections {

    public static final int NORTH = 1;
    public static final int EAST = 1 << 1;
    public static final int SOUTH = 1 << 2;
    public static final int WEST = 1 << 3;

    private LevelZeroCellConnections() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    public static int none() {
        return 0;
    }

    public static int with(int mask, int direction) {
        return mask | direction;
    }

    public static boolean has(int mask, int direction) {
        return (mask & direction) != 0;
    }

    public static int count(int mask) {
        return Integer.bitCount(mask & (NORTH | EAST | SOUTH | WEST));
    }
}
