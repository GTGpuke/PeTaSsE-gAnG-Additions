package com.petassegang.addons.world.backrooms.level0.layout;

/**
 * Source minimale de traversabilite pour une cellule logique.
 */
@FunctionalInterface
public interface LevelZeroWalkabilitySampler {

    /**
     * Echantillonne la traversabilite d'une cellule logique.
     *
     * @param cellX coordonnee cellule X
     * @param cellZ coordonnee cellule Z
     * @return {@code true} si la cellule est ouverte
     */
    boolean sampleWalkableCell(int cellX, int cellZ);
}
