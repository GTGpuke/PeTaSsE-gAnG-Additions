package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation;

import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroCoords;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroVerticalSlice;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroCellEvaluation;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroLayout;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.noise.LevelZeroSeedResolver;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.write.LevelZeroWallProps;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.write.LevelZeroWallSampler;

/**
 * Empreinte deterministe de la generation logique d'une zone, pour les tests de non-regression.
 *
 * <p>L'empreinte couvre, bloc par bloc, tout ce que le writer lit : evaluation
 * de cellule, exposition, masque de papier peint et details muraux.
 */
final class LevelZeroGoldenHash {

    static final int RADIUS_CHUNKS = 3;

    private LevelZeroGoldenHash() {
    }

    /**
     * Empreinte d'un layer pour une seed monde, sur les chunks de -RADIUS a +RADIUS.
     *
     * @param worldSeed seed de layout du monde
     * @param layerIndex index du layer
     * @return empreinte 64 bits
     */
    static long hash(long worldSeed, int layerIndex) {
        long layerSeed = LevelZeroSeedResolver.resolveLayerSeed(worldSeed, layerIndex);
        LevelZeroVerticalSlice slice = LevelZeroVerticalSlice.of(layerIndex);
        long hash = 17L;
        for (int chunkX = -RADIUS_CHUNKS; chunkX <= RADIUS_CHUNKS; chunkX++) {
            for (int chunkZ = -RADIUS_CHUNKS; chunkZ <= RADIUS_CHUNKS; chunkZ++) {
                LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, chunkZ, layerSeed, layerIndex);
                LevelZeroWallSampler sampler = new LevelZeroWallSampler(layout);
                for (int localX = 0; localX < LevelZeroCoords.CHUNK_SIZE; localX++) {
                    for (int localZ = 0; localZ < LevelZeroCoords.CHUNK_SIZE; localZ++) {
                        int worldX = LevelZeroCoords.chunkStart(chunkX) + localX;
                        int worldZ = LevelZeroCoords.chunkStart(chunkZ) + localZ;
                        LevelZeroCellEvaluation cell = layout.cellAt(localX, localZ);
                        hash = hash * 31 + (cell.walkable() ? 1 : 0);
                        hash = hash * 31 + cell.topology().ordinal();
                        hash = hash * 31 + cell.connectionMask();
                        hash = hash * 31 + cell.geometryMask();
                        hash = hash * 31 + cell.microPattern();
                        hash = hash * 31 + cell.surfaceBiome().ordinal();
                        hash = hash * 31 + cell.roomKind().ordinal();
                        hash = hash * 31 + (cell.largeRoom() ? 1 : 0);
                        hash = hash * 31 + (layout.hasLight(localX, localZ) ? 1 : 0);
                        if (layout.isLocallyWalkable(localX, localZ)) {
                            continue;
                        }
                        int exposed = sampler.exposedFaceMask(worldX, worldZ);
                        hash = hash * 31 + exposed;
                        if (exposed != 0) {
                            hash = hash * 31 + sampler.wallpaperFaceMask(worldX, worldZ);
                            LevelZeroWallProps props = LevelZeroWallProps.resolve(worldX, worldZ, exposed, slice);
                            hash = hash * 31 + props.baseboardFaceMask();
                            hash = hash * 31 + props.fixture().ordinal();
                            hash = hash * 31 + props.fixtureFaceMask();
                            hash = hash * 31 + props.fixtureY();
                        }
                    }
                }
            }
        }
        return hash;
    }
}
