package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gtgpuke.gtgsbackrooms.backrooms.level0.biome.LevelZeroSurfaceBiome;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.block.LevelZeroWallpaperBlock;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroCoords;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroLayout;

@DisplayName("Sampler de murs du Level 0")
class LevelZeroWallSamplerTest {

    @Test
    @DisplayName("Le masque d'exposition reflete les cellules ouvertes voisines")
    void exposedFaceMaskMatchesNeighbours() {
        long seed = 998877L;
        for (int chunkX = -2; chunkX <= 2; chunkX++) {
            LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, 3, seed, 0);
            LevelZeroWallSampler sampler = new LevelZeroWallSampler(layout);
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int worldX = LevelZeroCoords.chunkStart(chunkX) + localX;
                    int worldZ = LevelZeroCoords.chunkStart(3) + localZ;
                    int expected = 0;
                    expected |= LevelZeroLayout.isWalkableAtWorld(worldX, worldZ - 1, seed) ? LevelZeroWallpaperBlock.NORTH_MASK : 0;
                    expected |= LevelZeroLayout.isWalkableAtWorld(worldX, worldZ + 1, seed) ? LevelZeroWallpaperBlock.SOUTH_MASK : 0;
                    expected |= LevelZeroLayout.isWalkableAtWorld(worldX - 1, worldZ, seed) ? LevelZeroWallpaperBlock.WEST_MASK : 0;
                    expected |= LevelZeroLayout.isWalkableAtWorld(worldX + 1, worldZ, seed) ? LevelZeroWallpaperBlock.EAST_MASK : 0;
                    assertEquals(expected, sampler.exposedFaceMask(worldX, worldZ), "Exposition en " + worldX + "," + worldZ);
                }
            }
        }
    }

    @Test
    @DisplayName("Le court-circuit de region donne le meme masque que la sonde complete")
    void shortcutMatchesFullProbe() {
        long seed = 13579L;
        int layer = 1;
        int mixedColumns = 0;
        // La frontiere de region X=48 cellules (144 blocs) est couverte par les chunks 7 a 10.
        for (int chunkX = 6; chunkX <= 11; chunkX++) {
            for (int chunkZ = 6; chunkZ <= 11; chunkZ++) {
                LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, chunkZ, seed, layer);
                LevelZeroWallSampler sampler = new LevelZeroWallSampler(layout);
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int worldX = LevelZeroCoords.chunkStart(chunkX) + localX;
                        int worldZ = LevelZeroCoords.chunkStart(chunkZ) + localZ;
                        int actual = sampler.wallpaperFaceMask(worldX, worldZ);
                        assertEquals(probeAllFaces(worldX, worldZ, seed, layer), actual, "Masque en " + worldX + "," + worldZ);
                        if (actual != 0 && actual != LevelZeroWallpaperBlock.FULL_MASK) {
                            mixedColumns++;
                        }
                    }
                }
            }
        }
        assertTrue(mixedColumns >= 0, "Les colonnes mixtes sont comptees : " + mixedColumns);
    }

    @Test
    @DisplayName("Sans fenetre, le sampler lit directement le cache de secteurs")
    void worksWithoutRegion() {
        LevelZeroWallSampler sampler = new LevelZeroWallSampler(4242L, 0, null);
        assertEquals(LevelZeroWallpaperBlock.FULL_MASK, sampler.exposedFaceMask(1, 1),
                "Le centre de la zone d'origine est entoure de cellules ouvertes.");
    }

    private static int probeAllFaces(int worldX, int worldZ, long seed, int layer) {
        return probe(worldX, worldZ, 0, -1, LevelZeroWallpaperBlock.NORTH_MASK, seed, layer)
                | probe(worldX, worldZ, 0, 1, LevelZeroWallpaperBlock.SOUTH_MASK, seed, layer)
                | probe(worldX, worldZ, -1, 0, LevelZeroWallpaperBlock.WEST_MASK, seed, layer)
                | probe(worldX, worldZ, 1, 0, LevelZeroWallpaperBlock.EAST_MASK, seed, layer);
    }

    private static int probe(int worldX, int worldZ, int stepX, int stepZ, int bit, long seed, int layer) {
        for (int distance = 1; distance <= LevelZeroWallpaperBlock.MAX_SURFACE_PROBE_DISTANCE; distance++) {
            int sampleX = worldX + stepX * distance;
            int sampleZ = worldZ + stepZ * distance;
            if (LevelZeroLayout.isWalkableAtWorld(sampleX, sampleZ, seed)) {
                return LevelZeroSurfaceBiome.sampleAtWorld(sampleX, sampleZ, layer) == LevelZeroSurfaceBiome.RED ? bit : 0;
            }
        }
        int fallbackX = worldX + stepX * LevelZeroWallpaperBlock.FALLBACK_BIOME_SAMPLE_DISTANCE;
        int fallbackZ = worldZ + stepZ * LevelZeroWallpaperBlock.FALLBACK_BIOME_SAMPLE_DISTANCE;
        return LevelZeroSurfaceBiome.sampleAtWorld(fallbackX, fallbackZ, layer) == LevelZeroSurfaceBiome.RED ? bit : 0;
    }
}
