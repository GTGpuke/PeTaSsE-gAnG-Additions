package com.petassegang.addons.backrooms.level0.generation.layout;

import com.petassegang.addons.backrooms.level0.biome.LevelZeroSurfaceBiome;
import com.petassegang.addons.backrooms.level0.generation.sector.LevelZeroSectorRoomKind;

/**
 * Coeur de l'evaluation d'une cellule : enchaine salle, grande piece, biome,
 * topologie, lumiere et micro-geometrie a partir de la traversabilite brute.
 *
 * <p>Toutes les etapes sont deterministes et sans effet de bord ; l'ordre est
 * fige car la lumiere interdit la micro-geometrie et la topologie depend du
 * marquage grande piece.
 */
public final class LevelZeroCellEvaluator {

    private final LevelZeroCorridorGaps corridorGaps;

    /**
     * Construit un evaluateur.
     *
     * @param noiseGeometryEnabled {@code false} pour desactiver les gaps de couloir
     */
    public LevelZeroCellEvaluator(boolean noiseGeometryEnabled) {
        this.corridorGaps = new LevelZeroCorridorGaps(noiseGeometryEnabled);
    }

    /**
     * Evalue une cellule.
     *
     * @param context cellule evaluee
     * @param walkability fenetre de traversabilite, avec marge autour de la cellule
     * @return evaluation complete
     */
    public LevelZeroCellEvaluation evaluate(LevelZeroCellContext context, LevelZeroRegionWalkability walkability) {
        int cellX = context.cellX();
        int cellZ = context.cellZ();
        boolean walkable = walkability.isWalkable(cellX, cellZ);
        LevelZeroSectorRoomKind roomKind = walkability.roomKind(cellX, cellZ);
        boolean largeRoom = LevelZeroLargeRooms.isLargeRoom(context);
        LevelZeroSurfaceBiome biome = LevelZeroSurfaceBiome.sampleAtCell(cellX, cellZ, context.layerIndex());
        int connectionMask = walkable && !largeRoom
                ? LevelZeroTopology.connectionMask(cellX, cellZ, walkability)
                : LevelZeroCellConnections.NONE;
        LevelZeroCellTopology topology = LevelZeroTopology.classify(walkable, largeRoom, connectionMask);
        boolean lighted = walkable
                && LevelZeroLightPattern.isLit(context, biome, topology == LevelZeroCellTopology.ROOM_LARGE);
        int geometryMask = lighted
                ? LevelZeroGeometryFeature.NONE
                : corridorGaps.sample(context, topology, connectionMask, walkability);
        int microPattern;
        if (topology == LevelZeroCellTopology.WALL) {
            microPattern = LevelZeroCellMicroPattern.FULL_CLOSED;
        } else if (geometryMask == LevelZeroGeometryFeature.NONE) {
            microPattern = LevelZeroCellMicroPattern.FULL_OPEN;
        } else {
            microPattern = LevelZeroCorridorGaps.microPattern(connectionMask, geometryMask);
        }
        return new LevelZeroCellEvaluation(
                context,
                walkable,
                topology,
                connectionMask,
                geometryMask,
                microPattern,
                biome,
                roomKind,
                largeRoom,
                lighted);
    }
}
