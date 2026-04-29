package com.petassegang.addons.backrooms.level.level0.generation.layout.sector;

/**
 * Donnees immuables d'un secteur logique du Level 0.
 */
public final class LevelZeroSectorData {

    private final boolean[] walkable;
    private final LevelZeroSectorRoomKind[] roomKinds;
    private final int sectorCols;

    /**
     * Construit un secteur precalcule.
     *
     * @param walkable cellules traversables du secteur
     * @param roomKinds types de salles portes par cellule
     * @param sectorCols largeur du secteur en cellules
     */
    public LevelZeroSectorData(boolean[] walkable, LevelZeroSectorRoomKind[] roomKinds, int sectorCols) {
        this.walkable = walkable;
        this.roomKinds = roomKinds;
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

    /**
     * Retourne le type de salle legacy d'une cellule locale de secteur.
     *
     * @param x coordonnee locale X
     * @param z coordonnee locale Z
     * @return type de salle porte par la cellule
     */
    public LevelZeroSectorRoomKind roomKind(int x, int z) {
        return roomKinds[z * sectorCols + x];
    }
}
