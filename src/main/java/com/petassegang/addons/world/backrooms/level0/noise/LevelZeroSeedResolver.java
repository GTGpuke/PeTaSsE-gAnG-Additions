package com.petassegang.addons.world.backrooms.level0.noise;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.noise.NoiseConfig;

import com.petassegang.addons.world.backrooms.BackroomsConstants;

/**
 * Point d'entree central pour retrouver les seeds deterministes du Level 0 a
 * partir du {@link NoiseConfig} fourni par Minecraft.
 *
 * <p>Cette classe evite que le {@code ChunkGenerator}, le layout ou les tests
 * recomposent chacun leur seed de leur cote. Elle sert de pont entre les
 * derives de Minecraft et les derives internes du Level 0.
 */
public final class LevelZeroSeedResolver {

    private LevelZeroSeedResolver() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Retourne la seed de layout du Level 0 pour le monde courant.
     *
     * @param noiseConfig configuration de bruit fournie par Minecraft
     * @return seed deterministe du layout
     */
    public static long resolveLayoutSeed(NoiseConfig noiseConfig) {
        return noiseConfig.getOrCreateRandomDeriver(BackroomsConstants.LEVEL_ZERO_LAYOUT_RANDOM)
                .split(BlockPos.ORIGIN)
                .nextLong();
    }

    /**
     * Retourne la seed de layout d'un layer vertical donne.
     *
     * @param noiseConfig configuration de bruit fournie par Minecraft
     * @param layerIndex index du layer
     * @return seed deterministe de layout pour ce layer
     */
    public static long resolveLayerLayoutSeed(NoiseConfig noiseConfig, int layerIndex) {
        return StageRandom.mixLegacy(resolveLayoutSeed(noiseConfig), StageRandom.Stage.LAYER_LAYOUT, layerIndex, 0L);
    }
}
