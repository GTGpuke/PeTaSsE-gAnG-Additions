package com.petassegang.addons.backrooms.portal.propagation;

import java.util.EnumSet;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;

/**
 * Objectif de deplacement d'un mob qui poursuit une cible a travers un portail (voir {@link PortalPursuit}) : il prend
 * la posture d'attaque (bras leves, arc bande), fixe sa cible la ou il la voit par le cadre, marche jusqu'au portail et
 * le traverse ; il n'attaque qu'une fois de l'autre cote, ou elle devient sa vraie cible. Il ne s'active que sans cible
 * dans son propre monde : une cible proche passe avant un joueur entrevu par un cadre.
 */
public final class PortalPursuitGoal extends Goal {

    /** Intervalle entre deux recalculs du chemin, en ticks. */
    private static final int REPATH_INTERVAL_TICKS = 10;

    private final MobEntity mob;
    private int repathCooldown;
    /** Portail poursuivi dont les points vises sont en cache : un portail ne bouge jamais. */
    private @Nullable BackroomsPortalEntity throughPortal;
    /** Point d'approche, devant le cadre, et point vise derriere lui, une fois aligne. */
    private Vec3d approach = Vec3d.ZERO;
    private Vec3d through = Vec3d.ZERO;
    /** Aligne sur l'ouverture : il marche droit vers le point derriere le cadre, sans chemin. */
    private boolean linedUp;
    /** Ou la cible semble etre, vue par le cadre ; reprise tous les {@link #REPATH_INTERVAL_TICKS} ticks. */
    private Vec3d targetImage = Vec3d.ZERO;

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
        this.linedUp = false;
        // Comme les objectifs d'attaque vanilla : le mob a l'air de foncer sur sa cible, sans pouvoir la frapper d'ici.
        this.mob.setAttacking(true);
    }

    @Override
    public void stop() {
        this.mob.setAttacking(false);
        this.mob.getNavigation().stop();
        // Aligne, il marchait droit sans chemin : il s'arrete la ou il est.
        this.mob.getMoveControl().moveTo(this.mob.getX(), this.mob.getY(), this.mob.getZ(), 0.0);
        PortalPursuit.end(this.mob);
        // Les mobs qui reagissent a leur cible (enderman en colere et plus rapide) ont pris celle de l'autre monde pour une
        // vraie : sans cible de ce cote, ils la lachent comme vanilla.
        if (this.mob.getTarget() == null) {
            this.mob.setTarget(null);
        }
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
        if (this.throughPortal != portal) {
            // Un autre portail : le chemin vers l'approche de l'ancien est refait tout de suite.
            this.throughPortal = portal;
            this.approach = PortalPursuit.approachPoint(portal);
            this.through = PortalPursuit.throughPoint(portal);
            this.repathCooldown = 0;
        }
        // En face de l'ouverture, il marche droit a travers : un chemin de bloc en bloc pourrait couper le plan a cote du
        // cadre. Sinon il vient d'abord devant elle, par un chemin exact : a un bloc pres, il s'arreterait a cote.
        boolean aligned = PortalPursuit.linedUp(portal, this.mob.getX(), this.mob.getY(), this.mob.getZ());
        if (aligned != this.linedUp) {
            this.linedUp = aligned;
            this.repathCooldown = 0;
            this.mob.getNavigation().stop();
        }
        if (--this.repathCooldown <= 0) {
            this.repathCooldown = REPATH_INTERVAL_TICKS;
            this.targetImage = portal.fromDestination(pursuit.target().getEyePos());
            if (!aligned) {
                EntityNavigation navigation = this.mob.getNavigation();
                navigation.startMovingAlong(navigation.findPathTo(this.approach.x, this.approach.y, this.approach.z, 0), PortalPursuit.CHASE_SPEED);
            }
        }
        if (aligned) {
            this.mob.getMoveControl().moveTo(this.through.x, this.through.y, this.through.z, PortalPursuit.CHASE_SPEED);
        }
        this.mob.getLookControl().lookAt(this.targetImage.x, this.targetImage.y, this.targetImage.z);
    }
}
