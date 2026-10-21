package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.Util;
import net.minecraft.world.BlockView;

import com.gtgpuke.gtgsbackrooms.core.portal.client.view.CrossingRoll;

/** Apres son orientation par vanilla, la camera du joueur prend ce qui reste du roulis d'une traversee ({@link CrossingRoll}). */
@Mixin(Camera.class)
public abstract class MixinCamera {

    @Inject(method = "update", at = @At("TAIL"))
    private void backrooms$crossingRoll(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta,
            CallbackInfo ci) {
        Camera self = (Camera) (Object) this;
        if (CrossingRoll.active() && self == MinecraftClient.getInstance().gameRenderer.getCamera()) {
            CrossingRoll.apply(self, Util.getMeasuringTimeMs());
        }
    }
}
