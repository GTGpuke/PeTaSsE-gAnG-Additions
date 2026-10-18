package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.sector;

import java.util.Random;

import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.noise.StageRandom;

/**
 * Generateur d'un secteur logique du Level 0.
 *
 * <p>C'est la couche la plus brute de la generation, heritee du script de
 * reference : superposition de mini-labyrinthes (Prim) puis tamponnage de
 * salles rectangulaires, a piliers et polygonales. L'algorithme est fige :
 * toute modification change la topologie de tous les mondes existants.
 */
public final class LevelZeroSectorGenerator {

    /**
     * Parametres du generateur de secteur.
     *
     * @param cols largeur du secteur en cellules
     * @param rows hauteur du secteur en cellules
     * @param mazeFillPercentage part cible de cellules ouvertes avant arret des mini-labyrinthes
     * @param numMazes nombre de mini-labyrinthes superposes
     * @param stopCollisionProbability probabilite d'arret quand un labyrinthe rencontre un couloir existant
     * @param numRooms nombre de salles rectangulaires
     * @param roomMin taille minimale d'une salle rectangulaire
     * @param roomMax taille maximale d'une salle rectangulaire
     * @param numPillarRooms nombre de salles a piliers
     * @param pillarRoomMin taille minimale d'une salle a piliers
     * @param pillarRoomMax taille maximale d'une salle a piliers
     * @param pillarSpacingMin espacement minimal des piliers
     * @param pillarSpacingMax espacement maximal des piliers
     * @param numCustomRooms nombre de salles polygonales
     * @param minNumSides nombre minimal de cotes d'une salle polygonale
     * @param maxNumSides nombre maximal de cotes d'une salle polygonale
     * @param minCustomRoomRadius rayon minimal d'une salle polygonale
     * @param maxCustomRoomRadius rayon maximal d'une salle polygonale
     */
    public record Settings(
            int cols,
            int rows,
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

        /** Parametres historiques du Level 0 : grille 1920x1080 reduite par pas de 8. */
        public static final Settings LEVEL_ZERO = new Settings(
                1920 / 8, 1080 / 8,
                0.8D, 1000, 0.5D,
                2, 1, 32,
                1, 1, 32, 2, 6,
                1, 2, 8, 1, 16);

        /** Nombre total de cellules du secteur. */
        public int totalCells() {
            return cols * rows;
        }
    }

    private final Settings settings;

    /**
     * Construit un generateur de secteur.
     *
     * @param settings parametres du secteur
     */
    public LevelZeroSectorGenerator(Settings settings) {
        this.settings = settings;
    }

    /** Parametres de ce generateur. */
    public Settings settings() {
        return settings;
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
        int cols = settings.cols();
        int rows = settings.rows();
        int totalCells = settings.totalCells();
        boolean[] cells = new boolean[totalCells];
        byte[] roomKinds = new byte[totalCells];
        Random random = StageRandom.createRandom(layoutSeed, StageRandom.Stage.SECTOR_MAZE, sectorX, sectorZ);
        long[] frontier = new long[totalCells];
        boolean[] visited = new boolean[totalCells];
        int[] neighbors = new int[8];
        int visitedCount = 0;

        for (int overlay = 0; overlay < settings.numMazes(); overlay++) {
            int startX = random.nextInt(cols);
            int startZ = random.nextInt(rows);
            int frontierSize = 0;
            frontier[frontierSize++] = encode(startX, startZ);

            while ((double) visitedCount / totalCells < settings.mazeFillPercentage()) {
                if (frontierSize == 0) {
                    break;
                }

                // La frontiere ne tient jamais plus d'une cellule (une retiree, au plus une ajoutee par tour) : le tirage
                // vaut toujours zero, mais il consomme le generateur comme avant et fixe la suite des nombres.
                long encoded = frontier[random.nextInt(frontierSize)];
                frontierSize--;
                int x = decodeX(encoded);
                int z = decodeZ(encoded);
                int currentIndex = cellIndex(x, z);
                if (!visited[currentIndex]) {
                    visited[currentIndex] = true;
                    visitedCount++;
                }
                cells[currentIndex] = true;

                int neighborCount = 0;
                if (x > 1 && !visited[cellIndex(x - 2, z)]) {
                    neighbors[neighborCount++] = x - 2;
                    neighbors[neighborCount++] = z;
                }
                if (x < cols - 2 && !visited[cellIndex(x + 2, z)]) {
                    neighbors[neighborCount++] = x + 2;
                    neighbors[neighborCount++] = z;
                }
                if (z > 1 && !visited[cellIndex(x, z - 2)]) {
                    neighbors[neighborCount++] = x;
                    neighbors[neighborCount++] = z - 2;
                }
                if (z < rows - 2 && !visited[cellIndex(x, z + 2)]) {
                    neighbors[neighborCount++] = x;
                    neighbors[neighborCount++] = z + 2;
                }

                if (neighborCount > 0) {
                    int pick = random.nextInt(neighborCount / 2) * 2;
                    int nextX = neighbors[pick];
                    int nextZ = neighbors[pick + 1];
                    int betweenIndex = cellIndex((x + nextX) / 2, (z + nextZ) / 2);
                    if (random.nextDouble() > settings.stopCollisionProbability() || !cells[betweenIndex]) {
                        frontier[frontierSize++] = encode(nextX, nextZ);
                        cells[betweenIndex] = true;
                    }
                }
            }
        }

        applyRectRooms(cells, roomKinds, random);
        applyPillarRooms(cells, roomKinds, random);
        applyCustomRooms(cells, roomKinds, random);

        return new LevelZeroSectorData(cells, roomKinds, cols);
    }

