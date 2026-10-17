package com.petassegang.addons.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.server.world.ServerWorld;

import com.petassegang.addons.backrooms.portal.sync.PortalViewTracker;
import com.petassegang.addons.config.ModConfig;

/**
 * Particules diffusees par le serveur (coups critiques, explosions, fumee) : elles partent aussi aux joueurs qui voient
 * la dimension a travers un portail, et s'affichent dans la vue.
 */
@Mixin(ServerWorld.class)
public abstract class MixinServerWorld {

    @ModifyExpressionValue(
            method = "spawnParticles(Lnet/minecraft/particle/ParticleEffect;DDDIDDDD)I",
            at = @At(value = "NEW", target = "Lnet/minecraft/network/packet/s2c/play/ParticleS2CPacket;"))
    private ParticleS2CPacket backrooms$relayToPortalViewers(ParticleS2CPacket packet) {
        if (ModConfig.ENABLE_PORTAL_FEATURE) {
            ServerWorld self = (ServerWorld) (Object) this;
            PortalViewTracker.relayToViewers(self.getRegistryKey(), packet.getX(), packet.getZ(), packet);
        }
        return packet;
    }
}
