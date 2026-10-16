package com.petassegang.addons.backrooms.level0.generation.coord;

import com.petassegang.addons.backrooms.BackroomsConstants;

/**
 * Conversions canoniques entre coordonnees monde, chunk et cellule logique.
 *
 * <p>Une cellule logique couvre {@value BackroomsConstants#LEVEL_ZERO_CELL_SCALE}x
 * {@value BackroomsConstants#LEVEL_ZERO_CELL_SCALE} blocs ; un chunk couvre
 * {@value #CHUNK_SIZE}x{@value #CHUNK_SIZE} blocs. Toute conversion spatiale du
 * Level 0 doit passer par ici.
 */
public final class LevelZeroCoords {

    /** Taille d'un chunk en blocs. */
    public static final int CHUNK_SIZE = 16;

    /** Taille d'une cellule logique en blocs. */
    public static final int CELL_SCALE = BackroomsConstants.LEVEL_ZERO_CELL_SCALE;

    private LevelZeroCoords() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Coordonnee monde minimale d'un chunk (valable pour X et Z). */
    public static int chunkStart(int chunkCoord) {
        return chunkCoord * CHUNK_SIZE;
    }

    /** Coordonnee monde maximale incluse d'un chunk (valable pour X et Z). */
    public static int chunkEnd(int chunkCoord) {
        return chunkStart(chunkCoord) + CHUNK_SIZE - 1;
    }

    /** Coordonnee de cellule logique contenant une coordonnee monde. */
    public static int worldToCell(int worldCoord) {
        return Math.floorDiv(worldCoord, CELL_SCALE);
    }

    /** Coordonnee monde minimale d'une cellule logique. */
    public static int cellToWorldMin(int cellCoord) {
        return cellCoord * CELL_SCALE;
    }

    /** Coordonnee monde du bloc central d'une cellule logique. */
    public static int cellCenterWorld(int cellCoord) {
        return cellToWorldMin(cellCoord) + CELL_SCALE / 2;
    }

    /** Position d'un bloc a l'interieur de sa cellule logique, dans [0, CELL_SCALE[. */
    public static int subCell(int worldCoord) {
        return Math.floorMod(worldCoord, CELL_SCALE);
    }

    /** Coordonnee locale d'un bloc dans son chunk, dans [0, CHUNK_SIZE[. */
    public static int worldToLocal(int worldCoord) {
        return Math.floorMod(worldCoord, CHUNK_SIZE);
    }
}
