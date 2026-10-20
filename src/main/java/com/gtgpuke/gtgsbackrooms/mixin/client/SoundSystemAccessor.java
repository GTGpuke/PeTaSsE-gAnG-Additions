package com.gtgpuke.gtgsbackrooms.mixin.client;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.sound.Channel;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;

/** Sons en cours de lecture et leur source OpenAL, et le gain final d'un volume dans une categorie : pour les replacer apres une traversee. */
@Mixin(SoundSystem.class)
public interface SoundSystemAccessor {

    @Accessor("sources")
    Map<SoundInstance, Channel.SourceManager> backrooms$getSources();

    @Invoker("getAdjustedVolume")
    float backrooms$adjustedVolume(float volume, SoundCategory category);
}
