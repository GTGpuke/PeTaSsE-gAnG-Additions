package com.petassegang.addons.backrooms.portal.client.transition;

import java.util.List;

import it.unimi.dsi.fastutil.ints.IntList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.petassegang.addons.backrooms.portal.client.world.ClientPortalIndex;
import com.petassegang.addons.backrooms.portal.client.world.RemoteWorld;
import com.petassegang.addons.backrooms.portal.client.world.RemoteWorldRegistry;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.backrooms.portal.sync.PortalCrossPayload;
import com.petassegang.addons.backrooms.portal.sync.PortalTeleportPayload;
import com.petassegang.addons.backrooms.portal.transition.TeleportExecutor;
import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.mixin.EntityAccessor;
import com.petassegang.addons.mixin.client.ClientPlayNetworkHandlerAccessor;
import com.petassegang.addons.mixin.client.MinecraftClientAccessor;

/**
 * Traversee sans coupure cote client : prediction du passage et bascule de monde sans ecran de chargement.
 *
 * <p>A chaque image, si la camera a franchi le plan d'un portail depuis l'image precedente, le joueur
 * est teleporte localement (position, regard et vitesse transposes), le client bascule sur le monde
 * distant deja charge et previent le serveur. Tant que le serveur n'a pas confirme, les paquets
 * vanilla continuent d'alimenter l'ancien monde, qui devient un monde distant : on peut se retourner
 * et voir d'ou l'on vient. A la confirmation, le flux de paquets suit le joueur ; les dechargements
 * que vanilla envoie alors pour l'ancien monde sont ignores pour en garder la vue.
 */
public final class ClientPortalCrossing {

    /** Delai de securite avant de considerer qu'une prediction est restee sans reponse. */
    private static final int SERVER_ANSWER_TIMEOUT_TICKS = 60;
    /** Duree maximale pendant laquelle les dechargements vanilla de l'ancien monde sont ignores. */
    private static final int UNLOAD_WINDOW_TICKS = 40;
    /** Ecart au-dela duquel la sortie confirmee par le serveur differe vraiment de celle predite (il a pu relever le joueur d'un sol irregulier). */
    private static final double CONFIRMATION_TOLERANCE_SQUARED = 0.3 * 0.3;

    /** Pieds du joueur a l'image precedente, en primitifs : ce test tourne a chaque image. */
    private static double lastFeetX;
    private static double lastFeetY;
    private static double lastFeetZ;
    private static boolean hasLastFeet;
    private static @Nullable ClientWorld lastFeetWorld;
    private static int lastCrossingAge = Integer.MIN_VALUE / 2;
    private static int awaitingServerTicks;
    private static int unloadWindowTicks;
    private static @Nullable ClientWorld previousWorld;
    /** Sortie calculee par la derniere prediction, en attente de confirmation. */
    private static @Nullable Vec3d predictedExit;

