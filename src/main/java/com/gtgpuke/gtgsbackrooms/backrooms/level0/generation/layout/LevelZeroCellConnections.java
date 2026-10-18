package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout;

/**
 * Masque cardinal des sorties d'une cellule logique.
 */
public final class LevelZeroCellConnections {

    public static final int NONE = 0;
    public static final int NORTH = 1;
    public static final int EAST = 1 << 1;
    public static final int SOUTH = 1 << 2;
    public static final int WEST = 1 << 3;
    public static final int NORTH_SOUTH = NORTH | SOUTH;
    public static final int EAST_WEST = EAST | WEST;

    private LevelZeroCellConnections() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** {@code true} si le masque contient la direction. */
    public static boolean has(int mask, int direction) {
        return (mask & direction) != 0;
    }

    /** Nombre de sorties du masque. */
    public static int count(int mask) {
        return Integer.bitCount(mask & (NORTH | EAST | SOUTH | WEST));
    }

    /** {@code true} si le masque decrit exactement un couloir droit (nord-sud ou est-ouest). */
    public static boolean isStraight(int mask) {
        return mask == NORTH_SOUTH || mask == EAST_WEST;
    }
}
