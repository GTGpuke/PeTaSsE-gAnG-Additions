package com.petassegang.addons.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;

import com.petassegang.addons.backrooms.portal.propagation.PortalPursuer;
import com.petassegang.addons.backrooms.portal.propagation.PortalPursuit;
import com.petassegang.addons.backrooms.portal.propagation.PortalPursuitGoal;
import com.petassegang.addons.config.ModConfig;

/**
 * Poursuite a travers les portails : les mobs qui ciblent des joueurs ou se vengent recoivent l'objectif de
 * poursuite, et une cible dans un autre monde (frappe a travers un portail) devient une poursuite au lieu d'une cible.
 */
@Mixin(MobEntity.class)
public abstract class MixinMobEntity implements PortalPursuer {

    @Shadow
    @Final
    protected GoalSelector goalSelector;

    @Shadow
    @Final
    protected GoalSelector targetSelector;

    @Unique
    private PortalPursuit.@Nullable Pursuit backrooms$pursuit;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void backrooms$addPortalPursuit(EntityType<? extends MobEntity> type, World world, CallbackInfo ci) {
        if (ModConfig.ENABLE_PORTAL_FEATURE && world != null && !world.isClient && PortalPursuit.chasesPlayers(this.targetSelector)) {
            this.goalSelector.add(PortalPursuit.GOAL_PRIORITY, new PortalPursuitGoal((MobEntity) (Object) this));
        }
    }

    /** Une cible dans un autre monde n'est jamais gardee : le mob la poursuit par un portail s'il en a un a portee. */
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private @Nullable LivingEntity backrooms$targetThroughPortal(@Nullable LivingEntity target) {
        MobEntity self = (MobEntity) (Object) this;
        if (!ModConfig.ENABLE_PORTAL_FEATURE || target == null || target.getWorld() == self.getWorld() || self.getWorld().isClient) {
            return target;
        }
        PortalPursuit.redirect(self, target);
        return null;
    }

    @Override
    public PortalPursuit.@Nullable Pursuit backrooms$getPursuit() {
        return this.backrooms$pursuit;
    }

    @Override
    public void backrooms$setPursuit(PortalPursuit.@Nullable Pursuit pursuit) {
        this.backrooms$pursuit = pursuit;
    }
}
