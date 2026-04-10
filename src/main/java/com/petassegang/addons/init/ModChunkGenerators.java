package com.petassegang.addons.init;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.petassegang.addons.util.ModConstants;
import com.petassegang.addons.world.backrooms.level0.LevelZeroChunkGenerator;

/**
 * Registre des generateurs de chunks custom du mod.
 */
public final class ModChunkGenerators {

    /** DeferredRegister des codecs de generateurs de chunks. */
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, ModConstants.MOD_ID);

    /** Generateur du Level 0 des Backrooms. */
    public static final RegistryObject<MapCodec<LevelZeroChunkGenerator>> BACKROOMS_LEVEL_ZERO =
            CHUNK_GENERATORS.register("backrooms_level_zero", () -> LevelZeroChunkGenerator.CODEC);

    /**
     * Enregistre le registre sur le bus d'evenements du mod.
     *
     * @param modBusGroup le groupe de bus d'evenements du mod
     */
    public static void register(BusGroup modBusGroup) {
        CHUNK_GENERATORS.register(modBusGroup);
    }

    private ModChunkGenerators() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
