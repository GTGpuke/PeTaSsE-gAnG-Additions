package com.petassegang.addons.world.backrooms.level0.stage;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellMicroPattern;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;

/**
 * Etape legacy de projection bloc-par-bloc des anomalies geometriques.
 */
public final class LevelZeroLegacyMicroPatternStage {

    private static final int[][] NORTH_ROW = new int[][]{{0, 0}, {1, 0}, {2, 0}};
    private static final int[][] SOUTH_ROW = new int[][]{{0, 2}, {1, 2}, {2, 2}};
    private static final int[][] WEST_COLUMN = new int[][]{{0, 0}, {0, 1}, {0, 2}};
    private static final int[][] EAST_COLUMN = new int[][]{{2, 0}, {2, 1}, {2, 2}};
    private static final int[][] CENTER_ROW = new int[][]{{0, 1}, {1, 1}, {2, 1}};
    private static final int[][] CENTER_COLUMN = new int[][]{{1, 0}, {1, 1}, {1, 2}};

    /**
     * Projette les features geometriques rares sur un motif local 3x3.
     *
     * @param context contexte canonique de cellule
     * @param topology topologie semantique fine
     * @param geometryMask masque de features geometriques
     * @return motif 3x3 bloc-par-bloc
     */
    public int sample(LevelZeroCellContext context, LevelZeroCellTopology topology, int geometryMask) {
        if (topology == LevelZeroCellTopology.WALL) {
            return LevelZeroCellMicroPattern.FULL_CLOSED;
        }

        int pattern = LevelZeroCellMicroPattern.FULL_OPEN;
        if (geometryMask == LevelZeroGeometryMask.none()) {
            return pattern;
        }

        if (LevelZeroGeometryMask.has(geometryMask, LevelZeroGeometryFeature.PINCH_1WIDE)) {
            return samplePinchPattern(context);
        }
        if (LevelZeroGeometryMask.has(geometryMask, LevelZeroGeometryFeature.ALCOVE)) {
            pattern = applyAlcove(context, pattern);
        }
        if (LevelZeroGeometryMask.has(geometryMask, LevelZeroGeometryFeature.RECESS)) {
            pattern = applyRecess(context, pattern);
        }
        if (LevelZeroGeometryMask.has(geometryMask, LevelZeroGeometryFeature.HALF_WALL)) {
            pattern = applyHalfWall(context, pattern);
        }
        if (LevelZeroGeometryMask.has(geometryMask, LevelZeroGeometryFeature.OFFSET_WALL)) {
            pattern = applyOffsetWall(context, pattern);
        }
        return pattern;
    }

    private int samplePinchPattern(LevelZeroCellContext context) {
        boolean vertical = (blockHash(context, 1, 1) & 1L) == 0L;
        int pattern = LevelZeroCellMicroPattern.FULL_CLOSED;
        int[][] line = vertical ? CENTER_COLUMN : CENTER_ROW;
        for (int[] coords : line) {
            pattern = LevelZeroCellMicroPattern.open(pattern, coords[0], coords[1]);
        }
        return pattern;
    }

    private int applyAlcove(LevelZeroCellContext context, int pattern) {
        return switch (pickSide(context)) {
            case 0 -> closeSpecific(pattern, new int[][]{{0, 0}, {2, 0}});
            case 1 -> closeSpecific(pattern, new int[][]{{2, 0}, {2, 2}});
            case 2 -> closeSpecific(pattern, new int[][]{{0, 2}, {2, 2}});
            default -> closeSpecific(pattern, new int[][]{{0, 0}, {0, 2}});
        };
    }

    private int applyRecess(LevelZeroCellContext context, int pattern) {
        return switch (pickSide(context)) {
            case 0 -> closeNoisyEdge(context, pattern, NORTH_ROW, 1, true);
            case 1 -> closeNoisyEdge(context, pattern, EAST_COLUMN, 1, true);
            case 2 -> closeNoisyEdge(context, pattern, SOUTH_ROW, 1, true);
            default -> closeNoisyEdge(context, pattern, WEST_COLUMN, 1, true);
        };
    }

    private int applyHalfWall(LevelZeroCellContext context, int pattern) {
        boolean vertical = (blockHash(context, 1, 1) & 1L) == 0L;
        int[][] line = vertical ? CENTER_COLUMN : CENTER_ROW;
        int pivot = Math.floorMod(blockHash(context, line[1][0], line[1][1]), 2);
        pattern = LevelZeroCellMicroPattern.close(pattern, line[1][0], line[1][1]);
        pattern = LevelZeroCellMicroPattern.close(pattern, line[pivot == 0 ? 0 : 2][0], line[pivot == 0 ? 0 : 2][1]);
        return pattern;
    }

    private int applyOffsetWall(LevelZeroCellContext context, int pattern) {
        int side = pickSide(context);
        int[][] edge = switch (side) {
            case 0 -> NORTH_ROW;
            case 1 -> EAST_COLUMN;
            case 2 -> SOUTH_ROW;
            default -> WEST_COLUMN;
        };
        return closeNoisyEdge(context, pattern, edge, 1, false);
    }

    private int closeNoisyEdge(LevelZeroCellContext context,
                               int pattern,
                               int[][] edge,
                               int minimumClosed,
                               boolean preserveCenter) {
        int closed = 0;
        for (int[] coords : edge) {
            if (preserveCenter && coords[0] == 1 && coords[1] == 1) {
                continue;
            }
            if ((blockHash(context, coords[0], coords[1]) & 1L) == 0L) {
                pattern = LevelZeroCellMicroPattern.close(pattern, coords[0], coords[1]);
                closed++;
            }
        }
        if (closed >= minimumClosed) {
            return pattern;
        }

        int preferredIndex = 0;
        long best = Long.MIN_VALUE;
        for (int i = 0; i < edge.length; i++) {
            int[] coords = edge[i];
            if (preserveCenter && coords[0] == 1 && coords[1] == 1) {
                continue;
            }
            long score = blockHash(context, coords[0], coords[1]);
            if (score > best) {
                best = score;
                preferredIndex = i;
            }
        }
        return LevelZeroCellMicroPattern.close(pattern, edge[preferredIndex][0], edge[preferredIndex][1]);
    }

    private int closeSpecific(int pattern, int[][] blocks) {
        for (int[] coords : blocks) {
            pattern = LevelZeroCellMicroPattern.close(pattern, coords[0], coords[1]);
        }
        return pattern;
    }

    private int pickSide(LevelZeroCellContext context) {
        int side = 0;
        long best = Long.MIN_VALUE;
        int[][] probes = new int[][]{{1, 0}, {2, 1}, {1, 2}, {0, 1}};
        for (int i = 0; i < probes.length; i++) {
            long score = blockHash(context, probes[i][0], probes[i][1]);
            if (score > best) {
                best = score;
                side = i;
            }
        }
        return side;
    }

    private long blockHash(LevelZeroCellContext context, int subCellX, int subCellZ) {
        int blockX = context.cellX() * 3 + subCellX;
        int blockZ = context.cellZ() * 3 + subCellZ;
        return StageRandom.mixLegacy(
                context.layoutSeed(),
                StageRandom.Stage.NOISE_GEOMETRY,
                blockX,
                blockZ);
    }
}
