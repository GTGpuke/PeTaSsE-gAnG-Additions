package com.petassegang.addons.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.LimbAnimator;

/**
 * Etat complet du balancement des membres, repris par l'entity que le serveur recree de l'autre cote d'un portail.
 */
@Mixin(LimbAnimator.class)
public interface LimbAnimatorAccessor {

    @Accessor("prevSpeed")
    float backrooms$getPrevSpeed();

    @Accessor("prevSpeed")
    void backrooms$setPrevSpeed(float prevSpeed);

    @Accessor("pos")
    void backrooms$setPos(float pos);
}
