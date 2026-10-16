package com.petassegang.addons.backrooms.level0.generation.layout;

import com.petassegang.addons.backrooms.level0.generation.noise.StageRandom;

/**
 * Marquage deterministe des grandes pieces.
 *
 * <p>Environ 31 % des cellules sont candidates ; un arbitrage par score entre
 * voisins garantit que deux grandes pieces ne se touchent jamais, meme en
 * diagonale. Ce marquage ne creuse rien : il est relu par la topologie, la
 * lumiere et le rendu.
 */
public final class LevelZeroLargeRooms {

    private static final int CANDIDATE_MODULO = 16;
    private static final int CANDIDATE_THRESHOLD = 5;

    private LevelZeroLargeRooms() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * {@code true} si la cellule est une grande piece.
     *
     * @param context cellule evaluee
     * @return {@code true} si la cellule gagne son marquage grande piece
     */
    public static boolean isLargeRoom(LevelZeroCellContext context) {
        if (!isCandidate(context)) {
            return false;
        }
        long selfScore = score(context);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                if (offsetX == 0 && offsetZ == 0) {
                    continue;
                }
                LevelZeroCellContext neighbor = context.offset(offsetX, offsetZ);
                if (isCandidate(neighbor) && neighborWins(neighbor, context, selfScore)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isCandidate(LevelZeroCellContext context) {
        long hash = StageRandom.mix(
                context.layoutSeed(),
                StageRandom.Stage.LARGE_ROOMS,
                context.cellX(),
                context.cellZ());
        return Math.floorMod(hash, CANDIDATE_MODULO) < CANDIDATE_THRESHOLD;
    }

    private static long score(LevelZeroCellContext context) {
        return StageRandom.mix(
                context.layoutSeed(),
                StageRandom.Stage.LARGE_ROOMS,
                context.cellX() * 17 + context.layerIndex(),
                context.cellZ() * 31 - context.layerIndex());
    }

    private static boolean neighborWins(LevelZeroCellContext neighbor, LevelZeroCellContext context, long selfScore) {
        return LevelZeroTieBreak.neighborWins(selfScore, score(neighbor), context.cellX(), context.cellZ(), neighbor.cellX(), neighbor.cellZ());
    }
}
