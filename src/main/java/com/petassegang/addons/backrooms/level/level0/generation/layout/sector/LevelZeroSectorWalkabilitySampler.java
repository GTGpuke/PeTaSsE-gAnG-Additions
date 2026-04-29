package com.petassegang.addons.backrooms.level.level0.generation.layout.sector;

import java.util.concurrent.ConcurrentHashMap;

import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroWalkabilitySampler;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroLegacyLayoutPipeline;

/**
 * Sampler canonique de traversabilite base sur les secteurs historiques.
 *
 * <p>Il fournit la forme brute du labyrinthe avant toute topologie fine,
 * micro-geometrie ou eclairage. C'est donc l'un des vrais points d'ancrage
 * "structurels" de la generation.
 */
public final class LevelZeroSectorWalkabilitySampler implements LevelZeroWalkabilitySampler {

    /** Demi-taille de la zone de securite forcee autour de l'origine. */
    private static final int ORIGIN_SAFE_RADIUS = 2;

    private final long layoutSeed;
    private final int sectorCols;
    private final int sectorRows;
    private final int sectorCacheCapacity;
    private final ConcurrentHashMap<Long, LevelZeroSectorData> sectorCache;
    private final LevelZeroSectorGenerator sectorGenerator;
    private final LevelZeroLegacyLayoutPipeline layoutPipeline;

    /**
     * Construit un sampler de traversabilite base sur le cache historique.
     *
     * @param layoutSeed seed de layout
     * @param sectorCols largeur d'un secteur logique
     * @param sectorRows hauteur d'un secteur logique
     * @param sectorCacheCapacity capacite maximale du cache partage
     * @param sectorCache cache partage des secteurs
     * @param sectorGenerator generateur historique de secteur
     * @param layoutPipeline pipeline explicite du layout legacy
     */
    public LevelZeroSectorWalkabilitySampler(long layoutSeed,
                                             int sectorCols,
                                             int sectorRows,
                                             int sectorCacheCapacity,
                                             ConcurrentHashMap<Long, LevelZeroSectorData> sectorCache,
                                             LevelZeroSectorGenerator sectorGenerator,
                                             LevelZeroLegacyLayoutPipeline layoutPipeline) {
        this.layoutSeed = layoutSeed;
        this.sectorCols = sectorCols;
        this.sectorRows = sectorRows;
        this.sectorCacheCapacity = sectorCacheCapacity;
        this.sectorCache = sectorCache;
        this.sectorGenerator = sectorGenerator;
        this.layoutPipeline = layoutPipeline;
    }

    @Override
    public boolean sampleWalkableCell(int cellX, int cellZ) {
        if (cellX >= -ORIGIN_SAFE_RADIUS && cellX <= ORIGIN_SAFE_RADIUS
                && cellZ >= -ORIGIN_SAFE_RADIUS && cellZ <= ORIGIN_SAFE_RADIUS) {
            // La zone centrale reste forcee ouverte pour garantir un point de
            // depart jouable et stable autour de l'origine.
            return true;
        }

        int sectorX = Math.floorDiv(cellX, sectorCols);
        int sectorZ = Math.floorDiv(cellZ, sectorRows);
        int localCellX = Math.floorMod(cellX, sectorCols);
        int localCellZ = Math.floorMod(cellZ, sectorRows);
        LevelZeroSectorData sector = getSector(sectorX, sectorZ);
        return sector.isWalkable(localCellX, localCellZ);
    }

    @Override
    public LevelZeroSectorRoomKind sampleRoomKindCell(int cellX, int cellZ) {
        if (cellX >= -ORIGIN_SAFE_RADIUS && cellX <= ORIGIN_SAFE_RADIUS
                && cellZ >= -ORIGIN_SAFE_RADIUS && cellZ <= ORIGIN_SAFE_RADIUS) {
            return LevelZeroSectorRoomKind.NONE;
        }

        int sectorX = Math.floorDiv(cellX, sectorCols);
        int sectorZ = Math.floorDiv(cellZ, sectorRows);
        int localCellX = Math.floorMod(cellX, sectorCols);
        int localCellZ = Math.floorMod(cellZ, sectorRows);
        LevelZeroSectorData sector = getSector(sectorX, sectorZ);
        return sector.roomKind(localCellX, localCellZ);
    }

    private LevelZeroSectorData getSector(int sectorX, int sectorZ) {
        long cacheKey = layoutPipeline.sampleSectorCacheKey(sectorX, sectorZ, layoutSeed);
        LevelZeroSectorData cached = sectorCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        LevelZeroSectorData generated = sectorGenerator.generate(sectorX, sectorZ, layoutSeed);
        // Le cache reste volontairement borne : on veut eviter qu'une longue
        // session transforme cette couche de confort en retention memoire.
        if (sectorCache.size() < sectorCacheCapacity) {
            sectorCache.putIfAbsent(cacheKey, generated);
        }
        return generated;
    }
}
