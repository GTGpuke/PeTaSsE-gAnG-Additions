package com.petassegang.addons.world.backrooms.level0.layout;

/**
 * Representation semantique minimale des cellules du Level 0 actuel.
 *
 * <p>Cette premiere version ne couvre que les tags effectivement derives du
 * layout historique.
 */
public enum LevelZeroCellTag {

    /** Mur plein, non traversable. */
    WALL,
    /** Couloir ou espace standard traversable. */
    CORRIDOR,
    /** Zone traversable marquee comme grande piece. */
    ROOM_LARGE
}
