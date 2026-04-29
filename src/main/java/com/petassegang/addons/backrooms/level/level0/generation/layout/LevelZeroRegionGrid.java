package com.petassegang.addons.backrooms.level.level0.generation.layout;

import java.util.concurrent.ConcurrentHashMap;

import com.petassegang.addons.perf.section.ModPerformanceMonitor;
import com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroCoords;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorData;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorGenerator;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorWalkabilitySampler;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroLegacyLayoutPipeline;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroRegionContext;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroRegionStage;
import com.petassegang.addons.backrooms.level.level0.generation.stage.region.LevelZeroLegacyRegionLayoutStage;
import com.petassegang.addons.backrooms.level.level0.generation.stage.region.LevelZeroLegacyRegionWalkabilityStage;

/**
 * Facade regionale du Level 0 entre la generation sectorielle et l'extraction
 * par chunk.
 *
 * <p>Son role est de construire une vue regionale deterministe, assez large
 * pour qu'un chunk puisse etre evalue avec son contexte immediat, sans
 * recalculer brutalement toute la generation bloc par bloc.
 *
 * <p>En pratique :
 *
 * <ul>
 *   <li>le package {@code layout/sector} fournit la source brute de
 *   traversabilite et de types de salles ;</li>
 *   <li>la facade regionale applique ensuite la pipeline logique sur une
 *   fenetre de cellules ;</li>
 *   <li>le chunk final n'est extrait qu'a la toute fin.</li>
 * </ul>
 *
 * <p>Cette facade va dans la direction de la spec v6, mais il faut la lire
 * comme l'implementation active du projet, pas comme une promesse abstraite :
 * elle est deja responsable aujourd'hui de la construction regionale utile au
 * runtime courant.
 */
public final class LevelZeroRegionGrid {

    private final long layoutSeed;
    private final int layerIndex;
    private final LevelZeroSectorWalkabilitySampler walkabilitySampler;
    private final LevelZeroRegionStage<LevelZeroRegionLayout> regionLayoutStage;

    /**
     * Construit une facade regionale du layout historique.
     *
     * @param layoutSeed seed de layout
     * @param sectorCols largeur d'un secteur logique
     * @param sectorRows hauteur d'un secteur logique
     * @param sectorCacheCapacity capacite maximale du cache partage
     * @param sectorCache cache de secteurs partage
     * @param sectorGenerator generateur historique de secteur
     * @param layoutPipeline pipeline explicite du layout legacy
     */
    public LevelZeroRegionGrid(long layoutSeed,
                               int sectorCols,
                               int sectorRows,
                               int sectorCacheCapacity,
                               ConcurrentHashMap<Long, LevelZeroSectorData> sectorCache,
                               LevelZeroSectorGenerator sectorGenerator,
                               LevelZeroLegacyLayoutPipeline layoutPipeline) {
        this(layoutSeed,
                0,
                sectorCols,
                sectorRows,
                sectorCacheCapacity,
                sectorCache,
                sectorGenerator,
                layoutPipeline);
    }

    public LevelZeroRegionGrid(long layoutSeed,
                               int layerIndex,
                               int sectorCols,
                               int sectorRows,
                               int sectorCacheCapacity,
                               ConcurrentHashMap<Long, LevelZeroSectorData> sectorCache,
                               LevelZeroSectorGenerator sectorGenerator,
                               LevelZeroLegacyLayoutPipeline layoutPipeline) {
        this.layoutSeed = layoutSeed;
        this.layerIndex = layerIndex;
        // Le sampler secteur fournit la source de verite "brute" de
        // traversabilite pour ce layoutSeed et ce layer. La suite de la facade
        // n'ajoute que des couches semantiques au-dessus de cette base.
        this.walkabilitySampler = new LevelZeroSectorWalkabilitySampler(
                layoutSeed,
                sectorCols,
                sectorRows,
                sectorCacheCapacity,
                sectorCache,
                sectorGenerator,
                layoutPipeline);
        this.regionLayoutStage = new LevelZeroLegacyRegionLayoutStage(
                new LevelZeroRegionLayoutBuilder(layoutPipeline),
                new LevelZeroLegacyRegionWalkabilityStage(walkabilitySampler));
    }

    /**
     * Extrait un layout de chunk complet depuis la facade regionale.
     *
     * @param chunkX coordonnee X du chunk
     * @param chunkZ coordonnee Z du chunk
     * @return slice mutable contenant les donnees du chunk
     */
    public LevelZeroChunkSlice extractChunk(int chunkX, int chunkZ) {
        return buildChunkRegionLayout(chunkX, chunkZ).extractChunk(chunkX, chunkZ);
    }

    /**
     * Construit la region canonique minimale necessaire a un chunk.
     *
     * @param chunkX coordonnee X du chunk
     * @param chunkZ coordonnee Z du chunk
     * @return region layout couvrant toutes les cellules du chunk
     */
    @SuppressWarnings("try")
    public LevelZeroRegionLayout buildChunkRegionLayout(int chunkX, int chunkZ) {
        try (ModPerformanceMonitor.Scope ignored =
                     ModPerformanceMonitor.scope("level0.region_grid.build_chunk_region_layout")) {
            // On ne construit ici qu'une fenetre regionale minimale couvrant les
            // cellules necessaires au chunk demande. L'extraction bloc-par-bloc se
            // fera seulement dans l'etape suivante, a partir de la region evaluee.
            return regionLayoutStage.sample(new LevelZeroRegionContext(chunkCellWindow(chunkX, chunkZ), layoutSeed, layerIndex));
        }
    }

    /**
     * Echantillonne directement la traversabilite d'une cellule logique.
     *
     * @param cellX coordonnee cellule X
     * @param cellZ coordonnee cellule Z
     * @return {@code true} si la cellule est ouverte
     */
    public boolean sampleWalkableCell(int cellX, int cellZ) {
        return walkabilitySampler.sampleWalkableCell(cellX, cellZ);
    }

    private static LevelZeroChunkCellWindow chunkCellWindow(int chunkX, int chunkZ) {
        int worldMinX = LevelZeroCoords.chunkStartX(chunkX);
        int worldMinZ = LevelZeroCoords.chunkStartZ(chunkZ);
        int worldMaxX = LevelZeroCoords.chunkEndX(chunkX);
        int worldMaxZ = LevelZeroCoords.chunkEndZ(chunkZ);
        return new LevelZeroChunkCellWindow(
                chunkX,
                chunkZ,
                LevelZeroCoords.worldToCellX(worldMinX),
                LevelZeroCoords.worldToCellZ(worldMinZ),
                LevelZeroCoords.worldToCellX(worldMaxX),
                LevelZeroCoords.worldToCellZ(worldMaxZ));
    }
}
