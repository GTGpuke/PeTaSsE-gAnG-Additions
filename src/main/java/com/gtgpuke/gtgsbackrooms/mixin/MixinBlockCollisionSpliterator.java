package com.gtgpuke.gtgsbackrooms.mixin;

import java.util.function.BiFunction;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockCollisionSpliterator;
import net.minecraft.world.CollisionView;

import com.gtgpuke.gtgsbackrooms.core.ModConfig;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.core.portal.transition.PortalCollision;

/**
 * Collisions de blocs d'une entity engagee dans un portail : les blocs de son cote caches derriere le plan
 * ({@link PortalCollision#hides}) n'y sont pas. Toutes les requetes passent par ce parcours (deplacement, place libre,
 * poussee hors des blocs, controle des pas d'un joueur par le serveur) : le bloc n'existe pour aucune.
 */
@Mixin(BlockCollisionSpliterator.class)
public abstract class MixinBlockCollisionSpliterator {

    @Shadow
    @Final
    private BlockPos.Mutable pos;

    /** Portail ou l'entity de la requete est engagee, cherche une fois a la creation du parcours ; {@code null} sinon, ou sans entity. */
    @Unique
    private @Nullable BackroomsPortalEntity backrooms$tunnel;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void backrooms$findTunnel(CollisionView world, @Nullable Entity entity, Box box, boolean forEntity,
            BiFunction<BlockPos.Mutable, VoxelShape, ?> resultFunction, CallbackInfo ci) {
        this.backrooms$tunnel = entity != null && ModConfig.ENABLE_PORTAL_FEATURE ? PortalCollision.tunnel(entity, box) : null;
    }

    @ModifyExpressionValue(method = "computeNext", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/block/BlockState;getCollisionShape(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;"
                    + "Lnet/minecraft/block/ShapeContext;)Lnet/minecraft/util/shape/VoxelShape;"))
    private VoxelShape backrooms$hideBehindPortal(VoxelShape shape) {
        BackroomsPortalEntity tunnel = this.backrooms$tunnel;
        if (tunnel == null || shape.isEmpty() || !PortalCollision.hides(tunnel, this.pos.getX(), this.pos.getY(), this.pos.getZ())) {
            return shape;
        }
        return VoxelShapes.empty();
    }
}
