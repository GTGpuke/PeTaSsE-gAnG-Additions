package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.LightData;

/**
 * Fait suivre au gestionnaire de paquets le monde ou se trouve le joueur apres une bascule par portail, et lui fait
 * lire la lumiere d'un chunk distant comme celle d'un chunk vanilla.
 */
@Mixin(ClientPlayNetworkHandler.class)
public interface ClientPlayNetworkHandlerAccessor {

    @Accessor("world")
    void backrooms$setWorld(ClientWorld world);

    @Accessor("worldProperties")
    void backrooms$setWorldProperties(ClientWorld.Properties properties);

    /**
     * Lumiere d'un chunk, lue comme celle d'un chunk vanilla : les mods qui suivent l'arrivee des chunks par la (un mod
     * de rendu ne construit une section qu'apres sa lumiere) voient ceux des mondes distants ({@code RemoteWorld.loadChunk}).
     * Le gestionnaire lit son monde : a basculer sur le monde distant le temps de l'appel.
     */
    @Invoker("readLightData")
    void backrooms$readLightData(int chunkX, int chunkZ, LightData lightData);
}
