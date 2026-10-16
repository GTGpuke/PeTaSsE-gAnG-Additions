package com.petassegang.addons.backrooms.level0.generation.layout;

import com.petassegang.addons.backrooms.level0.biome.LevelZeroSurfaceBiome;

/**
 * Placement des neons : une trame globale en quinconce, filtree par le biome.
 *
 * <p>La trame allume une cellule sur deux sur les rangs 0 et 2 de chaque bloc
 * de quatre rangs, avec un decalage entre les deux. Deux neons ne peuvent donc
 * jamais etre adjacents, meme en diagonale. Le biome eclaircit ensuite la
 * trame (densite, regions sombres, blackout de grande piece).
 */
public final class LevelZeroLightPattern {

    private LevelZeroLightPattern() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * {@code true} si la cellule porte un neon.
     *
     * @param context cellule evaluee
     * @param biome biome cosmetique de la cellule
     * @param largeRoom {@code true} si la cellule est une grande piece
     * @return {@code true} si un neon est pose
     */
    public static boolean isLit(LevelZeroCellContext context, LevelZeroSurfaceBiome biome, boolean largeRoom) {
        int cellX = context.cellX();
        int cellZ = context.cellZ();
        long seed = context.layoutSeed();
        if (!isOnGlobalStripe(cellX, cellZ)) {
            return false;
        }
        if (biome.isFullDarkRegion(cellX, cellZ, seed)) {
            return false;
        }
        if (largeRoom && biome.isLargeRoomBlackout(cellX, cellZ, seed)) {
            return false;
        }
        return biome.keepsStripedLight(cellX, cellZ, seed);
    }

    /**
     * {@code true} si la cellule est sur la trame globale, independamment du biome.
     *
     * @param cellX coordonnee logique X
     * @param cellZ coordonnee logique Z
     * @return {@code true} si la cellule est candidate
     */
    public static boolean isOnGlobalStripe(int cellX, int cellZ) {
        return switch (Math.floorMod(cellZ, 4)) {
            case 0 -> Math.floorMod(cellX, 2) == 0;
            case 2 -> Math.floorMod(cellX, 2) != 0;
            default -> false;
        };
    }
}
