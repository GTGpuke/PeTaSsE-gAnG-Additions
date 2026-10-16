package com.petassegang.addons.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;

/**
 * Echange controle du monde et du renderer courants pendant le rendu d'un monde distant.
 */
@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {

    @Mutable
    @Accessor("world")
    void backrooms$setWorld(ClientWorld world);

    @Mutable
    @Accessor("worldRenderer")
    void backrooms$setWorldRenderer(WorldRenderer worldRenderer);
}
