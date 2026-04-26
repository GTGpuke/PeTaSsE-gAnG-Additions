package com.petassegang.addons.world.backrooms.level0.layout;

/**
 * Fenetre canonique des cellules logiques utiles a un chunk donne.
 *
 * <p>Le chunk est ecrit en blocs de 16x16, mais la generation legacy raisonne
 * d'abord en cellules 3x3. Cette fenetre decrit donc quelles cellules doivent
 * etre lues et evaluees pour reconstruire le chunk.
 */
public record LevelZeroChunkCellWindow(
        int chunkX,
        int chunkZ,
        int minCellX,
        int minCellZ,
        int maxCellX,
        int maxCellZ) {

    /**
     * Retourne la largeur de la fenetre en cellules.
     *
     * @return largeur en cellules
     */
    public int width() {
        return maxCellX - minCellX + 1;
    }

    /**
     * Retourne la hauteur de la fenetre en cellules.
     *
     * @return hauteur en cellules
     */
    public int height() {
        return maxCellZ - minCellZ + 1;
    }
}
