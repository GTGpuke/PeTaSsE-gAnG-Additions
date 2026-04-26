package com.petassegang.addons.world.backrooms.level0.layout.sector;

import java.util.Random;

import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;

/**
 * Generateur historique d'un secteur logique du Level 0.
 *
 * <p>Le secteur est la couche la plus brute de la generation : c'est ici que
 * vivent le melange mini-labyrinthes + stamping de salles herite du script de
 * reference. La suite de la pipeline enrichit et extrait cette base, mais ne
 * doit pas en modifier silencieusement la grammaire.
 */
public final class LevelZeroSectorGenerator {

    private final int sectorCols;
    private final int sectorRows;
    private final int totalCells;
    private final double mazeFillPercentage;
    private final int numMazes;
    private final double stopCollisionProbability;
    private final int numRooms;
    private final int roomMin;
    private final int roomMax;
    private final int numPillarRooms;
    private final int pillarRoomMin;
    private final int pillarRoomMax;
    private final int pillarSpacingMin;
    private final int pillarSpacingMax;
    private final int numCustomRooms;
    private final int minNumSides;
    private final int maxNumSides;
    private final int minCustomRoomRadius;
    private final int maxCustomRoomRadius;

    /**
     * Construit le generateur historique avec les parametres actuels.
     */
    public LevelZeroSectorGenerator(int sectorCols,
                                    int sectorRows,
                                    int totalCells,
                                    double mazeFillPercentage,
                                    int numMazes,
                                    double stopCollisionProbability,
                                    int numRooms,
                                    int roomMin,
                                    int roomMax,
                                    int numPillarRooms,
                                    int pillarRoomMin,
                                    int pillarRoomMax,
                                    int pillarSpacingMin,
                                    int pillarSpacingMax,
                                    int numCustomRooms,
                                    int minNumSides,
                                    int maxNumSides,
                                    int minCustomRoomRadius,
                                    int maxCustomRoomRadius) {
        this.sectorCols = sectorCols;
        this.sectorRows = sectorRows;
        this.totalCells = totalCells;
        this.mazeFillPercentage = mazeFillPercentage;
        this.numMazes = numMazes;
        this.stopCollisionProbability = stopCollisionProbability;
        this.numRooms = numRooms;
        this.roomMin = roomMin;
        this.roomMax = roomMax;
        this.numPillarRooms = numPillarRooms;
        this.pillarRoomMin = pillarRoomMin;
        this.pillarRoomMax = pillarRoomMax;
        this.pillarSpacingMin = pillarSpacingMin;
        this.pillarSpacingMax = pillarSpacingMax;
        this.numCustomRooms = numCustomRooms;
        this.minNumSides = minNumSides;
        this.maxNumSides = maxNumSides;
        this.minCustomRoomRadius = minCustomRoomRadius;
        this.maxCustomRoomRadius = maxCustomRoomRadius;
    }

    /**
     * Genere un secteur logique complet.
     *
     * @param sectorX coordonnee secteur X
     * @param sectorZ coordonnee secteur Z
     * @param layoutSeed seed de layout
     * @return donnees immuables du secteur
     */
    public LevelZeroSectorData generate(int sectorX, int sectorZ, long layoutSeed) {
        boolean[] cells = new boolean[totalCells];
        LevelZeroSectorRoomKind[] roomKinds = new LevelZeroSectorRoomKind[totalCells];
        java.util.Arrays.fill(roomKinds, LevelZeroSectorRoomKind.NONE);
        Random random = StageRandom.createLegacyRandom(layoutSeed, StageRandom.Stage.SECTOR_MAZE, sectorX, sectorZ);
        long[] frontier = new long[totalCells];
        int frontierSize = 0;
        int visitedCount = 0;
        boolean[] visited = new boolean[totalCells];

        for (int overlay = 0; overlay < numMazes; overlay++) {
            int startX = random.nextInt(sectorCols);
            int startZ = random.nextInt(sectorRows);
            frontierSize = 0;
            frontier[frontierSize++] = encode(startX, startZ);

            while ((double) visitedCount / totalCells < mazeFillPercentage) {
                if (frontierSize == 0) {
                    break;
                }

                int frontierIndex = random.nextInt(frontierSize);
                long encoded = frontier[frontierIndex];
                int lastIndex = frontierSize - 1;
                if (frontierIndex != lastIndex) {
                    frontier[frontierIndex] = frontier[lastIndex];
                }
                frontierSize--;
                int x = decodeX(encoded);
                int z = decodeZ(encoded);
                int currentIndex = cellIndex(x, z);
                if (!visited[currentIndex]) {
                    visited[currentIndex] = true;
                    visitedCount++;
                }
                cells[currentIndex] = true;

                int[] neighbors = new int[8];
                int neighborCount = 0;

                if (x > 1 && !visited[cellIndex(x - 2, z)]) {
                    neighbors[neighborCount++] = x - 2;
                    neighbors[neighborCount++] = z;
                }
                if (x < sectorCols - 2 && !visited[cellIndex(x + 2, z)]) {
                    neighbors[neighborCount++] = x + 2;
                    neighbors[neighborCount++] = z;
                }
                if (z > 1 && !visited[cellIndex(x, z - 2)]) {
                    neighbors[neighborCount++] = x;
                    neighbors[neighborCount++] = z - 2;
                }
                if (z < sectorRows - 2 && !visited[cellIndex(x, z + 2)]) {
                    neighbors[neighborCount++] = x;
                    neighbors[neighborCount++] = z + 2;
                }

                if (neighborCount > 0) {
                    int pick = random.nextInt(neighborCount / 2) * 2;
                    int nextX = neighbors[pick];
                    int nextZ = neighbors[pick + 1];
                    int betweenX = (x + nextX) / 2;
                    int betweenZ = (z + nextZ) / 2;
                    int betweenIndex = cellIndex(betweenX, betweenZ);

                    if (random.nextDouble() > stopCollisionProbability || !cells[betweenIndex]) {
                        frontier[frontierSize++] = encode(nextX, nextZ);
                        cells[betweenIndex] = true;
                    }
                }
            }
        }

        applyRectRooms(cells, roomKinds, random, numRooms, roomMin, roomMax, roomMin, roomMax);
        applyPillarRooms(cells, roomKinds, random);
        applyCustomRooms(cells, roomKinds, random);

        return new LevelZeroSectorData(cells, roomKinds, sectorCols);
    }

