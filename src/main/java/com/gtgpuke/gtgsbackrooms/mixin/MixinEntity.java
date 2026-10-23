package com.gtgpuke.gtgsbackrooms.mixin;

import java.util.List;
import java.util.function.Predicate;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.core.ModConfig;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.core.portal.transition.PortalCollision;

/**
 * Collision a travers les portails ouverts : les blocs de l'autre cote, et le sol d'un cadre d'arrivee au sol garde,
 * s'ajoutent aux collisions d'un deplacement, celui du pas comme celui de la marche montee (les deux passent par cette
 * methode). Les blocs qu'un portail cache a l'entity qui s'y engage ne l'etouffent pas.
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

    @ModifyArg(method = "isInsideWall", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;anyMatch(Ljava/util/function/Predicate;)Z"))
    private Predicate<BlockPos> backrooms$notInWallBehindPortal(Predicate<BlockPos> inWall) {
        Entity self = (Entity) (Object) this;
        BackroomsPortalEntity tunnel = ModConfig.ENABLE_PORTAL_FEATURE ? PortalCollision.tunnel(self, self.getBoundingBox()) : null;
        if (tunnel == null) {
            return inWall;
        }
        // Rare : une entity engagee dans un portail. Les blocs que ce portail lui cache ne l'etouffent pas.
        return cell -> !PortalCollision.hides(tunnel, cell.getX(), cell.getY(), cell.getZ()) && inWall.test(cell);
    }
}
