package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.render.LightmapTextureManager;

/**
 * Force le recalcul de la lightmap pour le monde distant, puis de nouveau pour le monde principal.
 */
@Mixin(LightmapTextureManager.class)
public interface LightmapTextureManagerAccessor {

    @Accessor("dirty")
    void backrooms$setDirty(boolean dirty);
}
