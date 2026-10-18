package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.render.Frustum;

/** Recalcul des plans d'un frustum existant : vanilla ne l'expose que par le constructeur, qui alloue. */
@Mixin(Frustum.class)
public interface FrustumAccessor {

    @Invoker("init")
    void backrooms$init(Matrix4f positionMatrix, Matrix4f projectionMatrix);
}
