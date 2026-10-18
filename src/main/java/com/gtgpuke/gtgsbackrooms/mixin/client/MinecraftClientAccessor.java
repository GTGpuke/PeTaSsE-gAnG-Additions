package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;

/**
 * Echange controle du renderer courant, champ final, pendant le rendu d'un monde distant et a la bascule de monde ; le
 * monde courant, champ public, s'affecte directement.
 */
@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {

    @Mutable
    @Accessor("worldRenderer")
    void backrooms$setWorldRenderer(WorldRenderer worldRenderer);
}
