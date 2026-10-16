package com.petassegang.addons.backrooms.level0.generation.layout;

/**
 * Derivation des sorties cardinales et de la forme locale d'une cellule.
 */
final class LevelZeroTopology {

    private LevelZeroTopology() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Masque des voisins cardinaux ouverts d'une cellule.
     *
     * @param cellX coordonnee logique X
     * @param cellZ coordonnee logique Z
     * @param walkability fenetre de traversabilite
     * @return masque de {@link LevelZeroCellConnections}
     */
    static int connectionMask(int cellX, int cellZ, LevelZeroRegionWalkability walkability) {
        int mask = LevelZeroCellConnections.NONE;
        if (walkability.isWalkable(cellX, cellZ - 1)) {
            mask |= LevelZeroCellConnections.NORTH;
        }
        if (walkability.isWalkable(cellX + 1, cellZ)) {
            mask |= LevelZeroCellConnections.EAST;
        }
        if (walkability.isWalkable(cellX, cellZ + 1)) {
            mask |= LevelZeroCellConnections.SOUTH;
        }
        if (walkability.isWalkable(cellX - 1, cellZ)) {
            mask |= LevelZeroCellConnections.WEST;
        }
        return mask;
    }

    /**
     * Classe une cellule d'apres sa traversabilite, son marquage grande piece et ses sorties.
     *
     * @param walkable {@code true} si la cellule est ouverte
     * @param largeRoom {@code true} si la cellule est marquee grande piece
     * @param connectionMask sorties cardinales
     * @return forme locale
     */
    static LevelZeroCellTopology classify(boolean walkable, boolean largeRoom, int connectionMask) {
        if (!walkable) {
            return LevelZeroCellTopology.WALL;
        }
        if (largeRoom) {
            return LevelZeroCellTopology.ROOM_LARGE;
        }
        int degree = LevelZeroCellConnections.count(connectionMask);
        if (degree <= 1) {
            return LevelZeroCellTopology.DEAD_END;
        }
        if (degree == 2) {
            return LevelZeroCellConnections.isStraight(connectionMask)
                    ? LevelZeroCellTopology.CORRIDOR
                    : LevelZeroCellTopology.ANGLE;
        }
        return degree == 3 ? LevelZeroCellTopology.T_JUNCTION : LevelZeroCellTopology.CROSSROAD;
    }
}
