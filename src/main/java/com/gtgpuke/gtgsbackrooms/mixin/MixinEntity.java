package com.gtgpuke.gtgsbackrooms.mixin;

import java.util.List;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.core.ModConfig;
import com.gtgpuke.gtgsbackrooms.core.portal.transition.PortalCollision;

/**
 * Collision a travers les portails ouverts : les blocs de l'autre cote s'ajoutent aux collisions d'un deplacement,
 * celui du pas comme celui de la marche montee (les deux passent par cette methode).
 */
@Mixin(Entity.class)
public abstract class MixinEntity {

    @ModifyReturnValue(method = "findCollisionsForMovement", at = @At("RETURN"))
    private static List<VoxelShape> backrooms$collideThroughPortals(
            List<VoxelShape> collisions,
            @Local(argsOnly = true) @Nullable Entity entity,
            @Local(argsOnly = true) World world,
            @Local(argsOnly = true) Box movingBox) {
        if (entity == null || !ModConfig.ENABLE_PORTAL_FEATURE) {
            return collisions;
        }
        return PortalCollision.withOtherSide(entity, world, collisions, movingBox);
    }
}
