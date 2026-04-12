package com.petassegang.addons.world.backrooms.level0;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import com.petassegang.addons.world.backrooms.BackroomsConstants;

/**
 * Traduction deterministe du script Python de reference pour le Level 0.
 */
public final class LevelZeroLayout {

    /** Taille d'un chunk en blocs. */
    public static final int CHUNK_SIZE = 16;

    private static final int CELL_SCALE = BackroomsConstants.LEVEL_ZERO_CELL_SCALE;
    private static final int SECTOR_COLS = 1920 / 8;
    private static final int SECTOR_ROWS = 1080 / 8;
    private static final int TOTAL_CELLS = SECTOR_COLS * SECTOR_ROWS;
    private static final double MAZE_FILL_PERCENTAGE = 0.8D;
    private static final int NUM_MAZES = 1000;
    private static final double STOP_COLLISION_PROBABILITY = 0.5D;
    private static final int NUM_ROOMS = 2;
    private static final int ROOM_MIN = 1;
    private static final int ROOM_MAX = 32;
    private static final int NUM_PILLAR_ROOMS = 1;
    private static final int PILLAR_ROOM_MIN = 1;
    private static final int PILLAR_ROOM_MAX = 32;
    private static final int PILLAR_SPACING_MIN = 2;
    private static final int PILLAR_SPACING_MAX = 6;
    private static final int NUM_CUSTOM_ROOMS = 1;
    private static final int MIN_NUM_SIDES = 2;
    private static final int MAX_NUM_SIDES = 8;
    private static final int MIN_CUSTOM_ROOM_RADIUS = 1;
    private static final int MAX_CUSTOM_ROOM_RADIUS = 16;
    private static final int LIGHT_INTERVAL = 4;

    /** Identifiant de la variante de base (papier peint jauni, moquette classique). */
    public static final int SURFACE_VARIANT_BASE = 0;
    /** Identifiant de la variante alternative (murs blancs, moquette rouge). */
    public static final int SURFACE_VARIANT_ALTERNATE = 1;

