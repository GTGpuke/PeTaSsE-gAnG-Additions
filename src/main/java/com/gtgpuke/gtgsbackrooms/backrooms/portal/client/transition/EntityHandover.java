package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.transition;

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
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.ClientPortalIndex;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.RemoteWorldRegistry;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.mixin.client.LimbAnimatorAccessor;

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
    /** Marge autour du cadre d'arrivee ou le serveur pose une entity qui vient de traverser. */
    private static final double ARRIVAL_MARGIN = 2.0;
    /** Pas d'interpolation vers la position du serveur, ceux des deplacements vanilla. */
    private static final int INTERPOLATION_STEPS = 3;
    /** Duree maximale d'une arrivee : une entity lente sort du cadre d'arrivee en quelques secondes. */
    private static final int MAX_ARRIVAL_TICKS = 100;

    /** Copie retiree, son monde, le portail qu'elle franchissait s'il y en a un, et le tick ou elle n'attend plus de suite. */
    private record Departure(Entity entity, ClientWorld world, @Nullable BackroomsPortalEntity portal, long expiresAt) {
    }

    /** Copie a retirer a l'echeance, si elle n'a pas ete remplacee entre-temps. */
    private record PendingRemoval(ClientWorld world, Entity entity, long dueAt) {
    }

    /** Copie arrivee par un portail, pas encore sortie de son cadre d'arrivee. */
    private record Arrival(Entity entity, BackroomsPortalEntity portal, long expiresAt) {
    }

    private static final Map<UUID, Departure> DEPARTURES = new HashMap<>();
    private static final List<PendingRemoval> PENDING_REMOVALS = new ArrayList<>();
    private static final List<Arrival> ARRIVALS = new ArrayList<>();
    private static long ticks;

    private EntityHandover() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Branche les evenements de chargement d'entities client (point d'entree client). */
    public static void init() {
        ClientEntityEvents.ENTITY_UNLOAD.register(EntityHandover::onUnload);
        ClientEntityEvents.ENTITY_LOAD.register(EntityHandover::onLoad);
    }

    /**
     * Tick client : les copies retirees sans suite sont oubliees, les retraits differes arrives a echeance s'appliquent. En
     * pause, les mondes ne tiquent pas, et les echeances attendent aussi.
     */
    public static void onClientTick() {
        if (MinecraftClient.getInstance().isPaused()) {
            return;
        }
        ticks++;
        if (!DEPARTURES.isEmpty()) {
            DEPARTURES.values().removeIf(departure -> departure.expiresAt() < ticks);
        }
        if (!PENDING_REMOVALS.isEmpty()) {
            PENDING_REMOVALS.removeIf(EntityHandover::applyIfDue);
        }
        if (!ARRIVALS.isEmpty()) {
            ARRIVALS.removeIf(EntityHandover::arrivalOver);
        }
    }

    /** Deconnexion : plus rien a reprendre ni a retirer. */
    public static void clear() {
        DEPARTURES.clear();
        PENDING_REMOVALS.clear();
        ARRIVALS.clear();
    }

    /**
     * Portail par lequel l'entity vient d'arriver, tant qu'elle n'est pas sortie de son cadre d'arrivee, ou {@code null}.
     * Le client la montre avec un peu de retard : a son arrivee, elle est souvent encore derriere le plan d'arrivee, et
     * chacune de ses parties se dessine de son cote du portail.
     */
    public static @Nullable BackroomsPortalEntity arrivalPortal(Entity entity) {
        for (int i = 0; i < ARRIVALS.size(); i++) {
            Arrival arrival = ARRIVALS.get(i);
            if (arrival.entity() == entity) {
                return arrival.portal();
            }
        }
        return null;
    }

    /** Nombre d'entities arrivees par un portail et pas encore sorties de son cadre. */
    public static int arrivalCount() {
        return ARRIVALS.size();
    }

    /** Entity arrivee d'indice donne, dans [0, {@link #arrivalCount()}[ : un parcours par index, sans allocation. */
    public static Entity arrivedEntity(int index) {
        return ARRIVALS.get(index).entity();
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

    /**
     * Le serveur renvoie une entity sous un autre identifiant alors que l'ancienne copie est encore la (retrait differe en
     * attente) : l'index des identifiants uniques refuserait la nouvelle, qui resterait affichee sans jamais etre mise a
     * jour ni retiree. L'ancienne part d'abord, et la nouvelle reprend son etat visible.
     *
     * @param world monde client ou la copie arrive
     * @param stale copie deja presente sous le meme identifiant unique
     */
    public static void replaceStaleCopy(ClientWorld world, Entity stale) {
        if (stale == MinecraftClient.getInstance().player) {
            return;
        }
        if (!(stale instanceof BackroomsPortalEntity)) {
            DEPARTURES.put(stale.getUuid(), new Departure(stale, world, null, ticks + MAX_DELAY_TICKS));
        }
        world.removeEntity(stale.getId(), Entity.RemovalReason.DISCARDED);
    }

    /**
     * Destructions recues pour un monde client. Une entity vivante detruite en franchissant un portail ouvert vers une
     * dimension que le client tient reste affichee jusqu'a l'arrivee de sa copie de l'autre cote, au plus quelques ticks :
     * la copie arrive par une autre voie, souvent un tick plus tard, et l'entity disparaitrait entre-temps. Toute autre
     * part aussitot, comme chez vanilla (un mob mort, un objet ramasse pres d'un portail).
     *
     * @param world monde client auquel s'adressent les destructions
     * @param entityIds identifiants detruits
     */
    public static void applyDestroys(ClientWorld world, IntList entityIds) {
        MinecraftClient client = MinecraftClient.getInstance();
        for (int i = 0; i < entityIds.size(); i++) {
            int id = entityIds.getInt(i);
            Entity entity = world.getEntityById(id);
            BackroomsPortalEntity portal = entity == null || entity == client.player || entity instanceof BackroomsPortalEntity
                    || !entity.isAlive() ? null : crossedPortal(entity, world);
            if (portal != null && (RemoteWorldRegistry.get(portal.destinationDim()) != null
                    || client.world != null && portal.destinationDim().equals(client.world.getRegistryKey()))) {
                DEPARTURES.put(entity.getUuid(), new Departure(entity, world, portal, ticks + MAX_DELAY_TICKS));
                PENDING_REMOVALS.add(new PendingRemoval(world, entity, ticks + MAX_DELAY_TICKS));
            } else {
                world.removeEntity(id, Entity.RemovalReason.DISCARDED);
            }
        }
    }

    /**
     * Position recue pour une entity : un portail du meme monde vient de la teleporter (elle etait dans son cadre, le
     * serveur la met a son arrivee). Vanilla la ferait glisser d'un bout a l'autre en trois ticks, a travers tout ce qui
     * les separe : elle passe d'abord le portail telle qu'elle est affichee, puis rejoint la position du serveur.
     *
     * @param world monde client de l'entity
     * @param entityId identifiant de l'entity
     * @param x position donnee par le serveur
     * @param y position donnee par le serveur
     * @param z position donnee par le serveur
     */
    public static void onPositionPacket(@Nullable ClientWorld world, int entityId, double x, double y, double z) {
        Entity entity = world == null ? null : world.getEntityById(entityId);
        if (entity == null || entity == MinecraftClient.getInstance().player) {
            return;
        }
        List<BackroomsPortalEntity> portals = ClientPortalIndex.portals(world);
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (!portal.teleportable() || !portal.destinationDim().equals(world.getRegistryKey())
                    || !portal.crossingBox().contains(entity.getPos()) || !portal.arrivalBox().expand(ARRIVAL_MARGIN).contains(x, y, z)) {
                continue;
            }
            // Plus pres de son image de l'autre cote que d'ou elle est : c'est le portail qui l'a deplacee.
            Vec3d image = portal.toDestination(entity.getPos());
            if (image.squaredDistanceTo(x, y, z) < entity.getPos().squaredDistanceTo(x, y, z)) {
                transpose(entity, portal);
                // Telle qu'elle est affichee, elle est encore derriere le plan d'arrivee : elle en sort comme une entity
                // venue d'une autre dimension, chaque partie de son cote du portail.
                arrive(entity, portal);
                return;
            }
        }
    }

    /** Passe une entity par un portail telle qu'elle est affichee : positions, regard, tete et corps. */
    private static void transpose(Entity entity, BackroomsPortalEntity portal) {
        Vec2f look = PortalFacing.transformLook(portal.facingSource(), portal.facingDestination(), entity.getYaw(), entity.getPitch());
        float yawShift = MathHelper.wrapDegrees(look.x - entity.getYaw());
        float pitchShift = look.y - entity.getPitch();
        Vec3d previous = portal.toDestination(new Vec3d(entity.prevX, entity.prevY, entity.prevZ));
        Vec3d rendered = portal.toDestination(new Vec3d(entity.lastRenderX, entity.lastRenderY, entity.lastRenderZ));
        entity.setPosition(portal.toDestination(entity.getPos()));
        entity.prevX = previous.x;
        entity.prevY = previous.y;
        entity.prevZ = previous.z;
        entity.lastRenderX = rendered.x;
        entity.lastRenderY = rendered.y;
        entity.lastRenderZ = rendered.z;
        entity.setYaw(entity.getYaw() + yawShift);
        entity.prevYaw += yawShift;
        entity.setPitch(entity.getPitch() + pitchShift);
        entity.prevPitch += pitchShift;
        if (entity instanceof LivingEntity living) {
            living.headYaw += yawShift;
            living.prevHeadYaw += yawShift;
            living.bodyYaw += yawShift;
            living.prevBodyYaw += yawShift;
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

    /**
     * Portail ouvert vers une autre dimension que la copie retiree franchissait : assez proche, et dont elle allait passer le
     * plan en face de l'ouverture, vers l'arriere, dans le delai d'une suite (voir {@link BackroomsPortalEntity#enteredBy}) ;
     * le plus proche s'il y en a plusieurs. Le client montre une entity avec un peu de retard sur le serveur : quand il la
     * detruit, sa copie n'a souvent pas encore atteint le plan. Les deux faces d'un bloc portail sont au meme endroit :
     * prendre la mauvaise transposerait la nouvelle copie en miroir, et chacune de ses moities se dessinerait du mauvais cote.
     */
    private static @Nullable BackroomsPortalEntity crossedPortal(Entity entity, ClientWorld world) {
        List<BackroomsPortalEntity> portals = ClientPortalIndex.portals(world);
        double moveX = entity.getX() - entity.prevX;
        double moveY = entity.getY() - entity.prevY;
        double moveZ = entity.getZ() - entity.prevZ;
        BackroomsPortalEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (!portal.teleportable() || portal.destinationDim().equals(world.getRegistryKey())
                    || !portal.crossingBox().contains(entity.getPos())
                    || !portal.enteredBy(entity.getX(), entity.getY(), entity.getZ(), moveX, moveY, moveZ, MAX_DELAY_TICKS)) {
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
            // L'ancienne copie, gardee le temps que la nouvelle arrive (voir applyDestroys), part : la nouvelle se dessine
            // des deux cotes du portail tant qu'elle n'est pas sortie de son cadre d'arrivee.
            if (departure.world().getEntityById(departure.entity().getId()) == departure.entity()) {
                retire(departure.world(), departure.entity());
            }
            arrive(entity, departure.portal());
        }
    }

    /** L'entity vient d'arriver par ce portail ; une arrivee precedente, pas encore finie, est remplacee. */
    private static void arrive(Entity entity, BackroomsPortalEntity portal) {
        Arrival arrival = new Arrival(entity, portal, ticks + MAX_ARRIVAL_TICKS);
        for (int i = 0; i < ARRIVALS.size(); i++) {
            if (ARRIVALS.get(i).entity() == entity) {
                ARRIVALS.set(i, arrival);
                return;
            }
        }
        ARRIVALS.add(arrival);
    }

    /**
     * Apres le tick : les images a venir montrent l'entity de sa position d'avant ce tick a la nouvelle. L'arrivee est
     * finie quand la premiere est sortie du cadre d'arrivee : la suite s'en eloigne.
     *
     * @return {@code true} si l'arrivee est finie : entity sortie du cadre d'arrivee, retiree, ou trop longtemps dedans
     */
    private static boolean arrivalOver(Arrival arrival) {
        Entity entity = arrival.entity();
        BackroomsPortalEntity portal = arrival.portal();
        if (entity.isRemoved() || arrival.expiresAt() < ticks) {
            return true;
        }
        Vec3d normal = portal.facingDestination().normal;
        Vec3d center = portal.destinationPos();
        double ahead = (entity.lastRenderX - center.x) * normal.x + (entity.lastRenderY - center.y) * normal.y
                + (entity.lastRenderZ - center.z) * normal.z;
        return ahead > 0.0 && !portal.arrivalFrameStraddles(entity.getBoundingBox(), entity.lastRenderX - entity.getX(),
                entity.lastRenderY - entity.getY(), entity.lastRenderZ - entity.getZ());
    }

    /** Retire une copie gardee ; son retrait la declarant de nouveau partante, sa suite deja la, on l'oublie aussitot. */
    private static void retire(ClientWorld world, Entity kept) {
        world.removeEntity(kept.getId(), Entity.RemovalReason.DISCARDED);
        Departure again = DEPARTURES.get(kept.getUuid());
        if (again != null && again.entity() == kept) {
            DEPARTURES.remove(kept.getUuid());
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
        if (from instanceof MobEntity before && to instanceof MobEntity after) {
            // La posture d'attaque suit sans attendre que le serveur la redonne a la copie neuve.
            after.setAttacking(before.isAttacking());
        }
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
        to.limbAnimator.setSpeed(from.limbAnimator.getSpeed());
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
