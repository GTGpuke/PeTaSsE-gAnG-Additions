package com.petassegang.addons.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;

import com.petassegang.addons.backrooms.portal.client.interaction.CrossPortalActions;

/**
 * Clics du joueur : quand il vise a travers un portail, frapper, miner et utiliser partent au serveur avec
 * la cible de l'autre cote, et vanilla ne touche pas a ce qui est derriere le cadre dans son propre monde.
 */
@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void backrooms$attackThroughPortal(CallbackInfoReturnable<Boolean> cir) {
        if (CrossPortalActions.attack((MinecraftClient) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void backrooms$mineThroughPortal(boolean breaking, CallbackInfo ci) {
        if (CrossPortalActions.handleBlockBreaking((MinecraftClient) (Object) this, breaking)) {
            ci.cancel();
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void backrooms$useThroughPortal(CallbackInfo ci) {
        if (CrossPortalActions.use((MinecraftClient) (Object) this)) {
            ci.cancel();
        }
    }
}
