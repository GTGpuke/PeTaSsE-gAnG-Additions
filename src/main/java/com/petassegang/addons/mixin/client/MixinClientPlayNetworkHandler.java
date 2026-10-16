package com.petassegang.addons.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;

import com.petassegang.addons.backrooms.portal.client.transition.ClientPortalCrossing;

/**
 * Pendant une bascule de monde par portail, vanilla nettoie l'ancien monde du joueur : les chunks
 * decharges sont gardes (ils restent visibles a travers le portail de retour) et les entities detruites
 * le sont dans l'ancien monde, pas dans le nouveau. Les injections se placent apres le renvoi sur le
 * thread principal pour lire un etat coherent.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {

    private static final String FORCE_MAIN_THREAD = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread("
            + "Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V";

    @Inject(method = "onUnloadChunk", at = @At(value = "INVOKE", target = FORCE_MAIN_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void backrooms$keepPreviousWorldChunks(UnloadChunkS2CPacket packet, CallbackInfo ci) {
        if (ClientPortalCrossing.swallowsVanillaUnloads()) {
            ci.cancel();
        }
    }

    @Inject(method = "onEntitiesDestroy", at = @At(value = "INVOKE", target = FORCE_MAIN_THREAD, shift = At.Shift.AFTER), cancellable = true)
    private void backrooms$destroyEntitiesInPreviousWorld(EntitiesDestroyS2CPacket packet, CallbackInfo ci) {
        if (ClientPortalCrossing.redirectDestroyedEntities(packet.getEntityIds())) {
            ci.cancel();
        }
    }

    @Inject(method = "onChunkRenderDistanceCenter", at = @At(value = "INVOKE", target = FORCE_MAIN_THREAD, shift = At.Shift.AFTER))
    private void backrooms$endPreviousWorldCleanup(ChunkRenderDistanceCenterS2CPacket packet, CallbackInfo ci) {
        ClientPortalCrossing.onVanillaChunkCenter();
    }
}
