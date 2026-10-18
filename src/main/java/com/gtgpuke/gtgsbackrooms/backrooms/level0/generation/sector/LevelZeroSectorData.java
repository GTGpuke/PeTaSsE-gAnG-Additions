package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.sector;

/**
 * Donnees immuables d'un secteur logique : traversabilite et type de salle par cellule.
 *
 * <p>Les types de salle sont stockes en {@code byte} pour garder un secteur
 * compact (un secteur Level 0 fait 240x135 cellules).
 */
public final class LevelZeroSectorData {

    private final boolean[] walkable;
    private final byte[] roomKinds;
    private final int cols;

    LevelZeroSectorData(boolean[] walkable, byte[] roomKinds, int cols) {
        this.walkable = walkable;
        this.roomKinds = roomKinds;
        this.cols = cols;
    }

    /**
     * Traversabilite d'une cellule locale du secteur.
     *
     * @param x coordonnee locale X
     * @param z coordonnee locale Z
     * @return {@code true} si la cellule est ouverte
     */
    public boolean isWalkable(int x, int z) {
        return walkable[z * cols + x];
    }

    /**
     * Type de salle d'une cellule locale du secteur.
     *
     * @param x coordonnee locale X
     * @param z coordonnee locale Z
     * @return type de salle
     */
    public LevelZeroSectorRoomKind roomKind(int x, int z) {
        return LevelZeroSectorRoomKind.fromOrdinal(roomKinds[z * cols + x]);
    }
}
