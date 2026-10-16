package com.petassegang.addons.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;

/**
 * Fait suivre au gestionnaire de paquets le monde ou se trouve le joueur apres une bascule par portail.
 */
@Mixin(ClientPlayNetworkHandler.class)
public interface ClientPlayNetworkHandlerAccessor {

    @Accessor("world")
    void backrooms$setWorld(ClientWorld world);

    @Accessor("worldProperties")
    void backrooms$setWorldProperties(ClientWorld.Properties properties);
}
