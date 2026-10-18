package com.gtgpuke.gtgsbackrooms.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.entity.ai.goal.ActiveTargetGoal;

/**
 * Classe des cibles d'un objectif de ciblage : un mob qui cherche des joueurs peut les chercher a travers un portail.
 */
@Mixin(ActiveTargetGoal.class)
public interface ActiveTargetGoalAccessor {

    /** Classe des entities que l'objectif cherche a cibler. */
    @Accessor("targetClass")
    Class<?> backrooms$getTargetClass();
}
