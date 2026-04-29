package com.petassegang.addons.backrooms.level.level0.generation.layout;

import com.petassegang.addons.backrooms.level.level0.biome.LevelZeroSurfaceBiome;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorRoomKind;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroCellEvaluation;

/**
 * Representation locale d'un chunk, prete pour la resolution de colonnes.
 *
 * <p>Le {@code RegionLayout} raisonne encore cellule par cellule. Le
 * {@code ChunkSlice}, lui, aplatit deja cette logique sur la grille locale du
 * chunk afin que la suite de la pipeline puisse travailler colonne par colonne.
 *
 * <p>En pratique, cette structure est le pont entre :
 * region evaluee -> chunk local -> colonnes resolues -> blocs poses.
 */
public final class LevelZeroChunkSlice {

    // Etat local bloc-par-bloc derive d'une evaluation logique par cellule.
    // Une meme cellule 3x3 legacy peut donc remplir plusieurs colonnes locales.
    private final boolean[] walkable;
    private final boolean[] lighted;
    private final LevelZeroSurfaceBiome[] surfaceBiomes;
    private final boolean[] largeRoom;
    private final LevelZeroCellTag[] cellTags;
    private final LevelZeroCellTopology[] cellTopologies;
    private final int[] connectionMasks;
    private final int[] geometryMasks;
    private final int[] microPatterns;
    private final LevelZeroSectorRoomKind[] roomKinds;
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
        this.connectionMasks = new int[LevelZeroLayout.CHUNK_SIZE * LevelZeroLayout.CHUNK_SIZE];
        this.geometryMasks = new int[LevelZeroLayout.CHUNK_SIZE * LevelZeroLayout.CHUNK_SIZE];
        this.microPatterns = new int[LevelZeroLayout.CHUNK_SIZE * LevelZeroLayout.CHUNK_SIZE];
        this.roomKinds = new LevelZeroSectorRoomKind[LevelZeroLayout.CHUNK_SIZE * LevelZeroLayout.CHUNK_SIZE];
        this.worldMinX = worldMinX;
        this.worldMinZ = worldMinZ;
        this.worldMaxX = worldMaxX;
        this.worldMaxZ = worldMaxZ;
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
        // Cette copie "aplatie" l'evaluation logique sur toutes les colonnes
        // locales couvertes par la cellule 3x3 afin que le writer n'ait plus
        // qu'a raisonner bloc par bloc.
        for (int localX = startLocalX; localX <= endLocalX; localX++) {
            for (int localZ = startLocalZ; localZ <= endLocalZ; localZ++) {
                int index = index(localX, localZ);
                walkable[index] = evaluation.walkable();
                surfaceBiomes[index] = evaluation.surfaceBiome();
                largeRoom[index] = evaluation.largeRoom();
                cellTags[index] = evaluation.cellTag();
                cellTopologies[index] = evaluation.topology();
                connectionMasks[index] = evaluation.connectionMask();
                geometryMasks[index] = evaluation.geometryMask();
                roomKinds[index] = evaluation.roomKind();
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
        // La lumiere reste volontairement marquee uniquement au centre logique
        // de la cellule 3x3 : le chunk slice ne stocke pas un motif lumineux,
        // seulement le point d'ancrage plafond qui sera ecrit plus tard.
        markLightAtWorldCenter(
                com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroCoords.cellCenterWorldX(evaluation.context().cellX()),
                com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroCoords.cellCenterWorldZ(evaluation.context().cellZ()));
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
        int subCellX = Math.floorMod(worldMinX + localX, com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroCoords.cellScale());
        int subCellZ = Math.floorMod(worldMinZ + localZ, com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroCoords.cellScale());
        // subCellX / subCellZ conservent la position intra-cellule 3x3 pour les
        // motifs fins, meme quand plusieurs colonnes locales partagent le meme
        // etat logique global.
        return new LevelZeroCellState(
                cellTags[index],
                cellTopologies[index],
                connectionMasks[index],
                geometryMasks[index],
                microPatterns[index],
                subCellX,
                subCellZ,
                surfaceBiomes[index],
                roomKinds[index],
                largeRoom[index],
                lighted[index]);
    }

    private static int index(int localX, int localZ) {
        return localZ * LevelZeroLayout.CHUNK_SIZE + localX;
    }
}
