package com.petassegang.addons.world.backrooms.level0.layout;

/**
 * Carte canonique de traversabilite pour une region finie de cellules.
 */
public final class LevelZeroRegionWalkability {

    private final int minCellX;
    private final int minCellZ;
    private final int maxCellX;
    private final int maxCellZ;
    private final int width;
    private final boolean[] walkable;

    /**
     * Construit une carte finie de traversabilite regionale.
     *
     * @param minCellX borne minimale X incluse
     * @param minCellZ borne minimale Z incluse
     * @param maxCellX borne maximale X incluse
     * @param maxCellZ borne maximale Z incluse
     * @param walkable tableau ligne-major des cellules ouvertes
     */
    public LevelZeroRegionWalkability(int minCellX,
                                      int minCellZ,
                                      int maxCellX,
                                      int maxCellZ,
                                      boolean[] walkable) {
        this.minCellX = minCellX;
        this.minCellZ = minCellZ;
        this.maxCellX = maxCellX;
        this.maxCellZ = maxCellZ;
        this.width = maxCellX - minCellX + 1;
        this.walkable = walkable;
    }

    /**
     * Retourne la traversabilite d'une cellule de la region.
     *
     * @param cellX coordonnee cellule X
     * @param cellZ coordonnee cellule Z
     * @return {@code true} si la cellule est ouverte
     */
    public boolean sampleWalkableCell(int cellX, int cellZ) {
        if (cellX < minCellX || cellX > maxCellX || cellZ < minCellZ || cellZ > maxCellZ) {
            throw new IllegalArgumentException("Cellule hors de la region de walkability.");
        }
        return walkable[(cellZ - minCellZ) * width + (cellX - minCellX)];
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
}
