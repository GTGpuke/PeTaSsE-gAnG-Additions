package com.petassegang.addons.world.backrooms.level0.layout;

import java.util.concurrent.ConcurrentHashMap;

import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroCoords;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroLegacyRegionLayoutStage;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroLegacyRegionWalkabilityStage;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroLegacyLayoutPipeline;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroRegionContext;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroRegionStage;

/**
 * Facade regionale compatible avec le layout historique du Level 0.
 *
 * <p>Cette premiere version ne calcule pas encore une vraie region finie
 * conforme a la spec v6. Elle encapsule toutefois deja les lectures
 * regionales et l'extraction locale d'un chunk.
 */
public final class LevelZeroRegionGrid {

    private final long layoutSeed;
    private final int sectorCols;
    private final int sectorRows;
    private final LevelZeroSectorWalkabilitySampler walkabilitySampler;
    private final LevelZeroRegionStage<LevelZeroRegionLayout> regionLayoutStage;

    /**
     * Construit une facade regionale du layout historique.
     *
     * @param layoutSeed seed de layout
     * @param sectorCols largeur d'un secteur logique
     * @param sectorRows hauteur d'un secteur logique
     * @param lightInterval modulo historique des neons
     * @param sectorCacheCapacity capacite maximale du cache partage
     * @param sectorCache cache de secteurs partage
     * @param sectorGenerator generateur historique de secteur
     * @param layoutPipeline pipeline explicite du layout legacy
     */
    public LevelZeroRegionGrid(long layoutSeed,
                               int sectorCols,
                               int sectorRows,
                               int lightInterval,
                               int sectorCacheCapacity,
                               ConcurrentHashMap<Long, LevelZeroSectorData> sectorCache,
                               LevelZeroSectorGenerator sectorGenerator,
                               LevelZeroLegacyLayoutPipeline layoutPipeline) {
        this.layoutSeed = layoutSeed;
        this.sectorCols = sectorCols;
        this.sectorRows = sectorRows;
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
    public LevelZeroRegionLayout buildChunkRegionLayout(int chunkX, int chunkZ) {
        return regionLayoutStage.sample(new LevelZeroRegionContext(chunkCellWindow(chunkX, chunkZ), layoutSeed));
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

    /**
     * Vide le cache partage de secteurs.
     */
    public void clearCache() {
        walkabilitySampler.clearCache();
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
