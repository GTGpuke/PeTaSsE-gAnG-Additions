package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.entity.EntityLookup;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.sound.FrameSoundPlayer;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.transition.EntityHandover;
import com.gtgpuke.gtgsbackrooms.config.ModConfig;

/**
 * Sons des mondes vus a travers un portail : ils passent par le portail qui les rapproche du joueur au lieu de sonner
 * a leurs coordonnees brutes (voir {@link FrameSoundPlayer}). Tous les sons positionnes d'un monde client aboutissent
 * a la meme methode privee, ceux des paquets du serveur comme ceux des blocs (feu, lave) et des entities. Une entity
 * renvoyee sous un autre identifiant remplace sa copie perimee (voir {@link EntityHandover#replaceStaleCopy}).
 */
@Mixin(ClientWorld.class)
public abstract class MixinClientWorld {

    @Shadow
    protected abstract EntityLookup<Entity> getEntityLookup();

    /** Avant l'index des identifiants uniques de vanilla, qui refuserait la nouvelle copie d'une entity deja presente. */
    @Inject(method = "addEntity", at = @At("HEAD"))
    private void backrooms$replaceStaleCopy(Entity entity, CallbackInfo ci) {
        if (!ModConfig.ENABLE_PORTAL_FEATURE) {
            return;
        }
        Entity stale = this.getEntityLookup().get(entity.getUuid());
        if (stale != null && stale.getId() != entity.getId()) {
            EntityHandover.replaceStaleCopy((ClientWorld) (Object) this, stale);
        }
    }

    @Inject(method = "playSound(DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFZJ)V", at = @At("HEAD"), cancellable = true)
    private void backrooms$soundThroughPortal(
            double x,
            double y,
            double z,
            SoundEvent event,
            SoundCategory category,
            float volume,
            float pitch,
            boolean useDistance,
            long seed,
            CallbackInfo ci) {
        if (FrameSoundPlayer.redirect((ClientWorld) (Object) this, x, y, z, event, category, volume, pitch, useDistance, seed)) {
            ci.cancel();
        }
    }

    /** Un son qui suivrait une entity d'un monde distant sonnerait a ses coordonnees brutes : il part de sa position actuelle. */
    @Inject(method = "playSoundFromEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void backrooms$entitySoundThroughPortal(Entity entity, SoundEvent sound, SoundCategory category, float volume, float pitch, CallbackInfo ci) {
        ClientWorld self = (ClientWorld) (Object) this;
        if (FrameSoundPlayer.redirect(self, entity.getX(), entity.getY(), entity.getZ(), sound, category, volume, pitch, false, self.random.nextLong())) {
            ci.cancel();
        }
    }
}
