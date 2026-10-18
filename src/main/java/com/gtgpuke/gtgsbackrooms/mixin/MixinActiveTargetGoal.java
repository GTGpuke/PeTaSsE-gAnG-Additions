package com.gtgpuke.gtgsbackrooms.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.MobEntity;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.propagation.PortalPursuit;
import com.gtgpuke.gtgsbackrooms.config.ModConfig;

/**
 * Un mob qui cherche un joueur et n'en trouve aucun dans son monde regarde aussi par les portails ouverts, avec ses
 * propres regles de ciblage ; distance et vue sont mesurees par le cadre (voir {@link PortalPursuit}).
 */
@Mixin(ActiveTargetGoal.class)
public abstract class MixinActiveTargetGoal extends TrackTargetGoal {

    @Shadow
    @Nullable
    protected LivingEntity targetEntity;

    @Shadow
    protected TargetPredicate targetPredicate;

    /** Regles de ciblage sans distance ni vue, copiees une fois : la distance et la vue se mesurent par le cadre. */
    @Unique
    private @Nullable TargetPredicate backrooms$throughPortal;

    protected MixinActiveTargetGoal(MobEntity mob, boolean checkVisibility) {
        super(mob, checkVisibility);
    }

    @Inject(method = "findClosestTarget", at = @At("TAIL"))
    private void backrooms$lookThroughPortals(CallbackInfo ci) {
        if (this.targetEntity != null || !ModConfig.ENABLE_PORTAL_FEATURE || !PortalPursuit.targetsPlayers((ActiveTargetGoalAccessor) this)) {
            return;
        }
        if (this.backrooms$throughPortal == null) {
            this.backrooms$throughPortal = this.targetPredicate.copy().setBaseMaxDistance(-1.0).ignoreVisibility();
        }
        PortalPursuit.perceive(this.mob, this.backrooms$throughPortal);
    }
}
