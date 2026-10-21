package com.gtgpuke.gtgsbackrooms.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;

import com.gtgpuke.gtgsbackrooms.core.portal.client.interaction.RemoteActions;

/**
 * Clics du joueur : quand il vise a travers un portail, frapper, miner et utiliser partent au serveur avec
 * la cible de l'autre cote, et vanilla ne touche pas a ce qui est derriere le cadre dans son propre monde. Le clic
 * molette choisit le bloc ou la creature de l'autre cote.
 */
@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {

    /** Clic molette : le bloc ou la creature vise de l'autre cote du cadre est choisi comme s'il etait la. */
    @WrapMethod(method = "doItemPick")
    private void backrooms$pickThroughPortal(Operation<Void> original) {
        RemoteActions.pick((MinecraftClient) (Object) this, () -> original.call());
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void backrooms$attackThroughPortal(CallbackInfoReturnable<Boolean> cir) {
        if (RemoteActions.attack((MinecraftClient) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
    private void backrooms$mineThroughPortal(boolean breaking, CallbackInfo ci) {
        if (RemoteActions.handleBlockBreaking((MinecraftClient) (Object) this, breaking)) {
            ci.cancel();
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void backrooms$useThroughPortal(CallbackInfo ci) {
        if (RemoteActions.use((MinecraftClient) (Object) this)) {
            ci.cancel();
        }
    }
}
