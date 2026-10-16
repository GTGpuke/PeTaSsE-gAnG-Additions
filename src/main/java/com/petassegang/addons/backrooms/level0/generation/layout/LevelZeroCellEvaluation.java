package com.petassegang.addons.backrooms.level0.generation.layout;

import com.petassegang.addons.backrooms.level0.biome.LevelZeroSurfaceBiome;
import com.petassegang.addons.backrooms.level0.generation.sector.LevelZeroSectorRoomKind;

/**
 * Resultat complet de l'evaluation d'une cellule logique : tout ce que le writer
 * doit savoir pour poser les blocs des 3x3 colonnes de la cellule.
 *
 * @param context identite de la cellule
 * @param walkable {@code true} si la cellule est ouverte dans le labyrinthe
 * @param topology forme locale du passage
 * @param connectionMask sorties cardinales (bits de {@link LevelZeroCellConnections})
 * @param geometryMask anomalies geometriques (bits de {@link LevelZeroGeometryFeature})
 * @param microPattern ouverture bloc par bloc dans la cellule (bits de {@link LevelZeroCellMicroPattern})
 * @param surfaceBiome palette cosmetique
 * @param roomKind type de salle tamponnee par le secteur
 * @param largeRoom {@code true} si la cellule est marquee grande piece
 * @param lighted {@code true} si un neon est pose au centre du plafond
 */
public record LevelZeroCellEvaluation(
        LevelZeroCellContext context,
        boolean walkable,
        LevelZeroCellTopology topology,
        int connectionMask,
        int geometryMask,
        int microPattern,
        LevelZeroSurfaceBiome surfaceBiome,
        LevelZeroSectorRoomKind roomKind,
        boolean largeRoom,
        boolean lighted) {

    /**
     * {@code true} si le bloc de la cellule a la position locale donnee reste traversable
     * une fois la micro-geometrie appliquee.
     *
     * @param subCellX position du bloc dans la cellule [0..2]
     * @param subCellZ position du bloc dans la cellule [0..2]
     * @return {@code true} si le bloc est ouvert
     */
    public boolean isLocallyWalkable(int subCellX, int subCellZ) {
        return walkable && LevelZeroCellMicroPattern.isOpen(microPattern, subCellX, subCellZ);
    }

    /** {@code true} si une anomalie geometrique est active sur cette cellule. */
    public boolean hasGeometryFeature() {
        return geometryMask != 0;
    }
}
