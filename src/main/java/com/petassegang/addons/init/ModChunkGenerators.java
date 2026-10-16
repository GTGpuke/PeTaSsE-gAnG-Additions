package com.petassegang.addons.init;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import com.petassegang.addons.backrooms.BackroomsConstants;
import com.petassegang.addons.backrooms.level0.generation.LevelZeroChunkGenerator;
import com.petassegang.addons.core.ModConstants;

/**
 * Registre des generateurs de chunks du mod.
 */
public final class ModChunkGenerators {

    private ModChunkGenerators() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Enregistre le codec du generateur du Level 0 ; a appeler avant le chargement des dimensions. */
    public static void register() {
        Registry.register(
                Registries.CHUNK_GENERATOR,
                Identifier.of(ModConstants.MOD_ID, BackroomsConstants.LEVEL_ZERO_CHUNK_GENERATOR),
                LevelZeroChunkGenerator.CODEC);
        ModConstants.LOGGER.debug("Generateur de chunks du Level 0 enregistre.");
    }
}
