package com.petassegang.addons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.ChunkLightProvider;

/**
 * Fournisseur de chunks d'un moteur de lumiere, donc son monde : le champ est declare dans la classe mere du moteur de la
 * lumiere des blocs, hors de portee d'un {@code @Shadow}.
 */
@Mixin(ChunkLightProvider.class)
public interface ChunkLightProviderAccessor {

    @Accessor("chunkProvider")
    ChunkProvider backrooms$getChunkProvider();
}
