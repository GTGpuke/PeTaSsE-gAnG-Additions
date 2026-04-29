package com.petassegang.addons.backrooms.level.level0.generation.layout;

/**
 * Representation semantique minimale des cellules du Level 0 actuel.
 *
 * <p>Cette premiere version ne couvre que les tags effectivement derives du
 * layout historique.
 *
 * <p>Le tag repond a la question la plus simple : "de quel grand type de
 * cellule parle-t-on ?". La topologie fine est ensuite portee par
 * {@code LevelZeroCellTopology}.
 */
public enum LevelZeroCellTag {

    /** Mur plein, non traversable. */
    WALL,
    /** Couloir ou espace standard traversable. */
    CORRIDOR,
    /** Zone traversable marquee comme grande piece. */
    ROOM_LARGE
}
