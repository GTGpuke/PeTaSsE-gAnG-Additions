package com.petassegang.addons.backrooms.level.level0.generation.write;

import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellConnections;

/**
 * Variante connectee derivee d'un masque cardinal pour les details surfaciques.
 *
 * <p>Cette normalisation reste utile meme si les `surface details` sont
 * actuellement geles, car elle sert deja de base commune pour les variantes
 * connectees d'autres couches comme les plinthes.
 */
public record LevelZeroConnectedDetailVariant(
        Shape shape,
        int rotationQuarterTurns) {

    /**
     * Variante isolee sans connexion.
     */
    public static final LevelZeroConnectedDetailVariant SINGLE =
            new LevelZeroConnectedDetailVariant(Shape.SINGLE, 0);

    /**
     * Derive une variante connectee a partir d'un masque cardinal.
     *
     * @param connectionMask masque NORTH/EAST/SOUTH/WEST
     * @return variante normalisee pour les futures textures connectees
     */
    public static LevelZeroConnectedDetailVariant fromConnectionMask(int connectionMask) {
        int mask = connectionMask
                & (LevelZeroCellConnections.NORTH
                | LevelZeroCellConnections.EAST
                | LevelZeroCellConnections.SOUTH
                | LevelZeroCellConnections.WEST);
        int degree = LevelZeroCellConnections.count(mask);
        if (degree == 0) {
            return SINGLE;
        }
        if (degree == 1) {
            return new LevelZeroConnectedDetailVariant(Shape.END, directionRotation(mask));
        }
        if (degree == 2) {
            if (isStraight(mask)) {
                return new LevelZeroConnectedDetailVariant(
                        Shape.STRAIGHT,
                        LevelZeroCellConnections.has(mask, LevelZeroCellConnections.NORTH) ? 0 : 1);
            }
            return new LevelZeroConnectedDetailVariant(Shape.CORNER, cornerRotation(mask));
        }
        if (degree == 3) {
            return new LevelZeroConnectedDetailVariant(Shape.TEE, teeRotation(mask));
        }
        return new LevelZeroConnectedDetailVariant(Shape.CROSS, 0);
    }

    /**
     * Formes connectees supportees pour les details surfaciques.
     */
    public enum Shape {
        SINGLE,
        END,
        STRAIGHT,
        CORNER,
        TEE,
        CROSS
    }

    private static boolean isStraight(int mask) {
        return LevelZeroCellConnections.has(mask, LevelZeroCellConnections.NORTH)
                && LevelZeroCellConnections.has(mask, LevelZeroCellConnections.SOUTH)
                || LevelZeroCellConnections.has(mask, LevelZeroCellConnections.EAST)
                && LevelZeroCellConnections.has(mask, LevelZeroCellConnections.WEST);
    }

    private static int directionRotation(int mask) {
        if (LevelZeroCellConnections.has(mask, LevelZeroCellConnections.NORTH)) {
            return 0;
        }
        if (LevelZeroCellConnections.has(mask, LevelZeroCellConnections.EAST)) {
            return 1;
        }
        if (LevelZeroCellConnections.has(mask, LevelZeroCellConnections.SOUTH)) {
            return 2;
        }
        return 3;
    }

    private static int cornerRotation(int mask) {
        if (LevelZeroCellConnections.has(mask, LevelZeroCellConnections.NORTH)
                && LevelZeroCellConnections.has(mask, LevelZeroCellConnections.EAST)) {
            return 0;
        }
        if (LevelZeroCellConnections.has(mask, LevelZeroCellConnections.EAST)
                && LevelZeroCellConnections.has(mask, LevelZeroCellConnections.SOUTH)) {
            return 1;
        }
        if (LevelZeroCellConnections.has(mask, LevelZeroCellConnections.SOUTH)
                && LevelZeroCellConnections.has(mask, LevelZeroCellConnections.WEST)) {
            return 2;
        }
        return 3;
    }

    private static int teeRotation(int mask) {
        if (!LevelZeroCellConnections.has(mask, LevelZeroCellConnections.SOUTH)) {
            return 0;
        }
        if (!LevelZeroCellConnections.has(mask, LevelZeroCellConnections.WEST)) {
            return 1;
        }
        if (!LevelZeroCellConnections.has(mask, LevelZeroCellConnections.NORTH)) {
            return 2;
        }
        return 3;
    }
}