    private ClientPortalCrossing() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Enregistre le recepteur de teleportation ; a appeler une fois depuis le point d'entree client. */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PortalTeleportPayload.ID,
                (payload, context) -> onServerTeleport(context.client(), payload));
    }

    /**
     * Appele au debut de chaque rendu du monde : detecte le franchissement du plan d'un portail par le joueur.
     *
     * @param tickDelta fraction de tick de l'image
     */
    public static void predictCrossing(float tickDelta) {
        if (!ModConfig.ENABLE_PORTAL_FEATURE) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        if (player == null || world == null) {
            return;
        }
        // Position affichee a cette image, pas celle du tick : la continuite visuelle se joue la. Le test
        // porte sur les pieds, exactement comme celui du serveur : les portails sont verticaux, la camera
        // franchit le plan au meme instant, et client comme serveur prennent la meme decision.
        double feetX = MathHelper.lerp(tickDelta, player.prevX, player.getX());
        double feetY = MathHelper.lerp(tickDelta, player.prevY, player.getY());
        double feetZ = MathHelper.lerp(tickDelta, player.prevZ, player.getZ());
        boolean hasFrom = hasLastFeet && lastFeetWorld == world;
        double fromX = lastFeetX;
        double fromY = lastFeetY;
        double fromZ = lastFeetZ;
        lastFeetX = feetX;
        lastFeetY = feetY;
        lastFeetZ = feetZ;
        hasLastFeet = true;
        lastFeetWorld = world;
        if (!hasFrom || awaitingServerTicks > 0 || player.hasVehicle()
                || player.age - lastCrossingAge < ModConfig.PORTAL_TELEPORT_COOLDOWN_TICKS
                || squaredDistance(fromX, fromY, fromZ, feetX, feetY, feetZ) > TeleportExecutor.MAX_STEP_SQUARED) {
            // Un saut de position (commande, respawn) traverse les plans sans les franchir : meme garde que le serveur.
            return;
        }
        List<BackroomsPortalEntity> portals = ClientPortalIndex.portals(world);
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (TeleportExecutor.crossedPortalPlane(portal, fromX, fromY, fromZ, feetX, feetY, feetZ)) {
                predict(client, player, world, portal, new Vec3d(feetX, feetY, feetZ));
                return;
            }
        }
    }

    private static double squaredDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Tick client : expiration des attentes. */
    public static void onClientTick() {
        if (awaitingServerTicks > 0) {
            awaitingServerTicks--;
        }
        if (unloadWindowTicks > 0 && --unloadWindowTicks == 0) {
            previousWorld = null;
        }
    }

    /** Deconnexion : plus rien a attendre. */
    public static void reset() {
        hasLastFeet = false;
        lastFeetWorld = null;
        awaitingServerTicks = 0;
        unloadWindowTicks = 0;
        previousWorld = null;
        predictedExit = null;
    }

    /** {@code true} tant que les dechargements de chunks vanilla concernent l'ancien monde et doivent etre ignores. */
    public static boolean swallowsVanillaUnloads() {
        return unloadWindowTicks > 0;
    }

    /**
     * Pendant la fenetre de bascule, les entities detruites par vanilla sont celles de l'ancien monde.
     *
     * @return {@code true} si les identifiants ont ete traites et que vanilla ne doit pas les appliquer
     */
    public static boolean redirectDestroyedEntities(IntList entityIds) {
        if (unloadWindowTicks <= 0 || previousWorld == null) {
            return false;
        }
        for (int i = 0; i < entityIds.size(); i++) {
            previousWorld.removeEntity(entityIds.getInt(i), Entity.RemovalReason.DISCARDED);
        }
        return true;
    }

    /** Le serveur recentre le suivi de chunks dans le nouveau monde : le nettoyage de l'ancien est termine. */
    public static void onVanillaChunkCenter() {
        unloadWindowTicks = 0;
        previousWorld = null;
    }

    private static void predict(MinecraftClient client, ClientPlayerEntity player, ClientWorld world, BackroomsPortalEntity portal, Vec3d feet) {
        RegistryKey<World> destination = portal.destinationDim();
        boolean sameDimension = world.getRegistryKey().equals(destination);
        RemoteWorld remote = RemoteWorldRegistry.get(destination);
        if (!sameDimension && remote == null) {
            // Sans monde distant charge, autant laisser le serveur mener la bascule.
            return;
        }
        Vec3d velocity = player.getVelocity();
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        PortalFacing source = portal.facingSource();
        PortalFacing exitFacing = portal.facingDestination();
        Vec3d exit = PortalFacing.transformPoint(source, portal.getPos(), exitFacing, portal.destinationPos(), feet)
                .add(exitFacing.normal.multiply(TeleportExecutor.EXIT_OFFSET));
        Vec3d exitVelocity = PortalFacing.transformVelocity(source, exitFacing, velocity);
        Vec2f look = PortalFacing.transformLook(source, exitFacing, yaw, pitch);

        // Le serveur doit recevoir l'annonce avant le prochain paquet de deplacement.
        ClientPlayNetworking.send(new PortalCrossPayload(portal.getUuid(), feet, velocity, yaw, pitch));
        predictedExit = exit;
        awaitingServerTicks = SERVER_ANSWER_TIMEOUT_TICKS;
        lastCrossingAge = player.age;
        if (sameDimension) {
            applyPosition(player, exit, look.x, look.y, exitVelocity);
        } else {
            switchWorld(client, remote, exit, look.x, look.y, exitVelocity, false);
        }
    }

    private static void onServerTeleport(MinecraftClient client, PortalTeleportPayload payload) {
        awaitingServerTicks = 0;
        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (player == null || world == null || handler == null) {
            return;
        }
        RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, payload.dimension());
        Vec3d predicted = predictedExit;
        predictedExit = null;
        if (world.getRegistryKey().equals(dimension)) {
            if (predicted == null || predicted.squaredDistanceTo(payload.position()) > CONFIRMATION_TOLERANCE_SQUARED) {
                // Bascule menee par le serveur, ou position corrigee : alignement sur lui.
                applyPosition(player, payload.position(), payload.yaw(), payload.pitch(), payload.velocity());
            }
            // Sinon la prediction est confirmee telle quelle : le joueur a continue d'avancer depuis, on ne le ramene pas en arriere.
            if (handler.getWorld() != world) {
                bindNetworkHandler(client, world, handler.getWorld());
            }
            return;
        }
        RemoteWorld remote = RemoteWorldRegistry.announce(dimension, payload.dimensionType());
        if (remote == null) {
            ModConstants.LOGGER.error("[Portal] Impossible de basculer vers {} : monde distant indisponible.", payload.dimension());
            return;
        }
        switchWorld(client, remote, payload.position(), payload.yaw(), payload.pitch(), payload.velocity(), true);
    }

    /**
     * Echange le monde principal et un monde distant : le joueur change de monde, le client rend le
     * nouveau monde, l'ancien reste disponible pour les portails de retour.
     *
     * @param confirmed {@code true} si le serveur a deja deplace le joueur : le flux de paquets suit alors le joueur
     */
    private static void switchWorld(
            MinecraftClient client,
            RemoteWorld remote,
            Vec3d position,
            float yaw,
            float pitch,
            Vec3d velocity,
            boolean confirmed) {
        ClientPlayerEntity player = client.player;
        ClientWorld oldWorld = client.world;
        WorldRenderer oldRenderer = client.worldRenderer;
        if (player == null || oldWorld == null) {
            return;
        }
        ClientWorld newWorld = remote.world();
        RemoteWorldRegistry.swapMainWorld(oldWorld, oldRenderer, newWorld,
                ChunkSectionPos.getSectionCoord(player.getX()), ChunkSectionPos.getSectionCoord(player.getZ()));

        oldWorld.removeEntity(player.getId(), Entity.RemovalReason.CHANGED_DIMENSION);
        ((EntityAccessor) player).backrooms$unsetRemoved();
        ((EntityAccessor) player).backrooms$setWorld(newWorld);
        applyPosition(player, position, yaw, pitch, velocity);
        newWorld.addEntity(player);

        MinecraftClientAccessor clientAccessor = (MinecraftClientAccessor) client;
        clientAccessor.backrooms$setWorld(newWorld);
        clientAccessor.backrooms$setWorldRenderer(remote.renderer());
        client.particleManager.setWorld(newWorld);
        client.getBlockEntityRenderDispatcher().setWorld(newWorld);
        client.getEntityRenderDispatcher().setWorld(newWorld);
        client.setCameraEntity(player);
        hasLastFeet = false;
        lastFeetWorld = null;
        if (confirmed) {
            bindNetworkHandler(client, newWorld, oldWorld);
        }
        ModConstants.LOGGER.debug("[Portal] Bascule client vers {} ({}).", newWorld.getRegistryKey().getValue(),
                confirmed ? "confirmee par le serveur" : "prediction");
    }

    /** Les paquets vanilla alimentent desormais {@code world} ; le nettoyage de l'ancien monde qui suit est ignore. */
    private static void bindNetworkHandler(MinecraftClient client, ClientWorld world, @Nullable ClientWorld oldWorld) {
        ClientPlayNetworkHandlerAccessor handler = (ClientPlayNetworkHandlerAccessor) client.getNetworkHandler();
        if (handler == null) {
            return;
        }
        handler.backrooms$setWorld(world);
        handler.backrooms$setWorldProperties(world.getLevelProperties());
        previousWorld = oldWorld;
        unloadWindowTicks = UNLOAD_WINDOW_TICKS;
    }

    /** Place le joueur sans interpolation depuis l'ancienne position ni l'ancien regard. */
    private static void applyPosition(ClientPlayerEntity player, Vec3d position, float yaw, float pitch, Vec3d velocity) {
        player.refreshPositionAndAngles(position.x, position.y, position.z, yaw, pitch);
        player.prevYaw = yaw;
        player.prevPitch = pitch;
        player.setHeadYaw(yaw);
        player.prevHeadYaw = yaw;
        player.setBodyYaw(yaw);
        player.prevBodyYaw = yaw;
        player.setVelocity(velocity);
    }
}