    private static final int SECTOR_CACHE_CAPACITY = 1024;
    private static final Map<Long, SectorData> SECTOR_CACHE = new LinkedHashMap<>(SECTOR_CACHE_CAPACITY + 1, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, SectorData> eldest) {
            return size() > SECTOR_CACHE_CAPACITY;
        }
    };

    private final boolean[] walkable;
    private final boolean[] lighted;
    private final LevelZeroSurfaceBiome[] surfaceBiomes;
    private final boolean[] largeRoom;

    private LevelZeroLayout(boolean[] walkable, boolean[] lighted,
                            LevelZeroSurfaceBiome[] surfaceBiomes, boolean[] largeRoom) {
        this.walkable = walkable;
        this.lighted = lighted;
        this.surfaceBiomes = surfaceBiomes;
        this.largeRoom = largeRoom;
    }

    /**
     * Genere le layout d'un chunk a partir de l'equivalent logique du script Python.
     *
     * @param chunkX coordonnee X du chunk
     * @param chunkZ coordonnee Z du chunk
     * @param layoutSeed seed deterministe du niveau
     * @return layout calcule pour le chunk
     */
    public static LevelZeroLayout generate(int chunkX, int chunkZ, long layoutSeed) {
        boolean[] walkable = new boolean[CHUNK_SIZE * CHUNK_SIZE];
        boolean[] lighted = new boolean[CHUNK_SIZE * CHUNK_SIZE];
        LevelZeroSurfaceBiome[] surfaceBiomes = new LevelZeroSurfaceBiome[CHUNK_SIZE * CHUNK_SIZE];
        boolean[] largeRoom = new boolean[CHUNK_SIZE * CHUNK_SIZE];
        int worldMinX = chunkX * CHUNK_SIZE;
        int worldMinZ = chunkZ * CHUNK_SIZE;
        int worldMaxX = worldMinX + CHUNK_SIZE - 1;
        int worldMaxZ = worldMinZ + CHUNK_SIZE - 1;
        int minCellX = Math.floorDiv(worldMinX, CELL_SCALE);
        int maxCellX = Math.floorDiv(worldMaxX, CELL_SCALE);
        int minCellZ = Math.floorDiv(worldMinZ, CELL_SCALE);
        int maxCellZ = Math.floorDiv(worldMaxZ, CELL_SCALE);

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            int cellWorldMinX = cellX * CELL_SCALE;
            int startLocalX = Math.max(cellWorldMinX - worldMinX, 0);
            int endLocalX = Math.min(cellWorldMinX + CELL_SCALE - 1 - worldMinX, CHUNK_SIZE - 1);

            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                int cellWorldMinZ = cellZ * CELL_SCALE;
                int startLocalZ = Math.max(cellWorldMinZ - worldMinZ, 0);
                int endLocalZ = Math.min(cellWorldMinZ + CELL_SCALE - 1 - worldMinZ, CHUNK_SIZE - 1);
                boolean cellWalkable = sampleWalkableCell(cellX, cellZ, layoutSeed);
                LevelZeroSurfaceBiome surfaceBiome = sampleSurfaceBiomeCell(cellX, cellZ);
                boolean cellLargeRoom = sampleLargeRoomCell(cellX, cellZ, layoutSeed);

                for (int localX = startLocalX; localX <= endLocalX; localX++) {
                    for (int localZ = startLocalZ; localZ <= endLocalZ; localZ++) {
                        int index = index(localX, localZ);
                        walkable[index] = cellWalkable;
                        surfaceBiomes[index] = surfaceBiome;
                        largeRoom[index] = cellLargeRoom;
                    }
                }

                if (!cellWalkable || !sampleLightCell(cellX, cellZ, layoutSeed)) {
                    continue;
                }

                int centerWorldX = cellWorldMinX + CELL_SCALE / 2;
                int centerWorldZ = cellWorldMinZ + CELL_SCALE / 2;
                if (centerWorldX < worldMinX || centerWorldX > worldMaxX || centerWorldZ < worldMinZ || centerWorldZ > worldMaxZ) {
                    continue;
                }

                int centerLocalX = centerWorldX - worldMinX;
                int centerLocalZ = centerWorldZ - worldMinZ;
                lighted[index(centerLocalX, centerLocalZ)] = true;
            }
        }

        return new LevelZeroLayout(walkable, lighted, surfaceBiomes, largeRoom);
    }

    /**
     * Echantillonne directement la walkability a une position monde.
     *
     * <p>Ce point d'entree est utile pour les systemes qui ont seulement besoin
     * de la forme du labyrinthe, sans payer le cout d'un layout complet.
     *
     * @param worldX coordonnee X monde
     * @param worldZ coordonnee Z monde
     * @param layoutSeed seed deterministe du niveau
     * @return {@code true} si la position appartient a une cellule ouverte
     */
    public static boolean isWalkableAtWorld(int worldX, int worldZ, long layoutSeed) {
        int cellX = Math.floorDiv(worldX, CELL_SCALE);
        int cellZ = Math.floorDiv(worldZ, CELL_SCALE);
        return sampleWalkableCell(cellX, cellZ, layoutSeed);
    }

    /**
     * Indique si la position locale est traversable.
     *
     * @param localX coordonnee X locale dans le chunk
     * @param localZ coordonnee Z locale dans le chunk
     * @return {@code true} si la position appartient a une cellule ouverte
     */
    public boolean isWalkable(int localX, int localZ) {
        return walkable[index(localX, localZ)];
    }

    /**
     * Indique si un neon doit etre place au plafond a cette position.
     *
     * @param localX coordonnee X locale dans le chunk
     * @param localZ coordonnee Z locale dans le chunk
     * @return {@code true} si un bloc lumineux doit etre place au plafond
     */
    public boolean hasLight(int localX, int localZ) {
        return lighted[index(localX, localZ)];
    }

    private static boolean sampleWalkableCell(int cellX, int cellZ, long layoutSeed) {
        if (cellX >= -4 && cellX <= 4 && cellZ >= -4 && cellZ <= 4) {
            return true;
        }

        int sectorX = Math.floorDiv(cellX, SECTOR_COLS);
        int sectorZ = Math.floorDiv(cellZ, SECTOR_ROWS);
        int localCellX = Math.floorMod(cellX, SECTOR_COLS);
        int localCellZ = Math.floorMod(cellZ, SECTOR_ROWS);
        SectorData sector = getSector(sectorX, sectorZ, layoutSeed);
        return sector.isWalkable(localCellX, localCellZ);
    }

    /**
     * Retourne le biome cosmetique de surface a la position locale donnee.
     *
     * @param localX coordonnee X locale dans le chunk
     * @param localZ coordonnee Z locale dans le chunk
     * @return biome de surface calcule pour cette position
     */
    public LevelZeroSurfaceBiome surfaceBiome(int localX, int localZ) {
        return surfaceBiomes[index(localX, localZ)];
    }

    /**
     * Retourne la variante de sol a la position locale donnee.
     *
     * @param localX coordonnee X locale dans le chunk
     * @param localZ coordonnee Z locale dans le chunk
     * @return identifiant de variante de moquette
     */
    public int floorVariant(int localX, int localZ) {
        return surfaceBiomes[index(localX, localZ)].floorVariant();
    }

    /**
     * Retourne la variante de papier peint a la position locale donnee.
     *
     * @param localX coordonnee X locale dans le chunk
     * @param localZ coordonnee Z locale dans le chunk
     * @return identifiant de variante de papier peint
     */
    public int wallpaperVariant(int localX, int localZ) {
        return surfaceBiomes[index(localX, localZ)].wallpaperVariant();
    }

    /**
     * Indique si la position locale appartient a une grande piece.
     *
     * @param localX coordonnee X locale dans le chunk
     * @param localZ coordonnee Z locale dans le chunk
     * @return {@code true} si la cellule logique est marquee comme grande piece
     */
    public boolean isLargeRoom(int localX, int localZ) {
        return largeRoom[index(localX, localZ)];
    }

    private static boolean sampleLightCell(int cellX, int cellZ, long layoutSeed) {
        long seed = mix(layoutSeed, cellX, cellZ, 0x4C49474854L);
        return Math.floorMod(seed, LIGHT_INTERVAL) == 0;
    }

    private static LevelZeroSurfaceBiome sampleSurfaceBiomeCell(int cellX, int cellZ) {
        return LevelZeroSurfaceBiome.sampleAtCell(cellX, cellZ);
    }

    private static boolean sampleLargeRoomCell(int cellX, int cellZ, long layoutSeed) {
        long hash = mix(layoutSeed, cellX, cellZ, 0x4C41524745524F4DL);
        return Math.floorMod(hash, 4) == 0;
    }

    private static SectorData getSector(int sectorX, int sectorZ, long layoutSeed) {
        long cacheKey = mix(layoutSeed, sectorX, sectorZ, 0x534543544F52L);

        // Lecture rapide — verrou court, ne bloque pas la génération.
        synchronized (SECTOR_CACHE) {
            SectorData cached = SECTOR_CACHE.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        // Génération hors du verrou — déterministe : si deux threads génèrent le même
        // secteur en parallèle, ils produisent exactement le même résultat.
        SectorData generated = generateSector(sectorX, sectorZ, layoutSeed);

        // Écriture dans le cache — computeIfAbsent conserve la version d'un autre thread
        // si plusieurs générations parallèles se sont terminées entre-temps.
        synchronized (SECTOR_CACHE) {
            return SECTOR_CACHE.computeIfAbsent(cacheKey, k -> generated);
        }
    }

    private static SectorData generateSector(int sectorX, int sectorZ, long layoutSeed) {
        boolean[] cells = new boolean[TOTAL_CELLS];
        Random random = new Random(mix(layoutSeed, sectorX, sectorZ, 0x4D415A45L));
        ArrayList<Long> frontier = new ArrayList<>();
        int visitedCount = 0;
        boolean[] visited = new boolean[TOTAL_CELLS];

        for (int overlay = 0; overlay < NUM_MAZES; overlay++) {
            int startX = random.nextInt(SECTOR_COLS);
            int startZ = random.nextInt(SECTOR_ROWS);
            frontier.clear();
            frontier.add(encode(startX, startZ));

            while ((double) visitedCount / TOTAL_CELLS < MAZE_FILL_PERCENTAGE) {
                if (frontier.isEmpty()) {
                    break;
                }

                int frontierIndex = random.nextInt(frontier.size());
                // Swap-and-pop O(1) : échange avec le dernier élément puis supprime la fin.
                // L'ordre dans la frontier n'a pas d'importance — l'index est tiré aléatoirement.
                long encoded = frontier.get(frontierIndex);
                int lastIndex = frontier.size() - 1;
                if (frontierIndex != lastIndex) {
                    frontier.set(frontierIndex, frontier.get(lastIndex));
                }
                frontier.remove(lastIndex);
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
                if (x < SECTOR_COLS - 2 && !visited[cellIndex(x + 2, z)]) {
                    neighbors[neighborCount++] = x + 2;
                    neighbors[neighborCount++] = z;
                }
                if (z > 1 && !visited[cellIndex(x, z - 2)]) {
                    neighbors[neighborCount++] = x;
                    neighbors[neighborCount++] = z - 2;
                }
                if (z < SECTOR_ROWS - 2 && !visited[cellIndex(x, z + 2)]) {
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

                    if (random.nextDouble() > STOP_COLLISION_PROBABILITY || !cells[betweenIndex]) {
                        frontier.add(encode(nextX, nextZ));
                        cells[betweenIndex] = true;
                    }
                }
            }
        }

        applyRectRooms(cells, random, NUM_ROOMS, ROOM_MIN, ROOM_MAX, ROOM_MIN, ROOM_MAX);
        applyPillarRooms(cells, random);
        applyCustomRooms(cells, random);

        return new SectorData(cells);
    }

    private static void applyRectRooms(boolean[] cells,
                                       Random random,
                                       int roomCount,
                                       int widthMin,
                                       int widthMax,
                                       int heightMin,
                                       int heightMax) {
        for (int roomIndex = 0; roomIndex < roomCount; roomIndex++) {
            int roomWidth = nextInclusive(random, widthMin, widthMax);
            int roomHeight = nextInclusive(random, heightMin, heightMax);
            int roomX = random.nextInt(SECTOR_COLS - roomWidth + 1);
            int roomZ = random.nextInt(SECTOR_ROWS - roomHeight + 1);

            for (int z = roomZ; z < roomZ + roomHeight; z++) {
                for (int x = roomX; x < roomX + roomWidth; x++) {
                    cells[cellIndex(x, z)] = true;
                }
            }
        }
    }

    private static void applyPillarRooms(boolean[] cells, Random random) {
        for (int roomIndex = 0; roomIndex < NUM_PILLAR_ROOMS; roomIndex++) {
            int roomWidth = nextInclusive(random, PILLAR_ROOM_MIN, PILLAR_ROOM_MAX);
            int roomHeight = nextInclusive(random, PILLAR_ROOM_MIN, PILLAR_ROOM_MAX);
            int roomX = random.nextInt(SECTOR_COLS - roomWidth + 1);
            int roomZ = random.nextInt(SECTOR_ROWS - roomHeight + 1);

            for (int z = roomZ; z < roomZ + roomHeight; z++) {
                for (int x = roomX; x < roomX + roomWidth; x++) {
                    cells[cellIndex(x, z)] = true;
                }
            }

            int pillarSpacing = nextInclusive(random, PILLAR_SPACING_MIN, PILLAR_SPACING_MAX);
            for (int z = roomZ; z < roomZ + roomHeight; z += pillarSpacing) {
                for (int x = roomX; x < roomX + roomWidth; x += pillarSpacing) {
                    cells[cellIndex(x, z)] = false;
                }
            }
        }
    }

    private static void applyCustomRooms(boolean[] cells, Random random) {
        for (int roomIndex = 0; roomIndex < NUM_CUSTOM_ROOMS; roomIndex++) {
            int sides = nextInclusive(random, MIN_NUM_SIDES, MAX_NUM_SIDES);
            int radius = nextInclusive(random, MIN_CUSTOM_ROOM_RADIUS, MAX_CUSTOM_ROOM_RADIUS);
            int centerX = nextInclusive(random, radius * 2, SECTOR_COLS - radius * 2);
            int centerZ = nextInclusive(random, radius * 2, SECTOR_ROWS - radius * 2);
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
                        cells[cellIndex(x, z)] = true;
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

    private static int index(int localX, int localZ) {
        return localZ * CHUNK_SIZE + localX;
    }

    private static int cellIndex(int cellX, int cellZ) {
        return cellZ * SECTOR_COLS + cellX;
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

    private static long mix(long seed, long a, long b, long salt) {
        long mixed = seed ^ salt;
        mixed ^= a * 0x9E3779B97F4A7C15L;
        mixed = Long.rotateLeft(mixed, 17);
        mixed ^= b * 0xC2B2AE3D27D4EB4FL;
        mixed = Long.rotateLeft(mixed, 31);
        mixed *= 0x165667B19E3779F9L;
        return mixed;
    }

    private static final class SectorData {
        private final boolean[] walkable;

        private SectorData(boolean[] walkable) {
            this.walkable = walkable;
        }

        private boolean isWalkable(int x, int z) {
            return walkable[cellIndex(x, z)];
        }
    }
}
