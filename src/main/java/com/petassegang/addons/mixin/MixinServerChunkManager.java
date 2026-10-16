package com.petassegang.addons.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import com.petassegang.addons.backrooms.portal.sync.PortalViewTracker;

/**
 * Relaie chaque changement de bloc serveur aux joueurs qui voient ce chunk a travers un portail.
 */
@Mixin(ServerChunkManager.class)
public abstract class MixinServerChunkManager {

    @Shadow @Final ServerWorld world;

    @Inject(method = "markForUpdate", at = @At("HEAD"))
    private void backrooms$notifyPortalViewers(BlockPos pos, CallbackInfo ci) {
        PortalViewTracker.onBlockChanged(this.world, pos);
    }
}
