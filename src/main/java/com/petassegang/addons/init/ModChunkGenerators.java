package com.petassegang.addons.init;

import com.mojang.serialization.MapCodec;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import com.petassegang.addons.util.ModConstants;
import com.petassegang.addons.world.backrooms.level0.LevelZeroChunkGenerator;

/**
 * Registre des codecs de generateurs de chunks custom du mod.
 */
public final class ModChunkGenerators {

    /**
     * Enregistre le codec du generateur du Level 0.
     * Doit etre appele avant le chargement des dimensions.
     */
    public static void register() {
        Registry.register(
                Registries.CHUNK_GENERATOR,
                Identifier.of(ModConstants.MOD_ID, "backrooms_level_zero"),
                LevelZeroChunkGenerator.CODEC);
        ModConstants.LOGGER.debug("Codec du generateur de chunks enregistre.");
    }

    private ModChunkGenerators() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
