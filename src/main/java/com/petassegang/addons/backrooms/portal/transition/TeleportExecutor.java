package com.petassegang.addons.backrooms.portal.transition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.DifficultyS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerAbilitiesS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.WorldProperties;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntityRegistry;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.backrooms.portal.sync.PortalCrossPayload;
import com.petassegang.addons.backrooms.portal.sync.PortalTeleportPayload;
import com.petassegang.addons.backrooms.portal.sync.PortalViewTracker;
import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.mixin.EntityAccessor;

/**
 * Fait passer joueurs, objets, mobs et projectiles d'un cote a l'autre des portails, cote serveur.
 *
 * <p>Pour les joueurs, deux declencheurs : le client annonce que sa camera a franchi le plan ({@link PortalCrossPayload},
 * chemin normal, verifie puis applique avec l'etat exact du client pour que sa prediction tienne), et
 * en secours le serveur detecte lui-meme le passage des pieds entre deux ticks. Un changement de
 * dimension suit la sequence vanilla sans le paquet de respawn : le client bascule sur le monde
* distant qu'il a deja charge, sans ecran de chargement. Les autres entities sont teleportees par le
 * serveur seul, d'apres leur deplacement du tick, avec le cooldown de portail vanilla.
 */
public final class TeleportExecutor {

    /** Jeu minuscule devant le portail d'arrivee : le joueur ressort exactement la ou il est entre, sans rester dans le plan. */
    public static final double EXIT_OFFSET = 0.02;
    /** Ticks laisses au client pour annoncer lui-meme un franchissement que le serveur a detecte : sa prediction doit gagner. */
    static final int CLIENT_PREDICTION_GRACE_TICKS = 5;
    /** Marge autour du cadre dans laquelle une traversee compte (demi-largeur du joueur et un peu de jeu). */
    static final double CROSSING_MARGIN = 0.5;
    /** Ecart tolere entre la position annoncee par le client et celle connue du serveur (un tick d'avance). */
    static final double MAX_PREDICTION_DRIFT = 3.0;
    /** Au-dela de ce deplacement par tick (ou par image, cote client), ce n'est pas une marche mais un saut de position (respawn, commande). */
    public static final double MAX_STEP_SQUARED = 8.0 * 8.0;
    /** Cooldown de portail vanilla pose sur un objet, un mob ou un projectile qui vient de passer : evite l'aller-retour immediat entre deux cadres proches. */
    static final int ENTITY_PORTAL_COOLDOWN_TICKS = 10;
    /** Hauteur maximale de remontee pour sortir le joueur d'un bloc a l'arrivee. */
    private static final double MAX_LIFT = 0.30;
    private static final double LIFT_STEP = 0.02;

    private static final Map<UUID, Vec3d> LAST_PLAYER_POS = new HashMap<>();
    /** Franchissements detectes par le serveur, en attente de l'annonce du client. */
    private static final Map<UUID, PendingCrossing> PENDING_CROSSINGS = new HashMap<>();
    private static final TypeFilter<Entity, Entity> ENTITY_FILTER = TypeFilter.instanceOf(Entity.class);
    private static final List<Entity> ENTITY_SCRATCH = new ArrayList<>();
    /** Objets, mobs et projectiles libres : les joueurs ont leur propre chemin, et un portail ne traverse rien. */
    private static final Predicate<Entity> CROSSING_CANDIDATE = entity -> !(entity instanceof PlayerEntity)
            && !(entity instanceof BackroomsPortalEntity)
            && entity.isAlive()
            && !entity.hasPortalCooldown()
            && !entity.hasVehicle()
            && !entity.hasPassengers();

    /** Portail franchi selon le serveur et tick a partir duquel il teleporte lui-meme. */
    private record PendingCrossing(UUID portal, long deadline) {
    }
    private static boolean initialized;

