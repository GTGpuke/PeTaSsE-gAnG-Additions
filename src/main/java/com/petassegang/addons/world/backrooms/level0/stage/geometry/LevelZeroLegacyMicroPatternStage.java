package com.petassegang.addons.world.backrooms.level0.stage.geometry;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellConnections;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellMicroPattern;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;

/**
 * Etape legacy de projection bloc-par-bloc des anomalies geometriques.
 *
 * <p>Cette couche ne decide pas quelles anomalies existent : elle traduit
 * seulement un geometryMask deja choisi en un motif local 3x3 au niveau bloc.
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
     * @param connectionMask masque des connexions cardinales
     * @param geometryMask masque de features geometriques
     * @return motif 3x3 bloc-par-bloc
     */
    public int sample(LevelZeroCellContext context, LevelZeroCellTopology topology, int connectionMask, int geometryMask) {
        if (topology == LevelZeroCellTopology.WALL) {
            return LevelZeroCellMicroPattern.FULL_CLOSED;
        }

        int pattern = LevelZeroCellMicroPattern.FULL_OPEN;
        if (geometryMask == LevelZeroGeometryMask.none()) {
            return pattern;
        }

        // L'ordre reste volontairement stable : les features les plus fortes
        // comme le pinch court-circuitent la suite, alors que les autres se
        // composent progressivement sur le motif 3x3.
        if (LevelZeroGeometryMask.has(geometryMask, LevelZeroGeometryFeature.PINCH_1WIDE)) {
            return samplePinchPattern(context, connectionMask);
        }
        if (LevelZeroGeometryMask.has(geometryMask, LevelZeroGeometryFeature.ALCOVE)) {
            pattern = applyAlcove(context, connectionMask, pattern);
        }
        if (LevelZeroGeometryMask.has(geometryMask, LevelZeroGeometryFeature.RECESS)) {
            pattern = applyRecess(context, topology, connectionMask, pattern);
        }
        if (LevelZeroGeometryMask.has(geometryMask, LevelZeroGeometryFeature.HALF_WALL)) {
            pattern = applyHalfWall(context, connectionMask, pattern);
        }
        if (LevelZeroGeometryMask.has(geometryMask, LevelZeroGeometryFeature.OFFSET_WALL)) {
            pattern = applyOffsetWall(context, topology, connectionMask, pattern);
        }
        return pattern;
    }

    private int samplePinchPattern(LevelZeroCellContext context, int connectionMask) {
        boolean vertical = corridorVertical(connectionMask);
        int pattern = LevelZeroCellMicroPattern.FULL_CLOSED;
        int[][] line = vertical ? CENTER_COLUMN : CENTER_ROW;
        for (int[] coords : line) {
            pattern = LevelZeroCellMicroPattern.open(pattern, coords[0], coords[1]);
        }
        return pattern;
    }

    private int applyAlcove(LevelZeroCellContext context, int connectionMask, int pattern) {
        int openSide = openSide(connectionMask);
        if (openSide >= 0) {
            return switch (oppositeOf(openSide)) {
                case 0 -> LevelZeroCellMicroPattern.ALCOVE_NORTH;
                case 1 -> LevelZeroCellMicroPattern.ALCOVE_EAST;
                case 2 -> LevelZeroCellMicroPattern.ALCOVE_SOUTH;
                default -> LevelZeroCellMicroPattern.ALCOVE_WEST;
            };
        }
        return switch (pickAlcoveSide(context, connectionMask)) {
            case 0 -> closeSpecific(pattern, new int[][]{{0, 0}, {2, 0}});
            case 1 -> closeSpecific(pattern, new int[][]{{2, 0}, {2, 2}});
            case 2 -> closeSpecific(pattern, new int[][]{{0, 2}, {2, 2}});
            default -> closeSpecific(pattern, new int[][]{{0, 0}, {0, 2}});
        };
    }

    private int applyRecess(LevelZeroCellContext context,
                            LevelZeroCellTopology topology,
                            int connectionMask,
                            int pattern) {
        if (topology == LevelZeroCellTopology.ANGLE) {
            return applyAngleRecess(context, connectionMask, pattern);
        }
        return switch (pickWallSide(context, topology, connectionMask)) {
            case 0 -> closeNoisyEdge(context, pattern, NORTH_ROW, 1, true);
            case 1 -> closeNoisyEdge(context, pattern, EAST_COLUMN, 1, true);
            case 2 -> closeNoisyEdge(context, pattern, SOUTH_ROW, 1, true);
            default -> closeNoisyEdge(context, pattern, WEST_COLUMN, 1, true);
        };
    }

    private int applyHalfWall(LevelZeroCellContext context, int connectionMask, int pattern) {
        boolean vertical = corridorVertical(connectionMask);
        return vertical
                ? LevelZeroCellMicroPattern.HALF_WALL_VERTICAL
                : LevelZeroCellMicroPattern.HALF_WALL_HORIZONTAL;
    }

    private int applyOffsetWall(LevelZeroCellContext context,
                                LevelZeroCellTopology topology,
                                int connectionMask,
                                int pattern) {
        if (topology == LevelZeroCellTopology.ANGLE) {
            return applyAngleOffset(context, connectionMask, pattern);
        }
        int side = pickWallSide(context, topology, connectionMask);
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

    private int applyAngleRecess(LevelZeroCellContext context, int connectionMask, int pattern) {
        int[] corner = blockedCorner(connectionMask);
        if (corner == null) {
            return pattern;
        }

        pattern = LevelZeroCellMicroPattern.close(pattern, corner[0], corner[1]);
        int[] arm = preferredAngleArm(context, connectionMask);
        return LevelZeroCellMicroPattern.close(pattern, arm[0], arm[1]);
    }

    private int applyAngleOffset(LevelZeroCellContext context, int connectionMask, int pattern) {
        int[] corner = blockedCorner(connectionMask);
        if (corner == null) {
            return pattern;
        }

        int[] arm = preferredAngleArm(context, connectionMask);
        pattern = LevelZeroCellMicroPattern.close(pattern, arm[0], arm[1]);
        if ((blockHash(context, corner[0], corner[1]) & 1L) == 0L) {
            pattern = LevelZeroCellMicroPattern.close(pattern, corner[0], corner[1]);
        }
        return pattern;
    }

    private int closeSpecific(int pattern, int[][] blocks) {
        for (int[] coords : blocks) {
            pattern = LevelZeroCellMicroPattern.close(pattern, coords[0], coords[1]);
        }
        return pattern;
    }

    private int pickWallSide(LevelZeroCellContext context,
                             LevelZeroCellTopology topology,
                             int connectionMask) {
        if (topology == LevelZeroCellTopology.CORRIDOR) {
            if (corridorVertical(connectionMask)) {
                return pickBetween(context, 1, 3);
            }
            return pickBetween(context, 0, 2);
        }
        if (topology == LevelZeroCellTopology.ANGLE) {
            return pickBlockedSide(context, connectionMask);
        }
        if (topology == LevelZeroCellTopology.DEAD_END) {
            return oppositeOf(openSide(connectionMask));
        }
        return pickSide(context);
    }

    private int pickAlcoveSide(LevelZeroCellContext context, int connectionMask) {
        int openSide = openSide(connectionMask);
        if (openSide >= 0) {
            return oppositeOf(openSide);
        }
        return pickSide(context);
    }

    private int pickBlockedSide(LevelZeroCellContext context, int connectionMask) {
        int first = -1;
        int second = -1;
        for (int side = 0; side < 4; side++) {
            if (!hasSide(connectionMask, side)) {
                if (first < 0) {
                    first = side;
                } else {
                    second = side;
                    break;
                }
            }
        }
        if (first >= 0 && second >= 0) {
            return pickBetween(context, first, second);
        }
        return pickSide(context);
    }

    private int openSide(int connectionMask) {
        int openSide = -1;
        for (int side = 0; side < 4; side++) {
            if (hasSide(connectionMask, side)) {
                if (openSide >= 0) {
                    return -1;
                }
                openSide = side;
            }
        }
        return openSide;
    }

    private boolean corridorVertical(int connectionMask) {
        return LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.NORTH)
                && LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.SOUTH);
    }

    private int pickBetween(LevelZeroCellContext context, int sideA, int sideB) {
        long scoreA = sideScore(context, sideA);
        long scoreB = sideScore(context, sideB);
        return scoreA >= scoreB ? sideA : sideB;
    }

    private long sideScore(LevelZeroCellContext context, int side) {
        int[] probe = sideProbe(side);
        return blockHash(context, probe[0], probe[1]);
    }

    private static boolean hasSide(int connectionMask, int side) {
        return switch (side) {
            case 0 -> LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.NORTH);
            case 1 -> LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.EAST);
            case 2 -> LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.SOUTH);
            default -> LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.WEST);
        };
    }

    private static int oppositeOf(int side) {
        return (side + 2) & 3;
    }

    private static int[] sideProbe(int side) {
        return switch (side) {
            case 0 -> new int[]{1, 0};
            case 1 -> new int[]{2, 1};
            case 2 -> new int[]{1, 2};
            default -> new int[]{0, 1};
        };
    }

    private int[] blockedCorner(int connectionMask) {
        boolean north = LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.NORTH);
        boolean east = LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.EAST);
        boolean south = LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.SOUTH);
        boolean west = LevelZeroCellConnections.has(connectionMask, LevelZeroCellConnections.WEST);

        if (!north && !west) {
            return new int[]{0, 0};
        }
        if (!north && !east) {
            return new int[]{2, 0};
        }
        if (!south && !east) {
            return new int[]{2, 2};
        }
        if (!south && !west) {
            return new int[]{0, 2};
        }
        return null;
    }

    private int[] preferredAngleArm(LevelZeroCellContext context, int connectionMask) {
        int firstBlockedSide = -1;
        int secondBlockedSide = -1;
        for (int side = 0; side < 4; side++) {
            if (!hasSide(connectionMask, side)) {
                if (firstBlockedSide < 0) {
                    firstBlockedSide = side;
                } else {
                    secondBlockedSide = side;
                    break;
                }
            }
        }

        if (firstBlockedSide < 0 || secondBlockedSide < 0) {
            return new int[]{1, 1};
        }

        int chosenSide = pickBetween(context, firstBlockedSide, secondBlockedSide);
        return sideProbe(chosenSide);
    }

    private int pickSide(LevelZeroCellContext context) {
        int side = 0;
        long best = Long.MIN_VALUE;
        for (int i = 0; i < 4; i++) {
            int[] probe = sideProbe(i);
            long score = blockHash(context, probe[0], probe[1]);
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
        // Le hash descend au niveau bloc pour casser la symetrie parfaite a
        // l'interieur d'une meme cellule tout en restant 100 % deterministe.
        return StageRandom.mixLegacy(
                context.layoutSeed(),
                StageRandom.Stage.NOISE_GEOMETRY,
                blockX,
                blockZ);
    }
}
