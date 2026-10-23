package com.gtgpuke.gtgsbackrooms.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

import com.gtgpuke.gtgsbackrooms.core.ModConfig;
import com.gtgpuke.gtgsbackrooms.core.portal.transition.PortalCollision;

/**
 * La tete qui approche d'un cadre contre un bloc, ou y passe, ne voit pas l'interieur de ce bloc : un bloc cache par le
 * portail ({@link PortalCollision#hiddenFrom}) ne pose pas sa texture de mur sur l'ecran.
 */
@Mixin(InGameOverlayRenderer.class)
public abstract class MixinInGameOverlayRenderer {

    @ModifyExpressionValue(method = "getInWallBlockState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    private static BlockState backrooms$noWallBehindPortal(BlockState state, @Local BlockPos.Mutable pos,
            @Local(argsOnly = true) PlayerEntity player) {
        if (ModConfig.ENABLE_PORTAL_FEATURE && PortalCollision.hiddenFrom(player, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return state;
    }
}
