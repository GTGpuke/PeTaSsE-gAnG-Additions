package com.petassegang.addons.backrooms.portal.client.transition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;

import com.petassegang.addons.backrooms.portal.client.world.ClientPortalIndex;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.mixin.client.LimbAnimatorAccessor;

/**
 * Continuite des entities que le serveur recree : un joueur ou un mob qui traverse un portail, vu par un autre joueur,
 * et les entities qu'un joueur retrouve dans le monde qu'il vient de quitter ou de rejoindre.
 *
 * <p>Pour changer une entity de monde, le serveur la retire de l'un et la recree dans l'autre ; un joueur qui change de
 * monde recoit de meme une copie neuve de chaque entity qu'il continue de voir. Une copie neuve a les membres au repos,
 * le bras baisse, l'age a zero (les animations d'attente repartent) et saute d'un coup a la position du serveur. Comme
 * l'entity garde son identifiant unique, la nouvelle copie reprend ici l'etat visible de l'ancienne, transpose par le
 * portail traverse s'il y en a un : age, membres, bras, tete et corps tournes avec le portail, cape d'un joueur, et
 * position affichee, d'ou elle rejoint celle du serveur en trois ticks, comme tout deplacement recu.
 *
 * <p>Quand le joueur change de monde, le serveur efface aussi chez lui les copies qu'il ne suit plus de la meme facon
 * (celles du monde quitte, celles du monde rejoint suivies jusque-la a travers le portail) et les renvoie aussitot par
 * l'autre voie. Retirees tout de suite, elles manqueraient quelques ticks : le portail de retour, juste derriere le
 * joueur, disparaitrait. Leur retrait est donc differe ({@link #deferRemoval}) : la copie renvoyee remplace l'ancienne
 * au meme identifiant, sans trou ; une copie que le serveur ne renvoie pas est retiree a l'echeance.
 */
public final class EntityHandover {

    /** Delai maximal entre le retrait d'une copie et l'arrivee de la suivante : au-dela, ce n'est pas la meme traversee. */
    private static final int MAX_DELAY_TICKS = 10;
    /** Pas d'interpolation vers la position du serveur, ceux des deplacements vanilla. */
    private static final int INTERPOLATION_STEPS = 3;

    /** Copie retiree, son monde, le portail qu'elle franchissait s'il y en a un, et le tick ou elle n'attend plus de suite. */
    private record Departure(Entity entity, ClientWorld world, @Nullable BackroomsPortalEntity portal, long expiresAt) {
    }

    /** Copie a retirer a l'echeance, si elle n'a pas ete remplacee entre-temps. */
    private record PendingRemoval(ClientWorld world, Entity entity, long dueAt) {
    }

    private static final Map<UUID, Departure> DEPARTURES = new HashMap<>();
    private static final List<PendingRemoval> PENDING_REMOVALS = new ArrayList<>();
    private static long ticks;

