package com.petassegang.addons.world.backrooms.level0.write;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellState;

/**
 * Colonne totalement resolue, entre semantique de layout et ecriture finale.
 */
public record LevelZeroResolvedColumn(
        LevelZeroColumnCoordinates coordinates,
        LevelZeroCellState cellState,
        boolean exposedWallpaper,
        int faceMask,
        LevelZeroColumnMaterial material) {

    /**
     * Retourne {@code true} si la colonne est traversable.
     *
     * @return {@code true} pour une colonne de couloir ou de grande piece
     */
    public boolean walkable() {
        return material.walkable();
    }
}
