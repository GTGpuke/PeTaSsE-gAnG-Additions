package com.petassegang.addons.mixin;

import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;

import com.petassegang.addons.util.ModConstants;

/**
 * Smoke test minimal pour valider le chargement Mixin sur Forge 26.1.
 */
@Mixin(MinecraftServer.class)
public final class SmokeTestMixin {

    @Unique
    private static boolean petassegang$loggedOnce;

    @Inject(method = "tickServer", at = @At("HEAD"), remap = false)
    private void petassegang$smokeTest(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (!petassegang$loggedOnce) {
            petassegang$loggedOnce = true;
            ModConstants.LOGGER.info("Smoke test Mixin charge.");
        }
    }
}