    private void applyRectRooms(boolean[] cells, byte[] roomKinds, Random random) {
        byte kind = (byte) LevelZeroSectorRoomKind.RECT_ROOM.ordinal();
        for (int roomIndex = 0; roomIndex < settings.numRooms(); roomIndex++) {
            int roomWidth = nextInclusive(random, settings.roomMin(), settings.roomMax());
            int roomHeight = nextInclusive(random, settings.roomMin(), settings.roomMax());
            int roomX = random.nextInt(settings.cols() - roomWidth + 1);
            int roomZ = random.nextInt(settings.rows() - roomHeight + 1);
            for (int z = roomZ; z < roomZ + roomHeight; z++) {
                for (int x = roomX; x < roomX + roomWidth; x++) {
                    int index = cellIndex(x, z);
                    cells[index] = true;
                    roomKinds[index] = kind;
                }
            }
        }
    }

    private void applyPillarRooms(boolean[] cells, byte[] roomKinds, Random random) {
        byte kind = (byte) LevelZeroSectorRoomKind.PILLAR_ROOM.ordinal();
        for (int roomIndex = 0; roomIndex < settings.numPillarRooms(); roomIndex++) {
            int roomWidth = nextInclusive(random, settings.pillarRoomMin(), settings.pillarRoomMax());
            int roomHeight = nextInclusive(random, settings.pillarRoomMin(), settings.pillarRoomMax());
            int roomX = random.nextInt(settings.cols() - roomWidth + 1);
            int roomZ = random.nextInt(settings.rows() - roomHeight + 1);
            for (int z = roomZ; z < roomZ + roomHeight; z++) {
                for (int x = roomX; x < roomX + roomWidth; x++) {
                    int index = cellIndex(x, z);
                    cells[index] = true;
                    roomKinds[index] = kind;
                }
            }
            int pillarSpacing = nextInclusive(random, settings.pillarSpacingMin(), settings.pillarSpacingMax());
            for (int z = roomZ; z < roomZ + roomHeight; z += pillarSpacing) {
                for (int x = roomX; x < roomX + roomWidth; x += pillarSpacing) {
                    int index = cellIndex(x, z);
                    cells[index] = false;
                    roomKinds[index] = kind;
                }
            }
        }
    }

    private void applyCustomRooms(boolean[] cells, byte[] roomKinds, Random random) {
        byte kind = (byte) LevelZeroSectorRoomKind.CUSTOM_ROOM.ordinal();
        for (int roomIndex = 0; roomIndex < settings.numCustomRooms(); roomIndex++) {
            int sides = nextInclusive(random, settings.minNumSides(), settings.maxNumSides());
            int radius = nextInclusive(random, settings.minCustomRoomRadius(), settings.maxCustomRoomRadius());
            int centerX = nextInclusive(random, radius * 2, settings.cols() - radius * 2);
            int centerZ = nextInclusive(random, radius * 2, settings.rows() - radius * 2);
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
                        roomKinds[index] = kind;
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
            int leftX = ax < bx ? ax : bx;
            int leftZ = ax < bx ? az : bz;
            int rightX = ax < bx ? bx : ax;
            int rightZ = ax < bx ? bz : az;
            if ((az > z) != (bz > z)) {
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
        return cellZ * settings.cols() + cellX;
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
