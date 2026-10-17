package com.petassegang.addons.backrooms.portal.propagation;

import java.util.EnumSet;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;

/**
 * Objectif de deplacement d'un mob qui poursuit une cible a travers un portail (voir {@link PortalPursuit}) : il
 * regarde le portail, y marche et le traverse. Il ne s'active que sans cible dans son propre monde : une cible
 * proche passe avant un joueur entrevu par un cadre.
 */
public final class PortalPursuitGoal extends Goal {

    /** Intervalle entre deux recalculs du chemin, en ticks. */
    private static final int REPATH_INTERVAL_TICKS = 10;
    /** Vitesse de marche, en multiple de la vitesse du mob. */
    private static final double SPEED = 1.0;

    private final MobEntity mob;
    private int repathCooldown;
    /** Point vise derriere le cadre du portail poursuivi, calcule une fois par portail : un portail ne bouge jamais. */
    private @Nullable BackroomsPortalEntity throughPortal;
    private @Nullable Vec3d through;

    /** Objectif du mob qui poursuit : il prend la marche et le regard tant que la poursuite tient. */
    public PortalPursuitGoal(MobEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        return this.mob.getTarget() == null && PortalPursuit.active(this.mob) != null;
    }

    @Override
    public boolean shouldContinue() {
        return canStart();
    }

    @Override
    public void start() {
        this.repathCooldown = 0;
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
        PortalPursuit.end(this.mob);
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        PortalPursuit.Pursuit pursuit = PortalPursuit.active(this.mob);
        if (pursuit == null) {
            return;
        }
        BackroomsPortalEntity portal = pursuit.portal();
        this.mob.getLookControl().lookAt(portal.getX(), portal.getY(), portal.getZ());
        if (--this.repathCooldown <= 0) {
            this.repathCooldown = REPATH_INTERVAL_TICKS;
            if (this.throughPortal != portal) {
                this.throughPortal = portal;
                this.through = PortalPursuit.throughPoint(portal);
            }
            this.mob.getNavigation().startMovingTo(this.through.x, this.through.y, this.through.z, SPEED);
        }
    }
}
