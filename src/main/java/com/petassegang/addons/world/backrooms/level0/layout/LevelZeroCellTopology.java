package com.petassegang.addons.world.backrooms.level0.layout;

/**
 * Topologie semantique plus fine d'une cellule du Level 0.
 */
public enum LevelZeroCellTopology {

    /** Mur plein, non traversable. */
    WALL,
    /** Couloir simple avec deux voisins opposes. */
    CORRIDOR,
    /** Carrefour ou angle connecte a plusieurs directions. */
    JUNCTION,
    /** Cul-de-sac. */
    DEAD_END,
    /** Grande piece historique. */
    ROOM_LARGE,
    /** Reserve pour de futures alcoves explicites. */
    ALCOVE
}
