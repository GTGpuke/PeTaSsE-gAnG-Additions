package com.gtgpuke.gtgsbackrooms.backrooms.portal.sync;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Ce que chaque joueur voit des portails, tel que son client le rapporte : les portails dessines a son ecran (troisieme
 * personne comprise, camera passee de l'autre cote d'un cadre aussi), sans bloc entre sa camera et leur cadre. Seul le
 * client connait sa camera, sa perspective et son champ de vision ; le serveur s'en sert pour la sortie fugace d'un bloc
 * portail, qui ne tient que sous le regard.
 *
 * <p>Un joueur dont le client n'a rien rapporte de recent dans le monde ou il est (il vient d'y arriver, ou son client n'a
 * pas le mod) peut tout voir : rien ne disparait faute de nouvelles. Un joueur qui vient d'arriver dans un monde le peut
 * aussi le temps de se retourner ({@link #ARRIVAL_GRACE_TICKS}) : passe de cote, il a la sortie juste a cote de lui,
 * hors de l'ecran.
 */
public final class PortalSight {

    /** Age, en ticks, au-dela duquel un rapport ne compte plus ; le client en renvoie un au moins chaque seconde. */
    static final int STALE_TICKS = 40;
    /** Ticks, depuis son premier rapport dans un monde, pendant lesquels un joueur qui y arrive peut tout voir. */
    public static final int ARRIVAL_GRACE_TICKS = 20;

    /**
     * Rapport d'un client : son monde, les portails vus, le tick de ce monde ou il est arrive, et celui du premier rapport
     * du joueur dans ce monde.
     */
    private record Report(RegistryKey<World> world, Set<UUID> seen, long tick, long arrivedAt) {
    }

    private static final Map<UUID, Report> REPORTS = new HashMap<>();
    private static boolean initialized;

    private PortalSight() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Branche la reception des rapports, l'oubli a la deconnexion et a l'arret du serveur (en solo, un rapport d'une partie
     * precedente passerait pour recent dans la suivante) ; idempotent.
     */
    public static void init() {
        if (initialized) {
            return;
        }
        ServerPlayNetworking.registerGlobalReceiver(PortalSightPayload.ID, (payload, context) -> report(context.player(), payload));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> REPORTS.remove(handler.player.getUuid()));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> REPORTS.clear());
        initialized = true;
    }

    /**
     * Rapport du client : ce qu'il voit, dans le monde ou il est. Un rapport fait dans un autre monde que celui ou le
     * serveur tient le joueur (parti juste avant qu'il n'arrive, ou passage predit puis refuse) ne dit rien de celui-ci :
     * il est ignore.
     */
    static void report(ServerPlayerEntity player, PortalSightPayload payload) {
        ServerWorld world = player.getServerWorld();
        if (!payload.dimension().equals(world.getRegistryKey().getValue())) {
            return;
        }
        Report previous = REPORTS.get(player.getUuid());
        long now = world.getTime();
        long arrivedAt = previous != null && previous.world() == world.getRegistryKey() ? previous.arrivedAt() : now;
        REPORTS.put(player.getUuid(), new Report(world.getRegistryKey(), Set.copyOf(payload.seen()), now, arrivedAt));
    }

    /**
     * {@code true} si le joueur peut voir l'un de ces portails : un rapport recent de son client le nomme, son client n'a
     * rien rapporte de recent dans le monde ou il est, ou il vient d'y arriver.
     *
     * @param player joueur
     * @param portals identifiants uniques des portails ; les cases vides sont ignorees
     */
    public static boolean seesAny(ServerPlayerEntity player, @Nullable UUID[] portals) {
        Report report = recentReport(player);
        return report == null || player.getServerWorld().getTime() - report.arrivedAt() < ARRIVAL_GRACE_TICKS || names(report, portals);
    }

    /**
     * {@code true} seulement si un rapport recent du client du joueur, dans le monde ou il est, nomme l'un de ces portails :
     * sans nouvelles, rien n'est suppose.
     *
     * @param player joueur
     * @param portals identifiants uniques des portails ; les cases vides sont ignorees
     */
    public static boolean reportsAny(ServerPlayerEntity player, @Nullable UUID[] portals) {
        Report report = recentReport(player);
        return report != null && names(report, portals);
    }

    /** Dernier rapport du joueur s'il vaut encore : fait dans le monde ou il est, il y a moins de {@value #STALE_TICKS} ticks. */
    private static @Nullable Report recentReport(ServerPlayerEntity player) {
        Report report = REPORTS.get(player.getUuid());
        ServerWorld world = player.getServerWorld();
        return report == null || report.world() != world.getRegistryKey() || world.getTime() - report.tick() > STALE_TICKS ? null : report;
    }

    private static boolean names(Report report, @Nullable UUID[] portals) {
        for (UUID portal : portals) {
            if (portal != null && report.seen().contains(portal)) {
                return true;
            }
        }
        return false;
    }
}
