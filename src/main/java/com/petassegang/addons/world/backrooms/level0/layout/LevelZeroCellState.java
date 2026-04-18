package com.petassegang.addons.world.backrooms.level0.layout;

import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;

/**
 * Etat semantique minimal d'une cellule locale du Level 0.
 */
public record LevelZeroCellState(
        LevelZeroCellTag tag,
        LevelZeroCellTopology topology,
        int geometryMask,
        int microPattern,
        int subCellX,
        int subCellZ,
        LevelZeroSurfaceBiome surfaceBiome,
        boolean largeRoom,
        boolean lighted) {

    /**
     * Retourne {@code true} si cette cellule est traversable.
     *
     * @return {@code true} pour tous les tags non mur
     */
    public boolean walkable() {
        return tag != LevelZeroCellTag.WALL;
    }

    /**
     * Indique si une feature geometrique fine est active sur cette cellule.
     *
     * @param feature feature recherchee
     * @return {@code true} si la feature est active
     */
    public boolean hasGeometryFeature(LevelZeroGeometryFeature feature) {
        return LevelZeroGeometryMask.has(geometryMask, feature);
    }

    /**
     * Indique si le bloc courant reste ouvert dans le motif micro-geometrique
     * de sa cellule 3x3.
     *
     * @return {@code true} si le bloc local est ouvert
     */
    public boolean isMicroOpen() {
        return LevelZeroCellMicroPattern.isOpen(microPattern, subCellX, subCellZ);
    }

    /**
     * Indique si le bloc local courant doit rester traversable apres
     * application de la micro-geometrie.
     *
     * @return {@code true} si la cellule est traversable et si son motif local
     *         laisse ce bloc ouvert
     */
    public boolean isLocallyWalkable() {
        return walkable() && isMicroOpen();
    }
}
