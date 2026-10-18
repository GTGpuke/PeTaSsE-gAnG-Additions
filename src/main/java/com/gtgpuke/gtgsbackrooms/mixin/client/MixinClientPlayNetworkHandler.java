package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.transition.ClientPortalCrossing;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.transition.EntityHandover;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.RemoteWorldRegistry;
import com.gtgpuke.gtgsbackrooms.config.ModConfig;
import com.gtgpuke.gtgsbackrooms.perf.ModPerformanceMonitor;

/**
 * Pendant une bascule de monde par portail, vanilla nettoie l'ancien monde du joueur : les chunks
 * decharges sont gardes (ils restent visibles a travers le portail de retour) et les entities detruites
 * le sont dans l'ancien monde, pas dans le nouveau. Hors bascule, une entity detruite en franchissant un portail reste
 * affichee jusqu'a l'arrivee de sa copie, et une entity qu'un portail de son monde vient de deplacer le passe telle
 * qu'elle est affichee (voir {@link EntityHandover}). Avant que vanilla ne cree un monde (connexion, respawn dans une
 * autre dimension), le renderer vanilla reprend sa place. Les chunks recus sont comptes pour la mesure des traversees. Les injections dans les
 * traitements de paquets se placent apres le renvoi sur le thread principal pour lire un etat coherent. Les sons qui
 * suivraient une entity d'un monde distant ne sont pas joues.
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
        } else if (ModConfig.ENABLE_PORTAL_FEATURE) {
            // Comme vanilla, sauf pour une entity qui franchit un portail : gardee jusqu'a l'arrivee de sa copie.
            EntityHandover.applyDestroys(((ClientPlayNetworkHandler) (Object) this).getWorld(), packet.getEntityIds());
            ci.cancel();
        }
    }

    /** Une entity teleportee par un portail de son monde passe le portail telle qu'elle est affichee, sans glisser. */
    @Inject(method = "onEntityPosition", at = @At(value = "INVOKE", target = FORCE_MAIN_THREAD, shift = At.Shift.AFTER))
    private void backrooms$teleportThroughPortal(EntityPositionS2CPacket packet, CallbackInfo ci) {
        if (ModConfig.ENABLE_PORTAL_FEATURE) {
            EntityHandover.onPositionPacket(((ClientPlayNetworkHandler) (Object) this).getWorld(), packet.getEntityId(),
                    packet.getX(), packet.getY(), packet.getZ());
        }
    }

    @Inject(method = "onChunkRenderDistanceCenter", at = @At(value = "INVOKE", target = FORCE_MAIN_THREAD, shift = At.Shift.AFTER))
    private void backrooms$endPreviousWorldCleanup(CallbackInfo ci) {
        ClientPortalCrossing.onVanillaChunkCenter();
    }

    /** Vanilla cree le monde d'une connexion avec le renderer courant : le sien d'abord. */
    @Inject(method = "onGameJoin", at = @At(value = "INVOKE", target = FORCE_MAIN_THREAD, shift = At.Shift.AFTER))
    private void backrooms$restoreRendererBeforeJoin(CallbackInfo ci) {
        RemoteWorldRegistry.restoreVanillaRenderer();
    }

    /**
     * Respawn dans une autre dimension : vanilla cree le monde d'arrivee avec le renderer courant, le sien d'abord. Dans
     * la meme dimension, vanilla garde son monde, et le renderer qui le dessine reste en place.
     */
    @Inject(method = "onPlayerRespawn", at = @At(value = "INVOKE", target = FORCE_MAIN_THREAD, shift = At.Shift.AFTER))
    private void backrooms$restoreRendererBeforeRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
        // Meme test que vanilla, sur le monde du joueur.
        if (packet.commonPlayerSpawnInfo().dimension() != MinecraftClient.getInstance().player.getWorld().getRegistryKey()) {
            RemoteWorldRegistry.restoreVanillaRenderer();
        }
    }

    /**
     * Les sons qui suivent une entity a son apparition (wagonnet, abeille) ne passent pas par un portail : pour une entity
     * d'un monde distant, ils sonneraient a ses coordonnees brutes, dans le monde du joueur. Ils ne sont pas joues.
     */
    @Inject(method = "playSpawnSound", at = @At("HEAD"), cancellable = true)
    private void backrooms$silenceRemoteSpawnSounds(Entity entity, CallbackInfo ci) {
        // Le champ client est declare dans la classe mere (ClientCommonNetworkHandler) : un @Shadow ne le trouverait pas.
        if (entity.getWorld() != MinecraftClient.getInstance().world) {
            ci.cancel();
        }
    }

    @Inject(method = "onChunkData", at = @At(value = "INVOKE", target = FORCE_MAIN_THREAD, shift = At.Shift.AFTER))
    private void backrooms$countReceivedChunk(CallbackInfo ci) {
        ModPerformanceMonitor.onChunkReceived();
    }
}
