package com.petassegang.addons.world.backrooms.level0;

import java.util.concurrent.ConcurrentHashMap;

import com.petassegang.addons.world.backrooms.BackroomsConstants;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroCoords;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTag;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellState;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroChunkSlice;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroSectorData;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroSectorGenerator;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionGrid;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroLegacyLayoutPipeline;

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
    private static final int LIGHT_INTERVAL = 7;

    /** Identifiant de la variante de base (papier peint jauni, moquette classique). */
    public static final int SURFACE_VARIANT_BASE = 0;
    /** Identifiant de la variante alternative (murs blancs, moquette rouge). */
    public static final int SURFACE_VARIANT_ALTERNATE = 1;

    private static final int SECTOR_CACHE_CAPACITY = 1024;
    private static final ConcurrentHashMap<Long, LevelZeroSectorData> SECTOR_CACHE =
            new ConcurrentHashMap<>(SECTOR_CACHE_CAPACITY * 2, 0.75f, 4);
    private static final LevelZeroSectorGenerator SECTOR_GENERATOR = new LevelZeroSectorGenerator(
            SECTOR_COLS,
            SECTOR_ROWS,
            TOTAL_CELLS,
            MAZE_FILL_PERCENTAGE,
            NUM_MAZES,
            STOP_COLLISION_PROBABILITY,
            NUM_ROOMS,
            ROOM_MIN,
            ROOM_MAX,
            NUM_PILLAR_ROOMS,
            PILLAR_ROOM_MIN,
            PILLAR_ROOM_MAX,
            PILLAR_SPACING_MIN,
            PILLAR_SPACING_MAX,
            NUM_CUSTOM_ROOMS,
            MIN_NUM_SIDES,
            MAX_NUM_SIDES,
            MIN_CUSTOM_ROOM_RADIUS,
            MAX_CUSTOM_ROOM_RADIUS);

    private final boolean[] walkable;
    private final boolean[] lighted;
    private final LevelZeroSurfaceBiome[] surfaceBiomes;
    private final boolean[] largeRoom;
    private final LevelZeroCellTag[] cellTags;
    private final LevelZeroChunkSlice chunkSlice;

    private LevelZeroLayout(boolean[] walkable, boolean[] lighted,
                            LevelZeroSurfaceBiome[] surfaceBiomes,
                            boolean[] largeRoom,
                            LevelZeroCellTag[] cellTags,
                            LevelZeroChunkSlice chunkSlice) {
        this.walkable = walkable;
        this.lighted = lighted;
        this.surfaceBiomes = surfaceBiomes;
        this.largeRoom = largeRoom;
        this.cellTags = cellTags;
        this.chunkSlice = chunkSlice;
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
        LevelZeroChunkSlice chunkSlice = regionGrid(layoutSeed).extractChunk(chunkX, chunkZ);
        return new LevelZeroLayout(
                chunkSlice.walkable(),
                chunkSlice.lighted(),
                chunkSlice.surfaceBiomes(),
                chunkSlice.largeRoom(),
                chunkSlice.cellTags(),
                chunkSlice);
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
        return regionGrid(layoutSeed).sampleWalkableCell(
                LevelZeroCoords.worldToCellX(worldX),
                LevelZeroCoords.worldToCellZ(worldZ));
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

    /**
     * Retourne le tag semantique minimal de la position locale.
     *
     * @param localX coordonnee X locale dans le chunk
     * @param localZ coordonnee Z locale dans le chunk
     * @return tag semantique de la cellule locale
     */
    public LevelZeroCellTag cellTag(int localX, int localZ) {
        return cellTags[index(localX, localZ)];
    }

    /**
     * Retourne l'etat semantique minimal de la position locale.
     *
     * @param localX coordonnee X locale dans le chunk
     * @param localZ coordonnee Z locale dans le chunk
     * @return etat semantique de la cellule locale
     */
    public LevelZeroCellState cellState(int localX, int localZ) {
        return chunkSlice.cellState(localX, localZ);
    }

    /**
     * Vide le cache de secteurs.
     * A appeler lors de l'arret du serveur pour liberer la memoire.
     */
    public static void clearCache() {
        SECTOR_CACHE.clear();
    }

    private static int index(int localX, int localZ) {
        return localZ * CHUNK_SIZE + localX;
    }

    private static LevelZeroRegionGrid regionGrid(long layoutSeed) {
        return new LevelZeroRegionGrid(
                layoutSeed,
                SECTOR_COLS,
                SECTOR_ROWS,
                LIGHT_INTERVAL,
                SECTOR_CACHE_CAPACITY,
                SECTOR_CACHE,
                SECTOR_GENERATOR,
                new LevelZeroLegacyLayoutPipeline(LIGHT_INTERVAL));
    }
}
