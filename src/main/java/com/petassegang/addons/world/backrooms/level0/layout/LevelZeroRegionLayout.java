package com.petassegang.addons.world.backrooms.level0.layout;

import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroCoords;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellEvaluation;

/**
 * Representation canonique d'une region finie de cellules logiques.
 *
 * <p>Cette premiere version couvre seulement la fenetre de cellules necessaire
 * a l'extraction d'un chunk, mais elle formalise deja le passage
 * "evaluation regionale -> extraction locale".
 */
public final class LevelZeroRegionLayout {

    private final int minCellX;
    private final int minCellZ;
    private final int maxCellX;
    private final int maxCellZ;
    private final int width;
    private final LevelZeroCellEvaluation[] evaluations;

    /**
     * Construit une region finie de cellules evaluees.
     *
     * @param minCellX borne minimale X incluse
     * @param minCellZ borne minimale Z incluse
     * @param maxCellX borne maximale X incluse
     * @param maxCellZ borne maximale Z incluse
     * @param evaluations evaluations ligne-major de la region
     */
    public LevelZeroRegionLayout(int minCellX,
                                 int minCellZ,
                                 int maxCellX,
                                 int maxCellZ,
                                 LevelZeroCellEvaluation[] evaluations) {
        this.minCellX = minCellX;
        this.minCellZ = minCellZ;
        this.maxCellX = maxCellX;
        this.maxCellZ = maxCellZ;
        this.width = maxCellX - minCellX + 1;
        this.evaluations = evaluations;
    }

    /**
     * Retourne l'evaluation agregee d'une cellule de la region.
     *
     * @param cellX coordonnee cellule X
     * @param cellZ coordonnee cellule Z
     * @return evaluation agregee de la cellule
     */
    public LevelZeroCellEvaluation sampleCell(int cellX, int cellZ) {
        if (cellX < minCellX || cellX > maxCellX || cellZ < minCellZ || cellZ > maxCellZ) {
            throw new IllegalArgumentException("Cellule hors de la region layout.");
        }
        return evaluations[index(cellX, cellZ)];
    }

    /**
     * Extrait un slice de chunk depuis cette region canonique.
     *
     * @param chunkX coordonnee chunk X
     * @param chunkZ coordonnee chunk Z
     * @return slice du chunk extrait depuis la region
     */
    public LevelZeroChunkSlice extractChunk(int chunkX, int chunkZ) {
        int worldMinX = LevelZeroCoords.chunkStartX(chunkX);
        int worldMinZ = LevelZeroCoords.chunkStartZ(chunkZ);
        int worldMaxX = LevelZeroCoords.chunkEndX(chunkX);
        int worldMaxZ = LevelZeroCoords.chunkEndZ(chunkZ);
        LevelZeroChunkSlice chunkSlice = new LevelZeroChunkSlice(worldMinX, worldMinZ, worldMaxX, worldMaxZ);

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            int cellWorldMinX = LevelZeroCoords.cellToWorldMinX(cellX);
            int startLocalX = Math.max(cellWorldMinX - worldMinX, 0);
            int endLocalX = Math.min(cellWorldMinX + LevelZeroCoords.cellScale() - 1 - worldMinX,
                    LevelZeroLayout.CHUNK_SIZE - 1);

            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                int cellWorldMinZ = LevelZeroCoords.cellToWorldMinZ(cellZ);
                int startLocalZ = Math.max(cellWorldMinZ - worldMinZ, 0);
                int endLocalZ = Math.min(cellWorldMinZ + LevelZeroCoords.cellScale() - 1 - worldMinZ,
                        LevelZeroLayout.CHUNK_SIZE - 1);
                LevelZeroCellEvaluation evaluation = sampleCell(cellX, cellZ);

                chunkSlice.fillCellRange(startLocalX, endLocalX, startLocalZ, endLocalZ, evaluation);
                chunkSlice.markLight(evaluation);
            }
        }

        return chunkSlice;
    }

    /**
     * Retourne la borne minimale X incluse de la region.
     *
     * @return borne minimale X
     */
    public int minCellX() {
        return minCellX;
    }

    /**
     * Retourne la borne minimale Z incluse de la region.
     *
     * @return borne minimale Z
     */
    public int minCellZ() {
        return minCellZ;
    }

    /**
     * Retourne la borne maximale X incluse de la region.
     *
     * @return borne maximale X
     */
    public int maxCellX() {
        return maxCellX;
    }

    /**
     * Retourne la borne maximale Z incluse de la region.
     *
     * @return borne maximale Z
     */
    public int maxCellZ() {
        return maxCellZ;
    }

    private int index(int cellX, int cellZ) {
        return (cellZ - minCellZ) * width + (cellX - minCellX);
    }
}