    private EntityHandover() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Branche les evenements de chargement d'entities client (point d'entree client). */
    public static void init() {
        ClientEntityEvents.ENTITY_UNLOAD.register(EntityHandover::onUnload);
        ClientEntityEvents.ENTITY_LOAD.register(EntityHandover::onLoad);
    }

    /** Tick client : les copies retirees sans suite sont oubliees, les retraits differes arrives a echeance s'appliquent. */
    public static void onClientTick() {
        ticks++;
        if (!DEPARTURES.isEmpty()) {
            DEPARTURES.values().removeIf(departure -> departure.expiresAt() < ticks);
        }
        if (!PENDING_REMOVALS.isEmpty()) {
            PENDING_REMOVALS.removeIf(EntityHandover::applyIfDue);
        }
    }

    /** Deconnexion : plus rien a reprendre ni a retirer. */
    public static void clear() {
        DEPARTURES.clear();
        PENDING_REMOVALS.clear();
    }

    /**
     * Copies d'entities que le serveur efface pendant une bascule de monde : retirees dans quelques ticks seulement, sauf
     * si le serveur les renvoie d'ici la (vanilla remplace alors l'ancienne copie au meme identifiant).
     */
    public static void deferRemoval(ClientWorld world, IntList entityIds) {
        Entity player = MinecraftClient.getInstance().player;
        for (int i = 0; i < entityIds.size(); i++) {
            Entity entity = world.getEntityById(entityIds.getInt(i));
            if (entity != null && entity != player) {
                PENDING_REMOVALS.add(new PendingRemoval(world, entity, ticks + MAX_DELAY_TICKS));
            }
        }
    }

    /** @return {@code true} si le retrait est traite : applique a l'echeance, sauf si la copie a ete remplacee */
    private static boolean applyIfDue(PendingRemoval removal) {
        if (removal.dueAt() > ticks) {
            return false;
        }
        Entity entity = removal.entity();
        if (removal.world().getEntityById(entity.getId()) == entity) {
            removal.world().removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
        }
        return true;
    }

    /**
     * Une copie quitte un monde client. On la garde si une suite est attendue : elle franchissait un portail vers une autre
     * dimension, ou le joueur change lui-meme de monde (les copies du monde quitte reviennent aussitot, vues par le portail
     * de retour, et celles du monde rejoint sont renvoyees par vanilla).
     */
    private static void onUnload(Entity entity, ClientWorld world) {
        if (entity instanceof BackroomsPortalEntity || entity == MinecraftClient.getInstance().player) {
            return;
        }
        BackroomsPortalEntity portal = crossedPortal(entity, world);
        if (portal != null || ClientPortalCrossing.swallowsVanillaUnloads()) {
            DEPARTURES.put(entity.getUuid(), new Departure(entity, world, portal, ticks + MAX_DELAY_TICKS));
        }
    }

    /** Portail ouvert vers une autre dimension dont la copie retiree etait assez proche pour l'avoir franchi, le plus proche. */
    private static @Nullable BackroomsPortalEntity crossedPortal(Entity entity, ClientWorld world) {
        List<BackroomsPortalEntity> portals = ClientPortalIndex.portals(world);
        BackroomsPortalEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (!portal.teleportable() || portal.destinationDim().equals(world.getRegistryKey())
                    || !portal.crossingBox().contains(entity.getPos())) {
                continue;
            }
            double distance = portal.squaredDistanceTo(entity);
            if (distance < nearestDistance) {
                nearest = portal;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    /** Une copie arrive : si elle succede a une copie retiree de la meme entity, elle en reprend l'etat visible. */
    private static void onLoad(Entity entity, ClientWorld world) {
        Departure departure = DEPARTURES.get(entity.getUuid());
        if (departure == null || departure.entity() == entity) {
            return;
        }
        if (departure.world() == world) {
            DEPARTURES.remove(entity.getUuid());
            handOver(departure.entity(), entity, null);
        } else if (departure.portal() != null && departure.portal().destinationDim().equals(world.getRegistryKey())) {
            DEPARTURES.remove(entity.getUuid());
            handOver(departure.entity(), entity, departure.portal());
        }
    }

    /**
     * La nouvelle copie reprend l'etat visible de l'ancienne, transpose par le portail s'il y en a un, puis rejoint la
     * position, le regard et la tete que le serveur lui a donnes.
     */
    private static void handOver(Entity from, Entity to, @Nullable BackroomsPortalEntity portal) {
        Vec3d serverPos = to.getPos();
        float serverYaw = to.getYaw();
        float serverPitch = to.getPitch();
        float yawShift = 0.0f;
        float pitchShift = 0.0f;
        Vec3d position = from.getPos();
        Vec3d previous = new Vec3d(from.prevX, from.prevY, from.prevZ);
        Vec3d rendered = new Vec3d(from.lastRenderX, from.lastRenderY, from.lastRenderZ);
        if (portal != null) {
            Vec2f look = PortalFacing.transformLook(portal.facingSource(), portal.facingDestination(), from.getYaw(), from.getPitch());
            yawShift = MathHelper.wrapDegrees(look.x - from.getYaw());
            pitchShift = look.y - from.getPitch();
            position = portal.toDestination(position);
            previous = portal.toDestination(previous);
            rendered = portal.toDestination(rendered);
        }
        to.age = from.age;
        to.setPosition(position);
        to.prevX = previous.x;
        to.prevY = previous.y;
        to.prevZ = previous.z;
        to.lastRenderX = rendered.x;
        to.lastRenderY = rendered.y;
        to.lastRenderZ = rendered.z;
        to.setYaw(from.getYaw() + yawShift);
        to.prevYaw = from.prevYaw + yawShift;
        to.setPitch(from.getPitch() + pitchShift);
        to.prevPitch = from.prevPitch + pitchShift;
        to.horizontalSpeed = from.horizontalSpeed;
        to.prevHorizontalSpeed = from.prevHorizontalSpeed;
        to.distanceTraveled = from.distanceTraveled;
        if (from instanceof LivingEntity before && to instanceof LivingEntity after) {
            handOverLiving(before, after, yawShift, serverPos, serverYaw, serverPitch);
        } else {
            // Objets et projectiles n'interpolent pas : l'image en cours part de l'ancienne position, le tick suivant les y met.
            to.setPosition(serverPos);
        }
        if (from instanceof PlayerEntity before && to instanceof PlayerEntity after) {
            handOverCape(before, after, portal);
        }
    }

    /** Membres, bras, blessure, tete et corps ; puis interpolation vers ce que le serveur a donne. */
    private static void handOverLiving(LivingEntity from, LivingEntity to, float yawShift, Vec3d serverPos, float serverYaw, float serverPitch) {
        float serverHeadYaw = to.getHeadYaw();
        LimbAnimatorAccessor limbs = (LimbAnimatorAccessor) to.limbAnimator;
        limbs.backrooms$setPrevSpeed(((LimbAnimatorAccessor) from.limbAnimator).backrooms$getPrevSpeed());
        limbs.backrooms$setSpeed(from.limbAnimator.getSpeed());
        limbs.backrooms$setPos(from.limbAnimator.getPos());
        to.handSwinging = from.handSwinging;
        to.handSwingTicks = from.handSwingTicks;
        to.handSwingProgress = from.handSwingProgress;
        to.lastHandSwingProgress = from.lastHandSwingProgress;
        to.preferredHand = from.preferredHand;
        to.hurtTime = from.hurtTime;
        to.maxHurtTime = from.maxHurtTime;
        to.headYaw = from.headYaw + yawShift;
        to.prevHeadYaw = from.prevHeadYaw + yawShift;
        to.bodyYaw = from.bodyYaw + yawShift;
        to.prevBodyYaw = from.prevBodyYaw + yawShift;
        // Vanilla interpole les angles par le plus court chemin : le regard repris rejoint celui du serveur sans tour complet.
        to.updateTrackedPositionAndAngles(serverPos.x, serverPos.y, serverPos.z, serverYaw, serverPitch, INTERPOLATION_STEPS);
        to.updateTrackedHeadRotation(serverHeadYaw, INTERPOLATION_STEPS);
    }

    /** Cape d'un joueur : un point du monde qui suit le joueur avec retard, transpose comme lui. */
    private static void handOverCape(PlayerEntity from, PlayerEntity to, @Nullable BackroomsPortalEntity portal) {
        Vec3d cape = new Vec3d(from.capeX, from.capeY, from.capeZ);
        Vec3d previousCape = new Vec3d(from.prevCapeX, from.prevCapeY, from.prevCapeZ);
        if (portal != null) {
            cape = portal.toDestination(cape);
            previousCape = portal.toDestination(previousCape);
        }
        to.capeX = cape.x;
        to.capeY = cape.y;
        to.capeZ = cape.z;
        to.prevCapeX = previousCape.x;
        to.prevCapeY = previousCape.y;
        to.prevCapeZ = previousCape.z;
        to.strideDistance = from.strideDistance;
        to.prevStrideDistance = from.prevStrideDistance;
    }
}