    private TeleportExecutor() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Branche le tick serveur, le recepteur client et le nettoyage ; idempotent. */
    public static void init() {
        if (initialized) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(TeleportExecutor::tick);
        ServerPlayNetworking.registerGlobalReceiver(PortalCrossPayload.ID, (payload, context) -> onClientCrossing(context.player(), payload));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            LAST_PLAYER_POS.remove(handler.player.getUuid());
            PENDING_CROSSINGS.remove(handler.player.getUuid());
            TpCooldownTracker.clear(handler.player.getUuid());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            LAST_PLAYER_POS.clear();
            PENDING_CROSSINGS.clear();
            TpCooldownTracker.clearAll();
        });
        initialized = true;
    }

    private static void tick(MinecraftServer server) {
        processEntities();
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        for (int i = 0; i < players.size(); i++) {
            if (processPlayer(players.get(i))) {
                // Une teleportation peut reorganiser les listes de joueurs : on reprend au tick suivant.
                return;
            }
        }
    }

    /** @return {@code true} si le joueur vient d'etre teleporte */
    private static boolean processPlayer(ServerPlayerEntity player) {
        // Un joueur retire n'est plus vivant non plus.
        if (!player.isAlive()) {
            LAST_PLAYER_POS.remove(player.getUuid());
            return false;
        }
        Vec3d currentPos = player.getPos();
        Vec3d lastPos = LAST_PLAYER_POS.put(player.getUuid(), currentPos);
        if (lastPos == null || lastPos == currentPos || player.hasVehicle()
                || lastPos.squaredDistanceTo(currentPos) > MAX_STEP_SQUARED) {
            return false;
        }
        ServerWorld world = player.getServerWorld();
        if (TpCooldownTracker.isOnCooldown(player.getUuid(), world.getTime(), ModConfig.PORTAL_TELEPORT_COOLDOWN_TICKS)) {
            return false;
        }
        PendingCrossing pending = PENDING_CROSSINGS.get(player.getUuid());
        if (pending != null) {
            // Le client n'a pas annonce le franchissement a temps : le serveur teleporte depuis son propre etat.
            if (world.getTime() < pending.deadline()) {
                return false;
            }
            PENDING_CROSSINGS.remove(player.getUuid());
            BackroomsPortalEntity portal = BackroomsPortalEntityRegistry.find(world, pending.portal());
            return portal != null && portal.teleportable() && execute(player, portal, currentPos, player.getVelocity(), player.getYaw(), player.getPitch(), false);
        }
        List<BackroomsPortalEntity> portals = BackroomsPortalEntityRegistry.all();
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (portal.getWorld() == world && portal.teleportable() && crossedPortalPlane(portal, lastPos, currentPos)) {
                PENDING_CROSSINGS.put(player.getUuid(), new PendingCrossing(portal.getUuid(), world.getTime() + CLIENT_PREDICTION_GRACE_TICKS));
                return false;
            }
        }
        return false;
    }

    /** Objets, mobs et projectiles : le serveur decide seul, d'apres le deplacement du tick. */
    private static void processEntities() {
        List<BackroomsPortalEntity> portals = BackroomsPortalEntityRegistry.all();
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (!portal.teleportable()) {
                continue;
            }
            // Le registre ne tient que des portails serveur.
            ServerWorld world = (ServerWorld) portal.getWorld();
            ENTITY_SCRATCH.clear();
            world.collectEntitiesByType(ENTITY_FILTER, portal.crossingBox(), CROSSING_CANDIDATE, ENTITY_SCRATCH);
            for (int j = 0; j < ENTITY_SCRATCH.size(); j++) {
                Entity entity = ENTITY_SCRATCH.get(j);
                if (crossedPortalPlane(portal, entity.prevX, entity.prevY, entity.prevZ, entity.getX(), entity.getY(), entity.getZ())) {
                    execute(entity, portal);
                }
            }
        }
        ENTITY_SCRATCH.clear();
    }

    /** Le client annonce un franchissement : on l'applique s'il est plausible, sinon on le ramene a l'etat serveur. */
    private static void onClientCrossing(ServerPlayerEntity player, PortalCrossPayload payload) {
        PENDING_CROSSINGS.remove(player.getUuid());
        ServerWorld world = player.getServerWorld();
        BackroomsPortalEntity portal = BackroomsPortalEntityRegistry.find(world, payload.portal());
        boolean plausible = portal != null
                && portal.teleportable()
                && !player.hasVehicle()
                && !TpCooldownTracker.isOnCooldown(player.getUuid(), world.getTime(), ModConfig.PORTAL_TELEPORT_COOLDOWN_TICKS)
                && payload.position().squaredDistanceTo(player.getPos()) <= MAX_PREDICTION_DRIFT * MAX_PREDICTION_DRIFT
                && portal.detectionBox().expand(CROSSING_MARGIN + 1.0).contains(payload.position());
        if (plausible && execute(player, portal, payload.position(), payload.velocity(), payload.yaw(), payload.pitch(), true)) {
            return;
        }
        // Le client a deja bascule de son cote : ce payload le renvoie la ou le serveur le voit.
        ServerPlayNetworking.send(player, PortalTeleportPayload.of(world, player.getPos(), player.getYaw(), player.getPitch(), player.getVelocity()));
        ModConstants.LOGGER.debug("[Portal] Franchissement refuse pour {} (portail {}).", player.getName().getString(), payload.portal());
    }

    /**
     * {@code true} si le segment {@code lastPos -> currentPos} traverse le plan du portail depuis le
     * cote d'entree, en un point situe dans le cadre (avec marge). Sert aussi a la prediction client.
     */
    public static boolean crossedPortalPlane(BackroomsPortalEntity portal, Vec3d lastPos, Vec3d currentPos) {
        return crossedPortalPlane(portal, lastPos.x, lastPos.y, lastPos.z, currentPos.x, currentPos.y, currentPos.z);
    }

    /** Variante sans allocation de {@link #crossedPortalPlane(BackroomsPortalEntity, Vec3d, Vec3d)}, pour la boucle des entities. */
    public static boolean crossedPortalPlane(
            BackroomsPortalEntity portal, double lastX, double lastY, double lastZ, double currentX, double currentY, double currentZ) {
        double last = portal.signedDistance(lastX, lastY, lastZ);
        double current = portal.signedDistance(currentX, currentY, currentZ);
        if (last <= 0.0 || current > 0.0) {
            return false;
        }
        double t = last / (last - current);
        return portal.frameContains(
                lastX + (currentX - lastX) * t, lastY + (currentY - lastY) * t, lastZ + (currentZ - lastZ) * t, CROSSING_MARGIN);
    }

    /**
     * Teleporte un objet, un mob ou un projectile a travers un portail : meme transposition que pour un
     * joueur, appliquee directement par le serveur. Le passage pose le cooldown de portail vanilla.
     *
     * @return {@code true} si la teleportation a eu lieu
     */
    public static boolean execute(Entity entity, BackroomsPortalEntity portal) {
        ServerWorld world = (ServerWorld) entity.getWorld();
        ServerWorld destinationWorld = world.getServer().getWorld(portal.destinationDim());
        if (destinationWorld == null) {
            return false;
        }
        PortalFacing source = portal.facingSource();
        PortalFacing exitFacing = portal.facingDestination();
        Vec3d exit = PortalFacing.transformPoint(source, portal.getPos(), exitFacing, portal.destinationPos(), entity.getPos())
                .add(exitFacing.normal.multiply(EXIT_OFFSET));
        Vec3d exitVelocity = PortalFacing.transformVelocity(source, exitFacing, entity.getVelocity());
        Vec2f look = PortalFacing.transformLook(source, exitFacing, entity.getYaw(), entity.getPitch());
        Entity arrived;
        if (destinationWorld == world) {
            entity.refreshPositionAndAngles(exit.x, exit.y, exit.z, look.x, look.y);
            entity.setVelocity(exitVelocity);
            arrived = entity;
        } else {
            arrived = entity.teleportTo(new TeleportTarget(destinationWorld, exit, exitVelocity, look.x, look.y, false, TeleportTarget.NO_OP));
            if (arrived == null) {
                return false;
            }
            // Vanilla ne reprend que le yaw de la cible : le pitch suit, pour les projectiles.
            arrived.setPitch(look.y);
        }
        arrived.setPortalCooldown(ENTITY_PORTAL_COOLDOWN_TICKS);
        ModConstants.LOGGER.debug("[Portal] {} traverse un portail vers {}.", entity.getType().getUntranslatedName(), portal.destinationDim().getValue());
        return true;
    }

    /**
     * Teleporte un joueur a travers un portail depuis l'etat courant connu du serveur.
     *
     * @param player joueur a teleporter
     * @param portal portail traverse
     * @return {@code true} si la teleportation a eu lieu
     */
    public static boolean execute(ServerPlayerEntity player, BackroomsPortalEntity portal) {
        return execute(player, portal, player.getPos(), player.getVelocity(), player.getYaw(), player.getPitch(), false);
    }

    /**
     * Teleporte un joueur a travers un portail depuis un etat donne (celui du client au franchissement,
     * ou celui du serveur). Position, vitesse et regard sont transposes par {@link PortalFacing}.
     *
     * @param predicted {@code true} si le client a lui-meme franchi le portail et annonce cet etat : il est deja de
     *                  l'autre cote, le serveur s'aligne sans paquet de teleportation vanilla
     * @return {@code true} si la teleportation a eu lieu
     */
    public static boolean execute(
            ServerPlayerEntity player, BackroomsPortalEntity portal, Vec3d position, Vec3d velocity, float yaw, float pitch, boolean predicted) {
        ServerWorld destinationWorld = player.getServerWorld().getServer().getWorld(portal.destinationDim());
        if (destinationWorld == null) {
            ModConstants.LOGGER.warn("[Portal] Teleportation annulee, dimension d'arrivee absente : {}.",
                    portal.destinationDim().getValue());
            return false;
        }
        PortalFacing source = portal.facingSource();
        PortalFacing exitFacing = portal.facingDestination();
        Vec3d exit = PortalFacing.transformPoint(source, portal.getPos(), exitFacing, portal.destinationPos(), position)
                .add(exitFacing.normal.multiply(EXIT_OFFSET));
        exit = liftOutOfBlocks(player, destinationWorld, exit);
        Vec3d exitVelocity = PortalFacing.transformVelocity(source, exitFacing, velocity);
        Vec2f look = PortalFacing.transformLook(source, exitFacing, yaw, pitch);

        player.setCameraEntity(player);
        player.stopRiding();
        if (destinationWorld == player.getServerWorld()) {
            placePlayer(player, exit, look.x, look.y, predicted);
            player.networkHandler.syncWithPlayerPosition();
            ServerPlayNetworking.send(player, PortalTeleportPayload.of(destinationWorld, exit, look.x, look.y, exitVelocity));
        } else {
            changeDimension(player, destinationWorld, exit, look.x, look.y, exitVelocity, predicted);
        }
        player.setVelocity(exitVelocity);
        player.velocityModified = true;

        TpCooldownTracker.markTeleported(player.getUuid(), destinationWorld.getTime());
        LAST_PLAYER_POS.put(player.getUuid(), player.getPos());
        ModConstants.LOGGER.info("[Portal] {} traverse un portail vers {} {}.",
                player.getName().getString(), portal.destinationDim().getValue(), exit);
        return true;
    }

    /**
     * Changement de dimension sans paquet de respawn : la sequence de {@code ServerPlayerEntity.teleportTo},
     * precedee de notre propre annonce pour que le client bascule sur son monde distant deja charge.
     */
    private static void changeDimension(
            ServerPlayerEntity player, ServerWorld destination, Vec3d exit, float yaw, float pitch, Vec3d velocity, boolean predicted) {
        ServerWorld origin = player.getServerWorld();
        PlayerManager playerManager = origin.getServer().getPlayerManager();
        WorldProperties properties = destination.getLevelProperties();
        ServerPlayNetworking.send(player, PortalTeleportPayload.of(destination, exit, yaw, pitch, velocity));
        player.networkHandler.sendPacket(new DifficultyS2CPacket(properties.getDifficulty(), properties.isDifficultyLocked()));
        playerManager.sendCommandTree(player);
        origin.removePlayer(player, Entity.RemovalReason.CHANGED_DIMENSION);
        ((EntityAccessor) player).backrooms$unsetRemoved();
        player.setServerWorld(destination);
        // Avant que la destination ne suive le joueur : les chunks qu'il tient deja ne lui seront pas renvoyes, et ses
        // copies distantes d'entities sont retirees chez lui d'abord.
        PortalViewTracker.keepHeldChunks(player, destination.getRegistryKey());
        PortalViewTracker.onPlayerChangedWorld(player);
        placePlayer(player, exit, yaw, pitch, predicted);
        player.networkHandler.syncWithPlayerPosition();
        destination.onDimensionChanged(player);
        Criteria.CHANGED_DIMENSION.trigger(player, origin.getRegistryKey(), destination.getRegistryKey());
        player.networkHandler.sendPacket(new PlayerAbilitiesS2CPacket(player.getAbilities()));
        playerManager.sendWorldInfo(player, destination);
        playerManager.sendPlayerStatus(player);
        playerManager.sendStatusEffects(player);
    }

    /**
     * Place le joueur a la sortie. Apres une prediction, il y est deja : le serveur se contente de s'aligner, sans
     * paquet de teleportation vanilla, qui le ramenerait la ou il etait un tick plus tot et annulerait son elan.
     * Sinon la teleportation vanilla s'impose : elle ecarte les deplacements deja en route depuis l'ancienne position.
     */
    private static void placePlayer(ServerPlayerEntity player, Vec3d exit, float yaw, float pitch, boolean predicted) {
        if (predicted) {
            player.refreshPositionAndAngles(exit.x, exit.y, exit.z, yaw, pitch);
        } else {
            player.networkHandler.requestTeleport(exit.x, exit.y, exit.z, yaw, pitch);
        }
    }

    /** Remonte legerement le point d'arrivee si la boite du joueur y chevauche un bloc (sol irregulier). */
    private static Vec3d liftOutOfBlocks(ServerPlayerEntity player, ServerWorld destinationWorld, Vec3d exit) {
        Box box = player.getBoundingBox().offset(exit.subtract(player.getPos()));
        if (destinationWorld.isSpaceEmpty(player, box)) {
            return exit;
        }
        for (double lift = LIFT_STEP; lift <= MAX_LIFT; lift += LIFT_STEP) {
            if (destinationWorld.isSpaceEmpty(player, box.offset(0.0, lift, 0.0))) {
                return exit.add(0.0, lift, 0.0);
            }
        }
        return exit;
    }
}
