package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;

/** Moteur de son du client, champ prive du gestionnaire de sons. */
@Mixin(SoundManager.class)
public interface SoundManagerAccessor {

    @Accessor("soundSystem")
    SoundSystem backrooms$getSoundSystem();
}
