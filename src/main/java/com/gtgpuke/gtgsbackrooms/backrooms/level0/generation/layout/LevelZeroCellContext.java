package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout;

/**
 * Identite d'une cellule logique a evaluer : position, seed du layer et index du layer.
 *
 * @param cellX coordonnee logique X
 * @param cellZ coordonnee logique Z
 * @param layoutSeed seed deterministe du layer
 * @param layerIndex index du layer dans la pile verticale
 */
public record LevelZeroCellContext(int cellX, int cellZ, long layoutSeed, int layerIndex) {

    /**
     * Contexte d'une cellule voisine, meme seed et meme layer.
     *
     * @param offsetX decalage en cellules sur X
     * @param offsetZ decalage en cellules sur Z
     * @return contexte du voisin
     */
    public LevelZeroCellContext offset(int offsetX, int offsetZ) {
        return new LevelZeroCellContext(cellX + offsetX, cellZ + offsetZ, layoutSeed, layerIndex);
    }
}
