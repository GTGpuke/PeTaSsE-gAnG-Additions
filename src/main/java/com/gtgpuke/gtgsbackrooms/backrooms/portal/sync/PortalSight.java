package com.gtgpuke.gtgsbackrooms.backrooms.portal.sync;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.GlobalPos;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;

/**
 * Ce que chaque joueur voit des sorties fugaces, tel que son client le rapporte : celles qu'il a sous les yeux (troisieme
 * personne comprise, camera passee de l'autre cote d'un cadre aussi), sans bloc entre sa camera et leur cadre. Seul le
 * client connait sa camera, sa perspective et son champ de vision ; le serveur s'en sert pour la sortie fugace d'un bloc
 * portail, qui n'existe que sous le regard.
 *
 * <p>Seul le regard compte : un joueur dont le client n'a rien rapporte de recent ne voit rien. Celui qui vient de
 * ressortir par une sortie fugace la voit encore le temps de se retourner vers elle ({@link #ARRIVAL_GRACE_TICKS}) :
 * passe de cote, il l'a juste a cote de lui, hors de l'ecran.
 */
public final class PortalSight {

    /** Age, en ticks serveur, au-dela duquel un rapport ne compte plus ; le client en renvoie un au moins chaque seconde. */
    public static final int STALE_TICKS = 40;
    /** Ticks serveur pendant lesquels un joueur ressorti par une sortie fugace la voit encore, le temps de se retourner. */
    public static final int ARRIVAL_GRACE_TICKS = 20;

    /**
     * Rapport d'un client : les sorties fugaces vues, et le tick serveur ou il est arrive. Il ne nomme que des portails du
     * monde ou il a ete fait : leurs identifiants sont uniques, il ne dit rien des autres mondes.
     */
    private record Report(Set<UUID> seen, long tick) {
    }

    /** Derniere arrivee d'un joueur par un portail : le bloc proprietaire de ce portail, et le tick serveur. */
    private record Arrival(@Nullable GlobalPos owner, long tick) {
    }

    private static final Map<UUID, Report> REPORTS = new HashMap<>();
    private static final Map<UUID, Arrival> ARRIVALS = new HashMap<>();
    private static boolean initialized;

    private PortalSight() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Branche la reception des rapports, l'oubli a la deconnexion, au changement de monde vanilla et au respawn (ni le
     * dernier rapport ni la derniere arrivee ne valent plus), et a l'arret du serveur (en solo, un rapport d'une partie
     * precedente passerait pour recent dans la suivante) ; idempotent.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        ServerPlayNetworking.registerGlobalReceiver(PortalSightPayload.ID, (payload, context) -> report(context.player(), payload));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.player.getUuid()));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> forget(player.getUuid()));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> forget(newPlayer.getUuid()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            REPORTS.clear();
            ARRIVALS.clear();
        });
        initialized = true;
    }

    private static void forget(UUID player) {
        REPORTS.remove(player);
        ARRIVALS.remove(player);
    }

    /**
     * Rapport du client : ce qu'il voit, dans le monde ou il est. Un rapport fait dans un autre monde que celui ou le
     * serveur tient le joueur (parti juste avant qu'il n'arrive, ou passage predit puis refuse) ne dit rien de celui-ci :
     * il est ignore.
     */
    static void report(ServerPlayerEntity player, PortalSightPayload payload) {
        if (!payload.dimension().equals(player.getServerWorld().getRegistryKey().getValue())) {
            return;
        }
        REPORTS.put(player.getUuid(), new Report(Set.copyOf(payload.seen()), player.getServer().getTicks()));
    }

    /**
     * Le joueur vient de passer un portail. Passe par l'entree d'un bloc, il ressort par sa sortie et la voit encore le temps
     * de se retourner ; repasse par une sortie fugace, il ressort par l'entree : aucune sortie derriere lui.
     *
     * @param player joueur
     * @param portal portail traverse ; son bloc proprietaire designe la sortie par laquelle le joueur ressort
     */
    public static void onArrival(ServerPlayerEntity player, BackroomsPortalEntity portal) {
        ARRIVALS.put(player.getUuid(), new Arrival(portal.fleeting() ? null : portal.owner(), player.getServer().getTicks()));
    }

    /**
     * {@code true} si le joueur voit cette sortie fugace : un rapport recent de son client nomme l'un de ses retours, ou il
     * vient de ressortir par elle.
     *
     * @param player joueur
     * @param portals identifiants uniques des retours de la sortie ; les cases vides sont ignorees
     * @param owner bloc portail auquel la sortie appartient
     */
    public static boolean seesAny(ServerPlayerEntity player, @Nullable UUID[] portals, GlobalPos owner) {
        if (cameOutOf(player, owner)) {
            return true;
        }
        Report report = recentReport(player);
        if (report == null) {
            return false;
        }
        for (UUID portal : portals) {
            if (portal != null && report.seen().contains(portal)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code true} si le joueur voit cette sortie fugace (voir {@link #seesAny}) ; un portail libre, sans bloc, n'a pas de
     * sortie fugace.
     */
    public static boolean sees(ServerPlayerEntity player, BackroomsPortalEntity portal) {
        GlobalPos owner = portal.owner();
        if (owner == null) {
            return false;
        }
        Report report = recentReport(player);
        return cameOutOf(player, owner) || report != null && report.seen().contains(portal.getUuid());
    }

    /** {@code true} si le joueur vient de ressortir par la sortie de ce bloc, il y a moins de {@value #ARRIVAL_GRACE_TICKS} ticks. */
    private static boolean cameOutOf(ServerPlayerEntity player, GlobalPos owner) {
        Arrival arrival = ARRIVALS.get(player.getUuid());
        return arrival != null && owner.equals(arrival.owner()) && player.getServer().getTicks() - arrival.tick() < ARRIVAL_GRACE_TICKS;
    }

    /** Dernier rapport du joueur s'il vaut encore : fait il y a au plus {@value #STALE_TICKS} ticks. */
    private static @Nullable Report recentReport(ServerPlayerEntity player) {
        Report report = REPORTS.get(player.getUuid());
        return report == null || player.getServer().getTicks() - report.tick() > STALE_TICKS ? null : report;
    }
}
