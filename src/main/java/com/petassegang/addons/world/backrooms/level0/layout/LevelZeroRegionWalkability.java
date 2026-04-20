package com.petassegang.addons.world.backrooms.level0.layout;

import com.petassegang.addons.world.backrooms.level0.layout.sector.LevelZeroSectorRoomKind;

/**
 * Carte regionale minimale de verite brute avant enrichissement semantique.
 *
 * <p>Cette structure ne decrit que deux choses :
 * quelles cellules sont ouvertes,
 * et a quel type de salle historique elles appartiennent.
 *
 * <p>La pipeline legacy s'appuie ensuite sur cette base pour deriver biome,
 * topologie, micro-geometrie et lumiere.
 */
public final class LevelZeroRegionWalkability {

    private final int minCellX;
    private final int minCellZ;
    private final int maxCellX;
    private final int maxCellZ;
    private final int width;
    private final boolean[] walkable;
    private final LevelZeroSectorRoomKind[] roomKinds;

    /**
     * Construit une carte finie de traversabilite regionale.
     *
     * @param minCellX borne minimale X incluse
     * @param minCellZ borne minimale Z incluse
     * @param maxCellX borne maximale X incluse
     * @param maxCellZ borne maximale Z incluse
     * @param walkable tableau ligne-major des cellules ouvertes
     * @param roomKinds tableau ligne-major des types de salle
     */
    public LevelZeroRegionWalkability(int minCellX,
                                      int minCellZ,
                                      int maxCellX,
                                      int maxCellZ,
                                      boolean[] walkable,
                                      LevelZeroSectorRoomKind[] roomKinds) {
        this.minCellX = minCellX;
        this.minCellZ = minCellZ;
        this.maxCellX = maxCellX;
        this.maxCellZ = maxCellZ;
        this.width = maxCellX - minCellX + 1;
        this.walkable = walkable;
        this.roomKinds = roomKinds;
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

    /**
     * Retourne le type de salle legacy d'une cellule de la region.
     *
     * @param cellX coordonnee cellule X
     * @param cellZ coordonnee cellule Z
     * @return type de salle derive
     */
    public LevelZeroSectorRoomKind sampleRoomKindCell(int cellX, int cellZ) {
        if (cellX < minCellX || cellX > maxCellX || cellZ < minCellZ || cellZ > maxCellZ) {
            throw new IllegalArgumentException("Cellule hors de la region de walkability.");
        }
        return roomKinds[(cellZ - minCellZ) * width + (cellX - minCellX)];
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
