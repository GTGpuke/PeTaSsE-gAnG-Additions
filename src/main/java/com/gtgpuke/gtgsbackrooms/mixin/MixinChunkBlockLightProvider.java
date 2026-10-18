package com.gtgpuke.gtgsbackrooms.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.BlockView;
import net.minecraft.world.chunk.light.ChunkBlockLightProvider;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.light.VirtualLightView;

/**
 * Sources de lumiere virtuelles (lumiere transmise par les portails) : la luminosite d'un bloc est la plus forte de la
 * sienne et de sa source virtuelle. C'est l'entree des sources des mises a jour d'un bloc ; la suite (colonne active,
 * propagation) reste celle de vanilla, cote serveur comme client. L'eclairage initial d'un chunk lit la luminosite des
 * blocs sans elle : une source virtuelle encore sans lumiere y est signalee de nouveau a la passe suivante.
 */
@Mixin(ChunkBlockLightProvider.class)
public abstract class MixinChunkBlockLightProvider {

    @ModifyExpressionValue(
            method = "getLightSourceLuminance",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;getLuminance()I"))
    private int backrooms$addVirtualSource(int luminance, @Local(argsOnly = true) long blockPos) {
        BlockView world = ((ChunkLightProviderAccessor) this).backrooms$getChunkProvider().getWorld();
        return world instanceof VirtualLightView view ? Math.max(luminance, view.backrooms$virtualLight().luminance(blockPos)) : luminance;
    }
}
