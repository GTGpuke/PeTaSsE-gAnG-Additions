package com.gtgpuke.gtgsbackrooms.backrooms.portal.propagation;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntityRegistry;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.mixin.ActiveTargetGoalAccessor;

/**
 * Les mobs voient a travers les portails ouverts.
 *
 * <p>Un mob qui cherche un joueur a attaquer et n'en trouve aucun dans son monde regarde aussi par les portails
 * ouverts a sa portee : il applique a chaque joueur de l'autre cote ses propres regles de ciblage (difficulte, mode de
 * jeu, equipe, colere des mobs neutres), puis mesure la distance et la vue comme si les deux cotes etaient colles, par
 * le cadre. S'il voit un joueur, il marche jusqu'au portail et le traverse ; a l'arrivee, le joueur devient sa cible.
 * Un mob frappe a travers un portail fait de meme : une cible dans un autre monde n'est jamais gardee telle quelle,
 * les distances et la vue de vanilla n'y auraient aucun sens. Les mobs a cerveau (piglins, villageois) ne sont pas
 * concernes : ils n'ont pas d'objectifs de ciblage.
 */
public final class PortalPursuit {

    /** Duree d'une poursuite sans nouvelle vue de la cible, en ticks. */
    private static final int PURSUIT_TICKS = 100;
    /**
     * Point vise derriere le plan du portail, en blocs : une fois aligne, le mob marche droit vers lui et franchit le plan
     * au lieu de s'arreter devant ; a cette profondeur, la ligne droite depuis le point d'approche passe par l'ouverture.
     */
    static final double THROUGH_DEPTH = 2.0;
    /** Distance devant le plan du point d'approche, en face de l'ouverture : de la, le mob marche droit a travers. */
    static final double APPROACH_DEPTH = 1.0;
    /** Marge au-dela du point d'approche ou le mob est deja aligne : le chemin s'arrete a moins d'un bloc du point. */
    private static final double APPROACH_MARGIN = 1.0;
    /** Priorite de l'objectif de poursuite : celle des attaques au corps a corps, devant les promenades. */
    public static final int GOAL_PRIORITY = 2;
    /** Vitesse de la poursuite, en multiple de la vitesse du mob : celle des attaques au corps a corps vanilla. */
    public static final double CHASE_SPEED = 1.0;
    /** Distance minimale de ciblage de vanilla, quelle que soit la discretion de la cible. */
    private static final double MIN_TARGET_DISTANCE = 2.0;

    /**
     * Poursuite d'une cible a travers un portail.
     *
     * @param portal portail a traverser, dans le monde du mob
     * @param target cible, dans le monde d'arrivee
     * @param deadline tick du monde du mob au-dela duquel la poursuite cesse sans nouvelle vue
     */
    public record Pursuit(BackroomsPortalEntity portal, LivingEntity target, long deadline) {
    }

    private PortalPursuit() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** {@code true} si le mob cherche des joueurs ou se venge : il peut poursuivre a travers un portail. */
    public static boolean chasesPlayers(GoalSelector targetSelector) {
        for (PrioritizedGoal prioritized : targetSelector.getGoals()) {
            if (prioritized.getGoal() instanceof RevengeGoal) {
                return true;
            }
            if (prioritized.getGoal() instanceof ActiveTargetGoal<?> goal && targetsPlayers((ActiveTargetGoalAccessor) goal)) {
                return true;
            }
        }
        return false;
    }

    /** {@code true} si l'objectif de ciblage vise des joueurs. */
    public static boolean targetsPlayers(ActiveTargetGoalAccessor goal) {
        return PlayerEntity.class.isAssignableFrom(goal.backrooms$getTargetClass());
    }

