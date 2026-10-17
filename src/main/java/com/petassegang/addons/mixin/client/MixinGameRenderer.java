package com.petassegang.addons.mixin.client;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;

import com.petassegang.addons.backrooms.portal.client.interaction.RemoteTargeting;
import com.petassegang.addons.backrooms.portal.client.transition.ClientPortalCrossing;

/**
 * Avant chaque rendu du monde : si la camera vient de franchir un portail, le client bascule de monde
 * tout de suite, et l'image qui suit est deja celle de l'autre cote. Apres le raycast de visee : si le
 * regard traverse un portail avant sa cible, la cible est cherchee de l'autre cote.
 */
@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void backrooms$predictPortalCrossing(RenderTickCounter tickCounter, CallbackInfo ci) {
        ClientPortalCrossing.predictCrossing(tickCounter.getTickDelta(true));
    }

    @Inject(method = "updateCrosshairTarget", at = @At("TAIL"))
    private void backrooms$targetThroughPortal(float tickDelta, CallbackInfo ci) {
        RemoteTargeting.update(this.client, tickDelta);
    }
}
