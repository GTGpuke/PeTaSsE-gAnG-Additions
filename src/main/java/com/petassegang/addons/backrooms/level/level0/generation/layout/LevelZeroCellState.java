package com.petassegang.addons.backrooms.level.level0.generation.layout;

import com.petassegang.addons.backrooms.level.level0.biome.LevelZeroSurfaceBiome;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorRoomKind;

/**
 * Etat semantique complet d'une cellule locale apres evaluation.
 *
 * <p>Ce record rassemble tout ce dont le writer et les resolvers ont besoin a
 * l'echelle d'une colonne locale : tag, topologie, biome, motif, lumiere et
 * sous-position dans la cellule 3x3.
 */
public record LevelZeroCellState(
        LevelZeroCellTag tag,
        LevelZeroCellTopology topology,
        int connectionMask,
        int geometryMask,
        int microPattern,
        int subCellX,
        int subCellZ,
        LevelZeroSurfaceBiome surfaceBiome,
        LevelZeroSectorRoomKind roomKind,
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
     * Indique si la cellule est ouverte vers une direction cardinale.
     *
     * @param direction bit cardinal de {@link LevelZeroCellConnections}
     * @return {@code true} si la connexion est presente
     */
    public boolean hasConnection(int direction) {
        return LevelZeroCellConnections.has(connectionMask, direction);
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
