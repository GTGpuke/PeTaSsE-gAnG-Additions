package com.petassegang.addons.world.backrooms.level0.layout;

/**
 * Masques bloc-par-bloc a l'interieur d'une cellule logique 3x3.
 *
 * <p>Un bit a 1 signifie "ouvert / traversable" pour le bloc correspondant
 * dans la cellule. L'ordre est ligne-major : `(z * 3 + x)`.
 *
 * <p>Ce fichier decrit donc la micro-geometrie locale, pas la topologie
 * globale du labyrinthe.
 */
public final class LevelZeroCellMicroPattern {

    /** Cellule pleine fermee. */
    public static final int FULL_CLOSED = 0;
    /** Cellule pleine ouverte. */
    public static final int FULL_OPEN = 0b111_111_111;
    /** Passage vertical d'un bloc de large. */
    public static final int PINCH_VERTICAL = 0b010_010_010;
    /** Passage horizontal d'un bloc de large. */
    public static final int PINCH_HORIZONTAL = 0b000_111_000;
    /** Renfoncement nord. */
    public static final int RECESS_NORTH = 0b000_111_111;
    /** Renfoncement sud. */
    public static final int RECESS_SOUTH = 0b111_111_000;
    /** Renfoncement ouest. */
    public static final int RECESS_WEST = 0b011_011_011;
    /** Renfoncement est. */
    public static final int RECESS_EAST = 0b110_110_110;
    /** Alcove nord. */
    public static final int ALCOVE_NORTH = 0b010_111_111;
    /** Alcove sud. */
    public static final int ALCOVE_SOUTH = 0b111_111_010;
    /** Alcove ouest. */
    public static final int ALCOVE_WEST = 0b011_111_011;
    /** Alcove est. */
    public static final int ALCOVE_EAST = 0b110_111_110;
    /** Decalage leger du mur vers l'est. */
    public static final int OFFSET_EAST = 0b110_111_110;
    /** Decalage leger du mur vers l'ouest. */
    public static final int OFFSET_WEST = 0b011_111_011;
    /** Demi-mur horizontal. */
    public static final int HALF_WALL_HORIZONTAL = 0b111_000_111;
    /** Demi-mur vertical. */
    public static final int HALF_WALL_VERTICAL = 0b101_101_101;

    private LevelZeroCellMicroPattern() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Retourne le bit local associe a un bloc de la cellule 3x3.
     *
     * @param subCellX offset local X dans la cellule [0..2]
     * @param subCellZ offset local Z dans la cellule [0..2]
     * @return masque du bloc local
     */
    public static int bit(int subCellX, int subCellZ) {
        return 1 << (subCellZ * 3 + subCellX);
    }

    /**
     * Ouvre explicitement un bloc local dans le motif.
     *
     * @param pattern motif courant
     * @param subCellX offset local X dans la cellule [0..2]
     * @param subCellZ offset local Z dans la cellule [0..2]
     * @return motif enrichi
     */
    public static int open(int pattern, int subCellX, int subCellZ) {
        return pattern | bit(subCellX, subCellZ);
    }

    /**
     * Ferme explicitement un bloc local dans le motif.
     *
     * @param pattern motif courant
     * @param subCellX offset local X dans la cellule [0..2]
     * @param subCellZ offset local Z dans la cellule [0..2]
     * @return motif filtre
     */
    public static int close(int pattern, int subCellX, int subCellZ) {
        return pattern & ~bit(subCellX, subCellZ);
    }

    /**
     * Compte le nombre de blocs ouverts dans le motif.
     *
     * @param pattern motif 3x3
     * @return nombre de bits ouverts
     */
    public static int openCount(int pattern) {
        return Integer.bitCount(pattern & FULL_OPEN);
    }

    /**
     * Indique si le bloc local de la cellule est ouvert dans ce motif.
     *
     * @param pattern masque 3x3
     * @param subCellX offset local X dans la cellule [0..2]
     * @param subCellZ offset local Z dans la cellule [0..2]
     * @return {@code true} si le bloc est ouvert
     */
    public static boolean isOpen(int pattern, int subCellX, int subCellZ) {
        int bit = subCellZ * 3 + subCellX;
        return ((pattern >> bit) & 1) != 0;
    }
}
