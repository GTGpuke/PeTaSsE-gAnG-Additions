package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;

/**
 * Renderer avec lequel vanilla a cree un monde client : c'est lui qui recoit ses mises a jour de blocs.
 */
@Mixin(ClientWorld.class)
public interface ClientWorldAccessor {

    @Accessor("worldRenderer")
    WorldRenderer backrooms$getWorldRenderer();

    /** Gestionnaire de paquets du monde : celui de la connexion, pour chaque monde distant comme pour le principal. */
    @Accessor("networkHandler")
    ClientPlayNetworkHandler backrooms$getNetworkHandler();
}
