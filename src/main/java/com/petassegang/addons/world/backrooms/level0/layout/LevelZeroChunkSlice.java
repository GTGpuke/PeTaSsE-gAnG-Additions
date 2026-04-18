package com.petassegang.addons.world.backrooms.level0.layout;

import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;
import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellEvaluation;

/**
 * Copie mutable des donnees de layout d'un chunk du Level 0.
 *
 * <p>Cette structure reste volontairement simple et compatible avec
 * l'implementation historique. Elle prepare le terrain pour un futur
 * decoupage region -> chunk plus explicite.
 */
public final class LevelZeroChunkSlice {

    private final boolean[] walkable;
    private final boolean[] lighted;
    private final LevelZeroSurfaceBiome[] surfaceBiomes;
    private final boolean[] largeRoom;
    private final LevelZeroCellTag[] cellTags;
    private final LevelZeroCellTopology[] cellTopologies;
    private final int[] geometryMasks;
    private final int[] microPatterns;
    private final int worldMinX;
    private final int worldMinZ;
    private final int worldMaxX;
    private final int worldMaxZ;

    /**
     * Construit un slice vide de chunk.
     *
     * @param worldMinX coordonnee monde X minimale du chunk
     * @param worldMinZ coordonnee monde Z minimale du chunk
     * @param worldMaxX coordonnee monde X maximale du chunk
     * @param worldMaxZ coordonnee monde Z maximale du chunk
     */
    public LevelZeroChunkSlice(int worldMinX, int worldMinZ, int worldMaxX, int worldMaxZ) {
        this.walkable = new boolean[LevelZeroLayout.CHUNK_SIZE * LevelZeroLayout.CHUNK_SIZE];
        this.lighted = new boolean[LevelZeroLayout.CHUNK_SIZE * LevelZeroLayout.CHUNK_SIZE];
        this.surfaceBiomes = new LevelZeroSurfaceBiome[LevelZeroLayout.CHUNK_SIZE * LevelZeroLayout.CHUNK_SIZE];
        this.largeRoom = new boolean[LevelZeroLayout.CHUNK_SIZE * LevelZeroLayout.CHUNK_SIZE];
        this.cellTags = new LevelZeroCellTag[LevelZeroLayout.CHUNK_SIZE * LevelZeroLayout.CHUNK_SIZE];
        this.cellTopologies = new LevelZeroCellTopology[LevelZeroLayout.CHUNK_SIZE * LevelZeroLayout.CHUNK_SIZE];
        this.geometryMasks = new int[LevelZeroLayout.CHUNK_SIZE * LevelZeroLayout.CHUNK_SIZE];
        this.microPatterns = new int[LevelZeroLayout.CHUNK_SIZE * LevelZeroLayout.CHUNK_SIZE];
        this.worldMinX = worldMinX;
        this.worldMinZ = worldMinZ;
        this.worldMaxX = worldMaxX;
        this.worldMaxZ = worldMaxZ;
    }

    /**
     * Remplit une plage locale du chunk avec les donnees d'une cellule logique.
     *
     * @param startLocalX debut local X inclus
     * @param endLocalX fin locale X incluse
     * @param startLocalZ debut locale Z incluse
     * @param endLocalZ fin locale Z incluse
     * @param cellWalkable etat traversable de la cellule
     * @param surfaceBiome biome cosmetique de surface
     * @param cellLargeRoom marquage grande piece
     */
    public void fillCellRange(int startLocalX,
                              int endLocalX,
                              int startLocalZ,
                              int endLocalZ,
                              boolean cellWalkable,
                              LevelZeroSurfaceBiome surfaceBiome,
                              boolean cellLargeRoom) {
        for (int localX = startLocalX; localX <= endLocalX; localX++) {
            for (int localZ = startLocalZ; localZ <= endLocalZ; localZ++) {
                int index = index(localX, localZ);
                walkable[index] = cellWalkable;
                surfaceBiomes[index] = surfaceBiome;
                largeRoom[index] = cellLargeRoom;
                cellTags[index] = resolveCellTag(cellWalkable, cellLargeRoom);
                cellTopologies[index] = cellLargeRoom
                        ? LevelZeroCellTopology.ROOM_LARGE
                        : (cellWalkable ? LevelZeroCellTopology.CORRIDOR : LevelZeroCellTopology.WALL);
                geometryMasks[index] = LevelZeroGeometryMask.none();
                microPatterns[index] = cellWalkable
                        ? LevelZeroCellMicroPattern.FULL_OPEN
                        : LevelZeroCellMicroPattern.FULL_CLOSED;
            }
        }
    }

