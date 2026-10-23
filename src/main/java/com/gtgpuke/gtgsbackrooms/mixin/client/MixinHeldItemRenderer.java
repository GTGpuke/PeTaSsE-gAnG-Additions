package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;

import com.gtgpuke.gtgsbackrooms.core.portal.client.view.HandMask;

/** Le dessin de la main du joueur marque ses pixels dans le stencil sous un pack de shaders ({@link HandMask}). */
@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRenderer {

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;"
            + "Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("HEAD"))
    private void backrooms$beginHandMask(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
            ClientPlayerEntity player, int light, CallbackInfo ci) {
        HandMask.begin();
    }

    @Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;"
            + "Lnet/minecraft/client/network/ClientPlayerEntity;I)V", at = @At("RETURN"))
    private void backrooms$endHandMask(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
            ClientPlayerEntity player, int light, CallbackInfo ci) {
        HandMask.end();
    }
}
