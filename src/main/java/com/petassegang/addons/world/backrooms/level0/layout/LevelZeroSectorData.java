package com.petassegang.addons.world.backrooms.level0.layout;

/**
 * Donnees immuables d'un secteur logique du Level 0.
 */
public final class LevelZeroSectorData {

    private final boolean[] walkable;
    private final int sectorCols;

    /**
     * Construit un secteur precalcule.
     *
     * @param walkable cellules traversables du secteur
     * @param sectorCols largeur du secteur en cellules
     */
    public LevelZeroSectorData(boolean[] walkable, int sectorCols) {
        this.walkable = walkable;
        this.sectorCols = sectorCols;
    }

    /**
     * Retourne si une cellule locale de secteur est traversable.
     *
     * @param x coordonnee locale X
     * @param z coordonnee locale Z
     * @return {@code true} si la cellule est ouverte
     */
    public boolean isWalkable(int x, int z) {
        return walkable[z * sectorCols + x];
    }
}
