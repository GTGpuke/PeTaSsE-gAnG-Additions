package com.gtgpuke.gtgsbackrooms.mixin.client;

import java.util.Map;
import java.util.Queue;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.particle.EmitterParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleGroup;

/**
 * Contenu du gestionnaire de particules (monde et files), echange avec celui d'un monde distant le temps d'y ajouter,
 * d'y faire vivre ou d'y dessiner ses particules.
 */
@Mixin(ParticleManager.class)
public interface ParticleManagerAccessor {

    @Accessor("world")
    ClientWorld backrooms$getWorld();

    @Accessor("world")
    void backrooms$setWorld(ClientWorld world);

    @Accessor("particles")
    Map<ParticleTextureSheet, Queue<Particle>> backrooms$getParticles();

    @Mutable
    @Accessor("particles")
    void backrooms$setParticles(Map<ParticleTextureSheet, Queue<Particle>> particles);

    @Accessor("newEmitterParticles")
    Queue<EmitterParticle> backrooms$getNewEmitterParticles();

    @Mutable
    @Accessor("newEmitterParticles")
    void backrooms$setNewEmitterParticles(Queue<EmitterParticle> emitters);

    @Accessor("newParticles")
    Queue<Particle> backrooms$getNewParticles();

    @Mutable
    @Accessor("newParticles")
    void backrooms$setNewParticles(Queue<Particle> particles);

    @Accessor("groupCounts")
    Object2IntOpenHashMap<ParticleGroup> backrooms$getGroupCounts();

    @Mutable
    @Accessor("groupCounts")
    void backrooms$setGroupCounts(Object2IntOpenHashMap<ParticleGroup> counts);
}
