package com.petassegang.addons.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.mob.ZombieEntity;

import com.petassegang.addons.config.ModConfig;

/**
 * Un zombie qui a deja la posture d'attaque quand son attaque commence la garde : vanilla lui baisse les bras les cinq
 * premiers ticks d'une attaque. Celui qui poursuivait un joueur a travers un portail et le reprend de l'autre cote (une
 * copie neuve, dont l'attaque recommence) garderait sinon une posture qui saute.
 */
@Mixin(ZombieAttackGoal.class)
public abstract class MixinZombieAttackGoal {

    /** Ticks d'attaque apres lesquels vanilla leve les bras du zombie. */
    @Unique
    private static final int ARMS_RAISED_TICKS = 5;

    @Shadow
    @Final
    private ZombieEntity zombie;

    @Shadow
    private int ticks;

    /** Posture du zombie avant que l'attaque ne la pose. */
    @Unique
    private boolean backrooms$alreadyAttacking;

    @Inject(method = "start", at = @At("HEAD"))
    private void backrooms$notePosture(CallbackInfo ci) {
        this.backrooms$alreadyAttacking = this.zombie.isAttacking();
    }

    @Inject(method = "start", at = @At("TAIL"))
    private void backrooms$keepPosture(CallbackInfo ci) {
        if (ModConfig.ENABLE_PORTAL_FEATURE && this.backrooms$alreadyAttacking) {
            this.ticks = ARMS_RAISED_TICKS;
        }
    }
}
