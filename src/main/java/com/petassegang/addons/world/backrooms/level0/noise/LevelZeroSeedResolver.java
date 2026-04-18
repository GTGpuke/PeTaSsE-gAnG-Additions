package com.petassegang.addons.world.backrooms.level0.noise;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.noise.NoiseConfig;

import com.petassegang.addons.world.backrooms.BackroomsConstants;

/**
 * Point d'entree central pour retrouver les seeds deterministes du Level 0 a
 * partir du {@link NoiseConfig} fourni par Minecraft.
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
}
