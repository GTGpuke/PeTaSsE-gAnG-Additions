package com.gtgpuke.gtgsbackrooms.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.network.ChunkDataSender;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.PortalViewTracker;

/**
 * Apres une traversee de portail, vanilla propose au joueur tous les chunks de sa nouvelle dimension : ceux que
 * son client tient deja de la synchronisation des portails, a jour et deja construits, ne lui sont pas renvoyes.
 */
@Mixin(ChunkDataSender.class)
public abstract class MixinChunkDataSender {

    @Inject(method = "sendChunkData", at = @At("HEAD"), cancellable = true)
    private static void backrooms$skipChunksTheClientHolds(ServerPlayNetworkHandler handler, ServerWorld world, WorldChunk chunk, CallbackInfo ci) {
        if (PortalViewTracker.consumeHeldChunk(handler.player, chunk.getPos())) {
            ci.cancel();
        }
    }
}
