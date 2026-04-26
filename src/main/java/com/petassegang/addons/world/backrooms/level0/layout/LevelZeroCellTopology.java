package com.petassegang.addons.world.backrooms.level0.layout;

/**
 * Topologie semantique fine d'une cellule du Level 0.
 *
 * <p>La topologie affine le tag principal en decrivant la forme locale du
 * passage : angle, T, carrefour, cul-de-sac, grande piece, etc.
 */
public enum LevelZeroCellTopology {

    /** Mur plein, non traversable. */
    WALL,
    /** Couloir simple avec deux voisins opposes. */
    CORRIDOR,
    /** Angle simple avec deux voisins adjacents. */
    ANGLE,
    /** Jonction en T avec trois voisins. */
    T_JUNCTION,
    /** Carrefour a quatre directions. */
    CROSSROAD,
    /** Alias legacy conservant la notion generique de jonction. */
    JUNCTION,
    /** Cul-de-sac. */
    DEAD_END,
    /** Grande piece historique. */
    ROOM_LARGE,
    /** Reserve pour de futures alcoves explicites. */
    ALCOVE
}
