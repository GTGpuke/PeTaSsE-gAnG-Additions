package com.petassegang.addons.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/**
 * Positionne et oriente la camera distante du rendu de portail.
 */
@Mixin(Camera.class)
public interface CameraAccessor {

    @Invoker("setPos")
    void backrooms$setPos(Vec3d pos);

    @Invoker("setRotation")
    void backrooms$setRotation(float yaw, float pitch);
}