    private void applyRectRooms(boolean[] cells,
                                LevelZeroSectorRoomKind[] roomKinds,
                                Random random,
                                int roomCount,
                                int widthMin,
                                int widthMax,
                                int heightMin,
                                int heightMax) {
        for (int roomIndex = 0; roomIndex < roomCount; roomIndex++) {
            int roomWidth = nextInclusive(random, widthMin, widthMax);
            int roomHeight = nextInclusive(random, heightMin, heightMax);
            int roomX = random.nextInt(sectorCols - roomWidth + 1);
            int roomZ = random.nextInt(sectorRows - roomHeight + 1);

            for (int z = roomZ; z < roomZ + roomHeight; z++) {
                for (int x = roomX; x < roomX + roomWidth; x++) {
                    int index = cellIndex(x, z);
                    cells[index] = true;
                    roomKinds[index] = LevelZeroSectorRoomKind.RECT_ROOM;
                }
            }
        }
    }

    private void applyPillarRooms(boolean[] cells, LevelZeroSectorRoomKind[] roomKinds, Random random) {
        for (int roomIndex = 0; roomIndex < numPillarRooms; roomIndex++) {
            int roomWidth = nextInclusive(random, pillarRoomMin, pillarRoomMax);
            int roomHeight = nextInclusive(random, pillarRoomMin, pillarRoomMax);
            int roomX = random.nextInt(sectorCols - roomWidth + 1);
            int roomZ = random.nextInt(sectorRows - roomHeight + 1);

            for (int z = roomZ; z < roomZ + roomHeight; z++) {
                for (int x = roomX; x < roomX + roomWidth; x++) {
                    int index = cellIndex(x, z);
                    cells[index] = true;
                    roomKinds[index] = LevelZeroSectorRoomKind.PILLAR_ROOM;
                }
            }

            int pillarSpacing = nextInclusive(random, pillarSpacingMin, pillarSpacingMax);
            for (int z = roomZ; z < roomZ + roomHeight; z += pillarSpacing) {
                for (int x = roomX; x < roomX + roomWidth; x += pillarSpacing) {
                    int index = cellIndex(x, z);
                    cells[index] = false;
                    roomKinds[index] = LevelZeroSectorRoomKind.PILLAR_ROOM;
                }
            }
        }
    }

    private void applyCustomRooms(boolean[] cells, LevelZeroSectorRoomKind[] roomKinds, Random random) {
        for (int roomIndex = 0; roomIndex < numCustomRooms; roomIndex++) {
            int sides = nextInclusive(random, minNumSides, maxNumSides);
            int radius = nextInclusive(random, minCustomRoomRadius, maxCustomRoomRadius);
            int centerX = nextInclusive(random, radius * 2, sectorCols - radius * 2);
            int centerZ = nextInclusive(random, radius * 2, sectorRows - radius * 2);
            int[] verticesX = new int[sides];
            int[] verticesZ = new int[sides];
            double angleStep = Math.PI * 2.0D / sides;

            for (int index = 0; index < sides; index++) {
                double angle = index * angleStep;
                verticesX[index] = (int) (centerX + radius * Math.cos(angle));
                verticesZ[index] = (int) (centerZ + radius * Math.sin(angle));
            }

            for (int z = centerZ - radius; z < centerZ + radius; z++) {
                for (int x = centerX - radius; x < centerX + radius; x++) {
                    if (isInsidePolygon(x, z, verticesX, verticesZ)) {
                        int index = cellIndex(x, z);
                        cells[index] = true;
                        roomKinds[index] = LevelZeroSectorRoomKind.CUSTOM_ROOM;
                    }
                }
            }
        }
    }

    private static boolean isInsidePolygon(int x, int z, int[] verticesX, int[] verticesZ) {
        boolean inside = false;
        for (int index = 0; index < verticesX.length; index++) {
            int nextIndex = (index + 1) % verticesX.length;
            int ax = verticesX[index];
            int az = verticesZ[index];
            int bx = verticesX[nextIndex];
            int bz = verticesZ[nextIndex];

            int leftX;
            int leftZ;
            int rightX;
            int rightZ;
            if (ax < bx) {
                leftX = ax;
                leftZ = az;
                rightX = bx;
                rightZ = bz;
            } else {
                leftX = bx;
                leftZ = bz;
                rightX = ax;
                rightZ = az;
            }

            boolean crosses = (az > z) != (bz > z);
            if (crosses) {
                double crossX = (double) (rightX - leftX) * (z - leftZ) / (double) (rightZ - leftZ) + leftX;
                if (x < crossX) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    private static int nextInclusive(Random random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private int cellIndex(int cellX, int cellZ) {
        return cellZ * sectorCols + cellX;
    }

    private static long encode(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int decodeX(long encoded) {
        return (int) (encoded >> 32);
    }

    private static int decodeZ(long encoded) {
        return (int) encoded;
    }
}
