package com.petassegang.addons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.level.lighting.LayerLightSectionStorage;

/**
 * Pont d'acces minimal vers le stockage interne du block light engine.
 */
@Mixin(value = LayerLightSectionStorage.class, remap = false)
interface BlockLightSectionStorageAccessor {

    @Invoker("storingLightForSection")
    boolean petassegang$storingLightForSection(long sectionNode);

    @Invoker("getStoredLevel")
    int petassegang$getStoredLevel(long blockNode);

    @Invoker("setStoredLevel")
    void petassegang$setStoredLevel(long blockNode, int level);
}
