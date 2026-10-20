package com.gtgpuke.gtgsbackrooms.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gl.VertexBuffer;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.LazyVertexBuffers;

/**
 * Tampons de sommets crees pendant la creation d'un renderer de vue : leurs objets GL (tampon de sommets, tampon
 * d'indices, tableau de sommets) ne sont reserves qu'au premier usage, et non a la construction. Un tampon qui n'a
 * jamais servi n'a rien a liberer : vanilla ignore un identifiant nul a la fermeture, et ne tient pour ferme qu'un
 * tableau de sommets a moins un.
 */
@Mixin(VertexBuffer.class)
public abstract class MixinVertexBuffer {

    @Shadow
    private int vertexBufferId;

    @Shadow
    private int indexBufferId;

    @Shadow
    private int vertexArrayId;

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glGenBuffers()I"))
    private int backrooms$lazyBuffer(Operation<Integer> original) {
        return LazyVertexBuffers.active() ? 0 : original.call();
    }

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glGenVertexArrays()I"))
    private int backrooms$lazyVertexArray(Operation<Integer> original) {
        return LazyVertexBuffers.active() ? 0 : original.call();
    }

    /** Premier usage d'un tampon cree en differe : ses objets GL existent a partir d'ici. */
    @Inject(method = {"bind", "upload(Lnet/minecraft/client/render/BuiltBuffer;)V",
        "uploadIndexBuffer(Lnet/minecraft/client/util/BufferAllocator$CloseableBuffer;)V"}, at = @At("HEAD"))
    private void backrooms$allocateOnFirstUse(CallbackInfo ci) {
        if (this.vertexArrayId == 0) {
            this.vertexBufferId = GlStateManager._glGenBuffers();
            this.indexBufferId = GlStateManager._glGenBuffers();
            this.vertexArrayId = GlStateManager._glGenVertexArrays();
        }
    }
}
