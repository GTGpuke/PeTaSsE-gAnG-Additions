package com.petassegang.addons.backrooms.level.level0.generation.layout;

import java.util.concurrent.ConcurrentHashMap;

import com.petassegang.addons.perf.section.ModPerformanceMonitor;
import com.petassegang.addons.backrooms.level.level0.biome.LevelZeroSurfaceBiome;
import com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroCoords;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorData;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorGenerator;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroLegacyLayoutPipeline;

/**
 * Facade historique stable du layout logique du Level 0.
 *
 * <p>Cette classe est le meilleur point d'entree pour comprendre la generation
 * logique sans se perdre dans l'ecriture de blocs :
 *
 * <ul>
 *   <li>{@code secteur} : generation brute du motif historique a grande echelle ;</li>
 *   <li>{@code region} : fenetre deterministe suffisante pour evaluer un chunk
 *   avec son contexte ;</li>
 *   <li>{@code chunk} : extraction locale finale, prete a etre ecrite.</li>
 * </ul>
 *
 * <p>Le mot {@code historique} / {@code legacy} autour de cette facade signifie
 * que l'on preserve ici le coeur de generation du Level 0 tel qu'il produit le
 * ressenti valide du projet. Ce coeur ne doit pas bouger silencieusement : les
 * nouvelles couches doivent s'appuyer dessus, pas le remplacer sans decision
 * explicite.
 */
public final class LevelZeroLayout {

    /** Taille d'un chunk en blocs. */
    public static final int CHUNK_SIZE = 16;

    // Ces dimensions reprennent la resolution logique historique utilisee par la
    // traduction du script de reference. Elles proviennent d'une grille
    // "ecran-like" 1920x1080 reduite par pas de 8, conservee pour preserver la
    // densite et le ressenti du layout d'origine.
    private static final int SECTOR_COLS = 1920 / 8;
    private static final int SECTOR_ROWS = 1080 / 8;
    private static final int TOTAL_CELLS = SECTOR_COLS * SECTOR_ROWS;
    // Part cible de cellules ouvertes dans un secteur. Plus la valeur monte,
    // plus le niveau s'aere ; plus elle baisse, plus le maillage se referme.
    private static final double MAZE_FILL_PERCENTAGE = 0.8D;
    // Nombre de mini-labyrinthes superposes. Cette valeur pilote surtout la
    // richesse du maillage historique et la frequence des recouvrements.
    private static final int NUM_MAZES = 1000;
    // Probabilite d'arret lorsqu'un mini-labyrinthe rencontre un couloir deja
    // present. C'est un des leviers historiques du melange entre densite et
    // fragmentation du tracé.
    private static final double STOP_COLLISION_PROBABILITY = 0.5D;
    // Quantite de salles rectangulaires injectees par secteur logique.
    private static final int NUM_ROOMS = 2;
    private static final int ROOM_MIN = 1;
    private static final int ROOM_MAX = 32;
    // Quantite de salles a piliers injectees par secteur logique.
    private static final int NUM_PILLAR_ROOMS = 1;
    private static final int PILLAR_ROOM_MIN = 1;
    private static final int PILLAR_ROOM_MAX = 32;
    private static final int PILLAR_SPACING_MIN = 2;
    private static final int PILLAR_SPACING_MAX = 6;
    // Quantite de salles polygonales/custom injectees par secteur logique.
    private static final int NUM_CUSTOM_ROOMS = 1;
    private static final int MIN_NUM_SIDES = 2;
    private static final int MAX_NUM_SIDES = 8;
    private static final int MIN_CUSTOM_ROOM_RADIUS = 1;
    private static final int MAX_CUSTOM_ROOM_RADIUS = 16;
    // Modulo historique des neons, conserve comme parametre du coeur legacy.
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
        return generate(chunkX, chunkZ, layoutSeed, 0);
    }

    /**
     * Genere le layout logique complet d'un chunk pour un layer donne.
     *
     * <p>Cette methode ne pose aucun bloc. Elle fabrique uniquement l'etat
     * logique local qui sera ensuite traduit en colonnes puis en blocs par le
     * writer.
     *
     * @param chunkX coordonnee X du chunk
     * @param chunkZ coordonnee Z du chunk
     * @param layoutSeed seed deterministe du layer courant
     * @param layerIndex index du layer courant dans la pile verticale
     * @return layout calcule pour le chunk et le layer demandes
     */
    @SuppressWarnings("try")
    public static LevelZeroLayout generate(int chunkX, int chunkZ, long layoutSeed, int layerIndex) {
        try (ModPerformanceMonitor.Scope ignored = ModPerformanceMonitor.scope("level0.layout.generate")) {
            LevelZeroChunkSlice chunkSlice = regionGrid(layoutSeed, layerIndex).extractChunk(chunkX, chunkZ);
            return new LevelZeroLayout(
                    chunkSlice.walkable(),
                    chunkSlice.lighted(),
                    chunkSlice.surfaceBiomes(),
                    chunkSlice.largeRoom(),
                    chunkSlice.cellTags(),
                    chunkSlice);
        }
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
        // Ce point d'entree sert encore surtout aux lectures globales hors
        // contexte de layer. Tant qu'aucun layer explicite n'est fourni, on
        // garde volontairement le comportement legacy du layer 0.
        return regionGrid(layoutSeed, 0).sampleWalkableCell(
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

    private static LevelZeroRegionGrid regionGrid(long layoutSeed, int layerIndex) {
        // Le region grid encapsule tout le pipeline logique du Level 0 :
        // secteur -> walkability -> evaluation de cellules -> extraction chunk.
        return new LevelZeroRegionGrid(
                layoutSeed,
                layerIndex,
                SECTOR_COLS,
                SECTOR_ROWS,
                SECTOR_CACHE_CAPACITY,
                SECTOR_CACHE,
                SECTOR_GENERATOR,
                new LevelZeroLegacyLayoutPipeline(LIGHT_INTERVAL));
    }
}
