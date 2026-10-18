package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout;

import java.util.function.IntBinaryOperator;

import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.sector.LevelZeroSectorRoomKind;

/**
 * Fenetre rectangulaire de cellules avec leur traversabilite brute et leur type de salle.
 *
 * <p>C'est la verite structurelle sur laquelle toute l'evaluation semantique
 * s'appuie. Hors de la fenetre, une cellule est consideree comme un mur : la
 * fenetre doit donc inclure une marge suffisante autour de la zone evaluee.
 */
public final class LevelZeroRegionWalkability {

    private final int minCellX;
    private final int minCellZ;
    private final int maxCellX;
    private final int maxCellZ;
    private final int width;
    private final boolean[] walkable;
    private final byte[] roomKinds;

    private LevelZeroRegionWalkability(int minCellX, int minCellZ, int maxCellX, int maxCellZ,
                                       boolean[] walkable, byte[] roomKinds) {
        this.minCellX = minCellX;
        this.minCellZ = minCellZ;
        this.maxCellX = maxCellX;
        this.maxCellZ = maxCellZ;
        this.width = maxCellX - minCellX + 1;
        this.walkable = walkable;
        this.roomKinds = roomKinds;
    }

    /**
     * Construit une fenetre a partir de fonctions d'echantillonnage.
     *
     * @param minCellX borne minimale X incluse
     * @param minCellZ borne minimale Z incluse
     * @param maxCellX borne maximale X incluse
     * @param maxCellZ borne maximale Z incluse
     * @param walkableSampler (cellX, cellZ) vers 1 si ouvert, 0 sinon
     * @param roomKindSampler (cellX, cellZ) vers ordinal du type de salle
     * @return fenetre remplie
     */
    public static LevelZeroRegionWalkability sample(int minCellX, int minCellZ, int maxCellX, int maxCellZ,
                                                    IntBinaryOperator walkableSampler,
                                                    IntBinaryOperator roomKindSampler) {
        int width = maxCellX - minCellX + 1;
        int height = maxCellZ - minCellZ + 1;
        boolean[] walkable = new boolean[width * height];
        byte[] roomKinds = new byte[width * height];
        for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
            for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
                int index = (cellZ - minCellZ) * width + (cellX - minCellX);
                walkable[index] = walkableSampler.applyAsInt(cellX, cellZ) != 0;
                roomKinds[index] = (byte) roomKindSampler.applyAsInt(cellX, cellZ);
            }
        }
        return new LevelZeroRegionWalkability(minCellX, minCellZ, maxCellX, maxCellZ, walkable, roomKinds);
    }

    /**
     * Construit une fenetre sans type de salle, pratique pour les tests.
     *
     * @param minCellX borne minimale X incluse
     * @param minCellZ borne minimale Z incluse
     * @param maxCellX borne maximale X incluse
     * @param maxCellZ borne maximale Z incluse
     * @param walkableSampler (cellX, cellZ) vers 1 si ouvert, 0 sinon
     * @return fenetre remplie
     */
    public static LevelZeroRegionWalkability of(int minCellX, int minCellZ, int maxCellX, int maxCellZ,
                                                IntBinaryOperator walkableSampler) {
        return sample(minCellX, minCellZ, maxCellX, maxCellZ, walkableSampler, (x, z) -> 0);
    }

    /** {@code true} si la cellule est dans la fenetre. */
    public boolean contains(int cellX, int cellZ) {
        return cellX >= minCellX && cellX <= maxCellX && cellZ >= minCellZ && cellZ <= maxCellZ;
    }

    /** {@code true} si la cellule est ouverte ; une cellule hors fenetre est un mur. */
    public boolean isWalkable(int cellX, int cellZ) {
        return contains(cellX, cellZ) && walkable[index(cellX, cellZ)];
    }

    /** Type de salle de la cellule ; {@link LevelZeroSectorRoomKind#NONE} hors fenetre. */
    public LevelZeroSectorRoomKind roomKind(int cellX, int cellZ) {
        return contains(cellX, cellZ)
                ? LevelZeroSectorRoomKind.fromOrdinal(roomKinds[index(cellX, cellZ)])
                : LevelZeroSectorRoomKind.NONE;
    }

    public int minCellX() {
        return minCellX;
    }

    public int minCellZ() {
        return minCellZ;
    }

    public int maxCellX() {
        return maxCellX;
    }

    public int maxCellZ() {
        return maxCellZ;
    }

    private int index(int cellX, int cellZ) {
        return (cellZ - minCellZ) * width + (cellX - minCellX);
    }
}