    /**
     * Le mob n'a trouve aucun joueur dans son monde : il regarde par les portails ouverts a sa portee.
     *
     * @param mob mob qui cherche une cible, cote serveur (les objectifs ne tournent que la)
     * @param predicate regles de ciblage du mob, sans distance ni vue (mesurees ici, par le cadre)
     */
    public static void perceive(MobEntity mob, TargetPredicate predicate) {
        ServerWorld world = (ServerWorld) mob.getWorld();
        double range = mob.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE);
        double eyeX = mob.getX();
        double eyeY = mob.getEyeY();
        double eyeZ = mob.getZ();
        BackroomsPortalEntity bestPortal = null;
        LivingEntity bestTarget = null;
        double bestDistance = Double.MAX_VALUE;
        List<BackroomsPortalEntity> portals = BackroomsPortalEntityRegistry.all();
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (!opensTowardsAnotherWorld(portal, world) || portal.signedDistance(eyeX, eyeY, eyeZ) <= 0.0
                    || portal.squaredDistanceTo(eyeX, eyeY, eyeZ) > range * range) {
                continue;
            }
            ServerWorld destination = world.getServer().getWorld(portal.destinationDim());
            if (destination == null) {
                continue;
            }
            // Un portail a portee, devant le mob : l'oeil prend un vecteur pour les mesures par le cadre, pas avant.
            Vec3d eye = mob.getEyePos();
            List<ServerPlayerEntity> players = destination.getPlayers();
            for (int j = 0; j < players.size(); j++) {
                ServerPlayerEntity player = players.get(j);
                if (!predicate.test(mob, player)) {
                    continue;
                }
                Vec3d image = portal.fromDestination(player.getEyePos());
                double distance = eye.distanceTo(image);
                if (distance >= bestDistance || distance > Math.max(range * player.getAttackDistanceScalingFactor(mob), MIN_TARGET_DISTANCE)) {
                    continue;
                }
                Vec3d crossing = frameCrossing(portal, eye, image);
                if (crossing == null || !clearSight(mob, portal, eye, crossing, destination, player)) {
                    continue;
                }
                bestPortal = portal;
                bestTarget = player;
                bestDistance = distance;
            }
        }
        if (bestPortal != null) {
            begin(mob, bestPortal, bestTarget);
        }
    }

    /**
     * Le mob recoit une cible dans un autre monde (frappe a travers un portail) : il la poursuit par le portail ouvert le
     * plus proche qui y mene, s'il y en a un a sa portee. La cible n'est jamais gardee telle quelle.
     */
    public static void redirect(MobEntity mob, LivingEntity target) {
        World world = mob.getWorld();
        double range = mob.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE);
        Vec3d eye = mob.getEyePos();
        BackroomsPortalEntity nearest = null;
        double nearestDistance = range * range;
        List<BackroomsPortalEntity> portals = BackroomsPortalEntityRegistry.all();
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (!opensTowardsAnotherWorld(portal, world) || !portal.destinationDim().equals(target.getWorld().getRegistryKey())
                    || portal.signedDistance(eye) <= 0.0) {
                continue;
            }
            double distance = portal.squaredDistanceTo(eye);
            if (distance <= nearestDistance) {
                nearest = portal;
                nearestDistance = distance;
            }
        }
        if (nearest != null) {
            begin(mob, nearest, target);
        }
    }

    /** Poursuite en cours et toujours valable du mob, ou {@code null} ; une poursuite perimee est oubliee. */
    public static @Nullable Pursuit active(MobEntity mob) {
        PortalPursuer pursuer = (PortalPursuer) mob;
        Pursuit pursuit = pursuer.backrooms$getPursuit();
        if (pursuit == null) {
            return null;
        }
        BackroomsPortalEntity portal = pursuit.portal();
        LivingEntity target = pursuit.target();
        boolean valid = mob.getWorld().getTime() < pursuit.deadline()
                && !portal.isRemoved() && portal.getWorld() == mob.getWorld() && portal.teleportable()
                && target.isAlive()
                && target.getWorld().getRegistryKey().equals(portal.destinationDim());
        if (!valid) {
            pursuer.backrooms$setPursuit(null);
            return null;
        }
        return pursuit;
    }

    /**
     * Un mob qui poursuivait vient de traverser : sa proie, de ce cote, devient sa cible. Il garde la posture d'attaque et
     * repart aussitot vers elle, sans attendre que ses objectifs se relancent sur la copie neuve que le changement de
     * monde a creee ; la posture n'est gardee que si un chemin y mene, l'attaque vanilla qui le suivra la rendra.
     *
     * @param mob mob arrive de ce cote
     * @param target sa proie, dans ce monde
     */
    public static void resumeAfterCrossing(MobEntity mob, LivingEntity target) {
        mob.setTarget(target);
        if (mob.getNavigation().startMovingTo(target, CHASE_SPEED)) {
            mob.setAttacking(true);
        }
    }

    /** Arrete la poursuite du mob. */
    public static void end(MobEntity mob) {
        ((PortalPursuer) mob).backrooms$setPursuit(null);
    }

    /**
     * Cible a reprendre de l'autre cote quand le mob traverse ce portail.
     *
     * @return cible poursuivie dans le monde d'arrivee, ou {@code null}
     */
    public static @Nullable LivingEntity targetAfterCrossing(Entity entity, BackroomsPortalEntity portal) {
        if (!(entity instanceof MobEntity mob)) {
            return null;
        }
        Pursuit pursuit = active(mob);
        return pursuit == null || !pursuit.target().getWorld().getRegistryKey().equals(portal.destinationDim()) ? null : pursuit.target();
    }

    /** Point ou le mob vient d'abord : devant le plan, en face du milieu de l'ouverture, au pied du cadre. */
    static Vec3d approachPoint(BackroomsPortalEntity portal) {
        PortalFacing facing = portal.facingSource();
        return portal.getPos()
                .add(facing.normal.multiply(APPROACH_DEPTH))
                .subtract(facing.axisH.multiply(portal.height() / 2.0));
    }

    /**
     * {@code true} si un mob a cette position est aligne sur l'ouverture : juste devant le plan, et la ligne droite vers le
     * point vise derriere le cadre ({@link #throughPoint}) passe par l'ouverture. De plus loin ou de biais, un chemin
     * couperait le plan a cote du cadre, et le mob longerait le portail sans le traverser.
     */
    static boolean linedUp(BackroomsPortalEntity portal, double x, double y, double z) {
        double ahead = portal.signedDistance(x, y, z);
        if (ahead <= 0.0 || ahead > APPROACH_DEPTH + APPROACH_MARGIN) {
            return false;
        }
        PortalFacing facing = portal.facingSource();
        double lateral = (x - portal.getX()) * facing.axisW.x + (y - portal.getY()) * facing.axisW.y + (z - portal.getZ()) * facing.axisW.z;
        // La ligne droite vers le point vise, dans l'axe du cadre, coupe le plan a cet ecart du milieu.
        return Math.abs(lateral * THROUGH_DEPTH / (ahead + THROUGH_DEPTH)) <= portal.width() / 2.0;
    }

    /** Point vise par le mob une fois aligne : derriere le plan, au pied du cadre, pour que son chemin le traverse. */
    static Vec3d throughPoint(BackroomsPortalEntity portal) {
        PortalFacing facing = portal.facingSource();
        return portal.getPos()
                .subtract(facing.normal.multiply(THROUGH_DEPTH))
                .subtract(facing.axisH.multiply(portal.height() / 2.0));
    }

    /**
     * Point ou le regard de l'oeil vers l'image d'une cible passe le plan du portail, depuis le cote d'entree. L'image est
     * ou la cible semble etre, vue a travers le cadre ({@link BackroomsPortalEntity#fromDestination(Vec3d)}) : le
     * passage etant une isometrie, la distance a cette image est la longueur du trajet par le cadre.
     *
     * @return le point de passage s'il tombe dans le cadre, sinon {@code null}
     */
    static @Nullable Vec3d frameCrossing(BackroomsPortalEntity portal, Vec3d eye, Vec3d image) {
        double from = portal.signedDistance(eye);
        double to = portal.signedDistance(image);
        if (from <= 0.0 || to >= 0.0) {
            return null;
        }
        double t = from / (from - to);
        Vec3d crossing = eye.add(image.subtract(eye).multiply(t));
        return portal.frameContains(crossing.x, crossing.y, crossing.z) ? crossing : null;
    }

    /** Vue degagee de chaque cote : de l'oeil au cadre dans le monde du mob, du cadre d'arrivee a la cible dans l'autre. */
    private static boolean clearSight(MobEntity mob, BackroomsPortalEntity portal, Vec3d eye, Vec3d crossing, World destination, LivingEntity target) {
        if (mob.getWorld().raycast(new RaycastContext(eye, crossing, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mob))
                .getType() != HitResult.Type.MISS) {
            return false;
        }
        Vec3d arrival = portal.toDestination(crossing);
        return destination.raycast(new RaycastContext(arrival, target.getEyePos(), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mob))
                .getType() == HitResult.Type.MISS;
    }

    /** Portail ouvert, visible, qui mene hors du monde donne. */
    private static boolean opensTowardsAnotherWorld(BackroomsPortalEntity portal, World world) {
        return portal.getWorld() == world && portal.teleportable() && portal.visible() && !portal.destinationDim().equals(world.getRegistryKey());
    }

    private static void begin(MobEntity mob, BackroomsPortalEntity portal, LivingEntity target) {
        ((PortalPursuer) mob).backrooms$setPursuit(new Pursuit(portal, target, mob.getWorld().getTime() + PURSUIT_TICKS));
    }
}
