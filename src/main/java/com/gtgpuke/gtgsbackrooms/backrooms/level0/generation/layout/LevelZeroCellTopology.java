package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout;

/**
 * Forme locale d'une cellule, deduite de ses sorties cardinales.
 */
public enum LevelZeroCellTopology {

    /** Mur plein, non traversable. */
    WALL,
    /** Couloir droit : deux sorties opposees. */
    CORRIDOR,
    /** Angle : deux sorties adjacentes. */
    ANGLE,
    /** Jonction en T : trois sorties. */
    T_JUNCTION,
    /** Carrefour : quatre sorties. */
    CROSSROAD,
    /** Cul-de-sac : zero ou une sortie. */
    DEAD_END,
    /** Cellule marquee grande piece, sans topologie de couloir. */
    ROOM_LARGE
}
