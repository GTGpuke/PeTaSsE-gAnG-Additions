package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.sector;

import java.util.LinkedHashMap;
import java.util.Map;

import com.gtgpuke.gtgsbackrooms.core.perf.ModPerformanceMonitor;

/**
 * Cache LRU borne des secteurs generes, partage entre threads de generation.
 *
 * <p>Un secteur couvre 720x405 blocs et coute plusieurs millisecondes a
 * generer ; un joueur qui explore n'en touche que quelques dizaines par layer.
 * Le cache evince les plus anciens au lieu de cesser de cacher une fois plein.
 * Les lectures sont synchronisees : un layout n'interroge le cache qu'une fois
 * par secteur touche, le verrou n'est donc jamais chaud.
 */
public final class LevelZeroSectorCache {

    /** Nombre maximal de secteurs conserves, tous layers confondus. */
    public static final int DEFAULT_CAPACITY = 256;

    private final LevelZeroSectorGenerator generator;
    private final LinkedHashMap<Key, LevelZeroSectorData> sectors;

    /**
     * Construit un cache de secteurs.
     *
     * @param generator generateur de secteur
     * @param capacity nombre maximal de secteurs conserves
     */
    public LevelZeroSectorCache(LevelZeroSectorGenerator generator, int capacity) {
        this.generator = generator;
        this.sectors = new LinkedHashMap<>(capacity * 2, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Key, LevelZeroSectorData> eldest) {
                return size() > capacity;
            }
        };
    }

    /** Parametres du generateur sous-jacent. */
    public LevelZeroSectorGenerator.Settings settings() {
        return generator.settings();
    }

    /**
     * Retourne un secteur, en le generant s'il n'est pas encore en cache.
     *
     * @param layoutSeed seed de layout du layer
     * @param sectorX coordonnee secteur X
     * @param sectorZ coordonnee secteur Z
     * @return donnees du secteur
     */
    @SuppressWarnings("try")
    public LevelZeroSectorData sector(long layoutSeed, int sectorX, int sectorZ) {
        Key key = new Key(layoutSeed, sectorX, sectorZ);
        synchronized (sectors) {
            LevelZeroSectorData cached = sectors.get(key);
            if (cached != null) {
                return cached;
            }
        }
        // Generation hors verrou : deux threads peuvent generer le meme secteur en meme temps,
        // le resultat etant deterministe, le second ecrase le premier sans consequence.
        LevelZeroSectorData generated;
        try (ModPerformanceMonitor.Scope ignored = ModPerformanceMonitor.scope("level0.sector.generate")) {
            generated = generator.generate(sectorX, sectorZ, layoutSeed);
        }
        synchronized (sectors) {
            sectors.put(key, generated);
        }
        return generated;
    }

    /** Nombre de secteurs actuellement en cache. */
    public int size() {
        synchronized (sectors) {
            return sectors.size();
        }
    }

    /** Vide le cache (arret du serveur). */
    public void clear() {
        synchronized (sectors) {
            sectors.clear();
        }
    }

    private record Key(long layoutSeed, int sectorX, int sectorZ) {
    }
}
