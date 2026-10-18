package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.noise;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.noise.NoiseConfig;

import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants;

/**
 * Derive les seeds du Level 0 a partir du {@link NoiseConfig} fourni par Minecraft.
 *
 * <p>Le seed monde n'est jamais lu directement : on passe par le random deriver
 * nomme de la dimension, ce qui isole le Level 0 des autres generateurs.
 */
public final class LevelZeroSeedResolver {

    private LevelZeroSeedResolver() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Seed de layout du Level 0 pour le monde courant.
     *
     * @param noiseConfig configuration de bruit du monde
     * @return seed deterministe du layout
     */
    public static long resolveLayoutSeed(NoiseConfig noiseConfig) {
        return noiseConfig.getOrCreateRandomDeriver(BackroomsConstants.LEVEL_ZERO_LAYOUT_RANDOM)
                .split(BlockPos.ORIGIN)
                .nextLong();
    }

    /**
     * Seed de layout d'un layer donne, derive du seed de layout du monde.
     *
     * @param layoutSeed seed de layout du monde
     * @param layerIndex index du layer
     * @return seed deterministe du layer
     */
    public static long resolveLayerSeed(long layoutSeed, int layerIndex) {
        return StageRandom.mix(layoutSeed, StageRandom.Stage.LAYER_LAYOUT, layerIndex, 0L);
    }
}