    /**
     * Remplit une plage locale du chunk a partir d'une evaluation agregee de
     * cellule logique.
     *
     * @param startLocalX debut local X inclus
     * @param endLocalX fin locale X incluse
     * @param startLocalZ debut locale Z incluse
     * @param endLocalZ fin locale Z incluse
     * @param evaluation evaluation agregee de la cellule
     */
    public void fillCellRange(int startLocalX,
                              int endLocalX,
                              int startLocalZ,
                              int endLocalZ,
                              LevelZeroCellEvaluation evaluation) {
        for (int localX = startLocalX; localX <= endLocalX; localX++) {
            for (int localZ = startLocalZ; localZ <= endLocalZ; localZ++) {
                int index = index(localX, localZ);
                walkable[index] = evaluation.walkable();
                surfaceBiomes[index] = evaluation.surfaceBiome();
                largeRoom[index] = evaluation.largeRoom();
                cellTags[index] = evaluation.cellTag();
                cellTopologies[index] = evaluation.topology();
                geometryMasks[index] = evaluation.geometryMask();
                microPatterns[index] = evaluation.microPattern();
            }
        }
    }

    /**
     * Marque la position locale d'un neon au centre d'une cellule.
     *
     * @param centerWorldX centre monde X de la cellule
     * @param centerWorldZ centre monde Z de la cellule
     */
    public void markLightAtWorldCenter(int centerWorldX, int centerWorldZ) {
        if (centerWorldX < worldMinX || centerWorldX > worldMaxX || centerWorldZ < worldMinZ || centerWorldZ > worldMaxZ) {
            return;
        }

        int centerLocalX = centerWorldX - worldMinX;
        int centerLocalZ = centerWorldZ - worldMinZ;
        lighted[index(centerLocalX, centerLocalZ)] = true;
    }

    /**
     * Marque l'eventuelle lumiere d'une cellule evaluee a son centre monde.
     *
     * @param evaluation evaluation agregee de la cellule
     */
    public void markLight(LevelZeroCellEvaluation evaluation) {
        if (!evaluation.lighted()) {
            return;
        }
        markLightAtWorldCenter(
                com.petassegang.addons.world.backrooms.level0.coord.LevelZeroCoords.cellCenterWorldX(evaluation.context().cellX()),
                com.petassegang.addons.world.backrooms.level0.coord.LevelZeroCoords.cellCenterWorldZ(evaluation.context().cellZ()));
    }

    /**
     * Retourne les colonnes traversables.
     *
     * @return tableau walkable du chunk
     */
    public boolean[] walkable() {
        return walkable;
    }

    /**
     * Retourne les positions de neons.
     *
     * @return tableau lighted du chunk
     */
    public boolean[] lighted() {
        return lighted;
    }

    /**
     * Retourne les biomes de surface.
     *
     * @return tableau des biomes de surface
     */
    public LevelZeroSurfaceBiome[] surfaceBiomes() {
        return surfaceBiomes;
    }

    /**
     * Retourne le marquage de grande piece.
     *
     * @return tableau largeRoom du chunk
     */
    public boolean[] largeRoom() {
        return largeRoom;
    }

    /**
     * Retourne les tags semantiques minimaux du chunk.
     *
     * @return tableau des tags de cellules locales
     */
    public LevelZeroCellTag[] cellTags() {
        return cellTags;
    }

    /**
     * Retourne les topologies semantiques fines du chunk.
     *
     * @return tableau des topologies de cellules locales
     */
    public LevelZeroCellTopology[] cellTopologies() {
        return cellTopologies;
    }

    /**
     * Retourne l'etat semantique minimal d'une cellule locale.
     *
     * @param localX coordonnee locale X
     * @param localZ coordonnee locale Z
     * @return etat semantique courant de la cellule
     */
    public LevelZeroCellState cellState(int localX, int localZ) {
        int index = index(localX, localZ);
        int subCellX = Math.floorMod(worldMinX + localX, com.petassegang.addons.world.backrooms.level0.coord.LevelZeroCoords.cellScale());
        int subCellZ = Math.floorMod(worldMinZ + localZ, com.petassegang.addons.world.backrooms.level0.coord.LevelZeroCoords.cellScale());
        return new LevelZeroCellState(
                cellTags[index],
                cellTopologies[index],
                geometryMasks[index],
                microPatterns[index],
                subCellX,
                subCellZ,
                surfaceBiomes[index],
                largeRoom[index],
                lighted[index]);
    }

    private static int index(int localX, int localZ) {
        return localZ * LevelZeroLayout.CHUNK_SIZE + localX;
    }

    private static LevelZeroCellTag resolveCellTag(boolean cellWalkable, boolean cellLargeRoom) {
        if (!cellWalkable) {
            return LevelZeroCellTag.WALL;
        }
        return cellLargeRoom ? LevelZeroCellTag.ROOM_LARGE : LevelZeroCellTag.CORRIDOR;
    }
}
