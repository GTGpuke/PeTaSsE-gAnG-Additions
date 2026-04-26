package com.petassegang.addons.world.backrooms.level0.layout;

import com.petassegang.addons.world.backrooms.BackroomsConstants;
import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;
import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;

/**
 * Echantillonneur local de layout pour retrouver rapidement les transitions
 * de papier peint adaptatif autour d'une colonne de mur.
 */
public final class LevelZeroLayoutSampler {

    public static final int NORTH_MASK = 1;
    public static final int SOUTH_MASK = 1 << 1;
    public static final int WEST_MASK = 1 << 2;
    public static final int EAST_MASK = 1 << 3;
    public static final int FULL_MASK = NORTH_MASK | SOUTH_MASK | WEST_MASK | EAST_MASK;

    private final long layoutSeed;
    private final int layerIndex;

    public LevelZeroLayoutSampler(long layoutSeed) {
        this(layoutSeed, 0);
    }

    public LevelZeroLayoutSampler(long layoutSeed, int layerIndex) {
        this.layoutSeed = layoutSeed;
        this.layerIndex = layerIndex;
    }

    /**
     * Retourne {@code true} si cette colonne de mur est exposee a l'air sur au
     * moins un cote horizontal.
     *
     * @param worldX coordonnee X monde
     * @param worldZ coordonnee Z monde
     * @return {@code true} si le mur est visible
     */
    public boolean isWallpaperExposed(int worldX, int worldZ) {
        return sampleExposedFaceMask(worldX, worldZ) != 0;
    }

    /**
     * Retourne le masque des faces horizontales exposees a l'air.
     *
     * @param worldX coordonnee X monde
     * @param worldZ coordonnee Z monde
     * @return bitmask N/S/W/E des faces visibles
     */
    public int sampleExposedFaceMask(int worldX, int worldZ) {
        int faceMask = 0;
        if (isWalkableAt(worldX, worldZ - 1)) {
            faceMask |= NORTH_MASK;
        }
        if (isWalkableAt(worldX, worldZ + 1)) {
            faceMask |= SOUTH_MASK;
        }
        if (isWalkableAt(worldX - 1, worldZ)) {
            faceMask |= WEST_MASK;
        }
        if (isWalkableAt(worldX + 1, worldZ)) {
            faceMask |= EAST_MASK;
        }
        return faceMask;
    }

    /**
     * Echantillonne le masque des faces qui doivent afficher la variante
     * alternative de papier peint.
     *
     * @param worldX coordonnee X monde
     * @param worldZ coordonnee Z monde
     * @return bitmask N/S/W/E des faces alternatives
     */
    public int sampleWallpaperFaceMask(int worldX, int worldZ) {
        int faceMask = 0;
        faceMask |= sampleFace(worldX, worldZ, 0, -1, NORTH_MASK);
        faceMask |= sampleFace(worldX, worldZ, 0, 1, SOUTH_MASK);
        faceMask |= sampleFace(worldX, worldZ, -1, 0, WEST_MASK);
        faceMask |= sampleFace(worldX, worldZ, 1, 0, EAST_MASK);
        return faceMask;
    }

    private int sampleFace(int worldX, int worldZ, int stepX, int stepZ, int maskBit) {
        for (int distance = 1; distance <= BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 4; distance++) {
            int sampleX = worldX + stepX * distance;
            int sampleZ = worldZ + stepZ * distance;
            if (!isWalkableAt(sampleX, sampleZ)) {
                continue;
            }
            return LevelZeroSurfaceBiome.sampleAtWorld(sampleX, sampleZ, layerIndex) == LevelZeroSurfaceBiome.RED
                    ? maskBit
                    : 0;
        }

        int fallbackX = worldX + stepX * BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 2;
        int fallbackZ = worldZ + stepZ * BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 2;
        return LevelZeroSurfaceBiome.sampleAtWorld(fallbackX, fallbackZ, layerIndex) == LevelZeroSurfaceBiome.RED
                ? maskBit
                : 0;
    }

    private boolean isWalkableAt(int worldX, int worldZ) {
        return LevelZeroLayout.isWalkableAtWorld(worldX, worldZ, layoutSeed);
    }
}
