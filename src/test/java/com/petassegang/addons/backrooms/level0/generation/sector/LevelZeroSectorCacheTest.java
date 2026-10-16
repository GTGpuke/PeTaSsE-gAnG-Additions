package com.petassegang.addons.backrooms.level0.generation.sector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cache de secteurs du Level 0")
class LevelZeroSectorCacheTest {

    private static final LevelZeroSectorGenerator.Settings TINY = new LevelZeroSectorGenerator.Settings(
            16, 16, 0.5D, 4, 0.5D, 1, 2, 4, 0, 2, 4, 2, 2, 0, 3, 4, 2, 3);

    @Test
    @DisplayName("Un secteur deja genere est rendu depuis le cache")
    void returnsCachedInstance() {
        LevelZeroSectorCache cache = new LevelZeroSectorCache(new LevelZeroSectorGenerator(TINY), 8);
        LevelZeroSectorData first = cache.sector(1L, 0, 0);
        assertSame(first, cache.sector(1L, 0, 0), "Le meme secteur doit etre rendu sans regeneration.");
        assertEquals(1, cache.size(), "Un seul secteur en cache.");
    }

    @Test
    @DisplayName("Seed, X et Z distinguent les entrees")
    void keyIncludesSeedAndCoordinates() {
        LevelZeroSectorCache cache = new LevelZeroSectorCache(new LevelZeroSectorGenerator(TINY), 8);
        LevelZeroSectorData base = cache.sector(1L, 0, 0);
        assertNotSame(base, cache.sector(2L, 0, 0), "Une autre seed est une autre entree.");
        assertNotSame(base, cache.sector(1L, 1, 0), "Un autre X est une autre entree.");
        assertNotSame(base, cache.sector(1L, 0, 1), "Un autre Z est une autre entree.");
        assertEquals(4, cache.size(), "Quatre entrees distinctes.");
    }

    @Test
    @DisplayName("Le cache evince les secteurs les moins recemment utilises")
    void evictsLeastRecentlyUsed() {
        LevelZeroSectorCache cache = new LevelZeroSectorCache(new LevelZeroSectorGenerator(TINY), 2);
        LevelZeroSectorData a = cache.sector(1L, 0, 0);
        cache.sector(1L, 1, 0);
        cache.sector(1L, 0, 0);
        cache.sector(1L, 2, 0);
        assertEquals(2, cache.size(), "La capacite est respectee.");
        assertSame(a, cache.sector(1L, 0, 0), "Le secteur relu recemment est conserve.");
    }

    @Test
    @DisplayName("Vider le cache supprime toutes les entrees")
    void clearEmptiesCache() {
        LevelZeroSectorCache cache = new LevelZeroSectorCache(new LevelZeroSectorGenerator(TINY), 8);
        cache.sector(1L, 0, 0);
        cache.clear();
        assertEquals(0, cache.size(), "Plus aucun secteur apres clear.");
    }
}
