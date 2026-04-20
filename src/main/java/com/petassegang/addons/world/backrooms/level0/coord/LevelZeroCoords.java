package com.petassegang.addons.world.backrooms.level0.coord;

import com.petassegang.addons.world.backrooms.BackroomsConstants;
import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;

/**
 * Conversions canoniques entre monde, chunk et cellule logique.
 *
 * <p>Ce fichier doit rester la source de verite des conversions spatiales du
 * Level 0. Lorsqu'un doute apparait sur une coordonnee, c'est ici qu'il faut
 * regarder avant de reintroduire des calculs locaux dupliques.
 */
public final class LevelZeroCoords {

    private LevelZeroCoords() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Retourne l'echelle d'une cellule logique en blocs monde.
     *
     * @return taille d'une cellule en blocs
     */
    public static int cellScale() {
        return BackroomsConstants.LEVEL_ZERO_CELL_SCALE;
    }

    /**
     * Retourne le debut monde X d'un chunk.
     *
     * @param chunkX coordonnee X du chunk
     * @return coordonnee monde X minimale du chunk
     */
    public static int chunkStartX(int chunkX) {
        return chunkX * LevelZeroLayout.CHUNK_SIZE;
    }

    /**
     * Retourne le debut monde Z d'un chunk.
     *
     * @param chunkZ coordonnee Z du chunk
     * @return coordonnee monde Z minimale du chunk
     */
    public static int chunkStartZ(int chunkZ) {
        return chunkZ * LevelZeroLayout.CHUNK_SIZE;
    }

    /**
     * Retourne la fin monde X d'un chunk.
     *
     * @param chunkX coordonnee X du chunk
     * @return coordonnee monde X maximale du chunk
     */
    public static int chunkEndX(int chunkX) {
        return chunkStartX(chunkX) + LevelZeroLayout.CHUNK_SIZE - 1;
    }

    /**
     * Retourne la fin monde Z d'un chunk.
     *
     * @param chunkZ coordonnee Z du chunk
     * @return coordonnee monde Z maximale du chunk
     */
    public static int chunkEndZ(int chunkZ) {
        return chunkStartZ(chunkZ) + LevelZeroLayout.CHUNK_SIZE - 1;
    }

    /**
     * Convertit une coordonnee monde X en coordonnee cellule X.
     *
     * @param worldX coordonnee monde X
     * @return coordonnee cellule X
     */
    public static int worldToCellX(int worldX) {
        return Math.floorDiv(worldX, cellScale());
    }

    /**
     * Convertit une coordonnee monde Z en coordonnee cellule Z.
     *
     * @param worldZ coordonnee monde Z
     * @return coordonnee cellule Z
     */
    public static int worldToCellZ(int worldZ) {
        return Math.floorDiv(worldZ, cellScale());
    }

    /**
     * Convertit une coordonnee cellule X en origine monde X.
     *
     * @param cellX coordonnee cellule X
     * @return coordonnee monde X minimale de la cellule
     */
    public static int cellToWorldMinX(int cellX) {
        return cellX * cellScale();
    }

    /**
     * Convertit une coordonnee cellule Z en origine monde Z.
     *
     * @param cellZ coordonnee cellule Z
     * @return coordonnee monde Z minimale de la cellule
     */
    public static int cellToWorldMinZ(int cellZ) {
        return cellZ * cellScale();
    }

    /**
     * Retourne le centre monde X d'une cellule logique.
     *
     * @param cellX coordonnee cellule X
     * @return coordonnee monde X du centre
     */
    public static int cellCenterWorldX(int cellX) {
        return cellToWorldMinX(cellX) + cellScale() / 2;
    }

    /**
     * Retourne le centre monde Z d'une cellule logique.
     *
     * @param cellZ coordonnee cellule Z
     * @return coordonnee monde Z du centre
     */
    public static int cellCenterWorldZ(int cellZ) {
        return cellToWorldMinZ(cellZ) + cellScale() / 2;
    }

    /**
     * Convertit une coordonnee monde X en coordonnee locale de chunk.
     *
     * @param worldX coordonnee monde X
     * @return coordonnee locale X dans le chunk
     */
    public static int worldToLocalX(int worldX) {
        return Math.floorMod(worldX, LevelZeroLayout.CHUNK_SIZE);
    }

    /**
     * Convertit une coordonnee monde Z en coordonnee locale de chunk.
     *
     * @param worldZ coordonnee monde Z
     * @return coordonnee locale Z dans le chunk
     */
    public static int worldToLocalZ(int worldZ) {
        return Math.floorMod(worldZ, LevelZeroLayout.CHUNK_SIZE);
    }
}
