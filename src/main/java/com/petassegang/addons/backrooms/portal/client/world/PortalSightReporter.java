package com.petassegang.addons.backrooms.portal.client.world;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.petassegang.addons.backrooms.portal.client.view.PortalRenderState;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.backrooms.portal.sync.PortalSightPayload;
import com.petassegang.addons.config.ModConfig;

/**
 * Rapporte au serveur les portails que voit la camera du joueur ({@code PortalSight}) : ceux que le rendu a dessines a la
 * derniere image, dont un point du cadre se voit sans bloc opaque entre lui et la camera (le verre, les feuilles, une porte
 * ouverte laissent voir), et celui par lequel la camera regarde en troisieme personne, passee de l'autre cote d'un cadre.
 * Le rendu note ces portails a chaque image ; le rapport part quand ce qui est vu change, au changement de monde, et au
 * moins chaque seconde. Sans rendu distant (stencil refuse, rendu coupe apres une erreur), rien ne part : le serveur,
 * sans nouvelles, tient le joueur pour tout voir, plutot que de lui faire perdre ce qu'il a sous les yeux.
 *
 * <p>Juste apres une traversee, la derniere image est encore celle de l'ancien monde : rien ne part avant une image du
 * nouveau, sans quoi le serveur recevrait, pour le monde d'arrivee, ce qu'on voyait au depart (et la sortie fugace que le
 * joueur regarde disparaitrait).
 */
public final class PortalSightReporter {

    private static final int REPORT_INTERVAL_TICKS = 4;
    /** Delai au bout duquel le rapport repart, meme inchange : le serveur ne garde pas un rapport trop vieux. */
    private static final int HEARTBEAT_TICKS = 20;
    /** Points du cadre essayes par cote, une grille de trois sur trois, un peu en retrait de ses bords. */
    private static final int FRAME_SAMPLES = 3;
    private static final double FRAME_INSET = 0.9;
    /** Le rayon s'arrete un peu avant le plan : un mur juste derriere ne le cache pas. */
    private static final double SIGHT_MARGIN = 0.1;
    /** Le regard s'arrete sur un cube plein et opaque ; le verre, les feuilles, une dalle ou une porte laissent voir. */
    private static final BiFunction<ClientWorld, BlockPos, Boolean> BLOCKS_SIGHT =
            (world, pos) -> world.getBlockState(pos).isOpaqueFullCube(world, pos) ? Boolean.TRUE : null;
    private static final Function<ClientWorld, Boolean> CLEAR_SIGHT = world -> Boolean.FALSE;
    private static final List<BackroomsPortalEntity> DRAWN = new ArrayList<>();
    /** Portails vus a ce passage, liste reprise d'un passage a l'autre ; un rapport qui part en emporte une copie. */
    private static final List<UUID> SEEN = new ArrayList<>();
    private static @Nullable BackroomsPortalEntity lookedThrough;
    /** Monde du joueur a la derniere image : les portails notes sont les siens. */
    private static @Nullable ClientWorld frameWorld;
    private static List<UUID> sent = List.of();
    private static @Nullable RegistryKey<World> sentWorld;
    private static int ticks;
    private static int sinceSent;

    private PortalSightReporter() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Debut d'une image du monde du joueur : rien n'est encore dessine. */
    public static void beginFrame() {
        DRAWN.clear();
        lookedThrough = null;
        frameWorld = MinecraftClient.getInstance().world;
    }

    /** Portails du monde du joueur que le rendu dessine a cette image ; un par un, sans copie de la liste. */
    public static void noteDrawn(List<BackroomsPortalEntity> portals) {
        for (int i = 0; i < portals.size(); i++) {
            DRAWN.add(portals.get(i));
        }
    }

    /** Portail par lequel la camera regarde, passee de l'autre cote de son cadre en troisieme personne. */
    public static void noteLookedThrough(BackroomsPortalEntity portal) {
        lookedThrough = portal;
    }

    /** Hors partie : plus rien de vu ni d'envoye. */
    public static void clear() {
        DRAWN.clear();
        lookedThrough = null;
        frameWorld = null;
        sent = List.of();
        sentWorld = null;
    }

    /** Tick client : toutes les {@value #REPORT_INTERVAL_TICKS} ticks, le rapport repart s'il le faut. */
    public static void onClientTick(MinecraftClient client) {
        if (!ModConfig.ENABLE_PORTAL_FEATURE || client.world == null || client.player == null || ++ticks % REPORT_INTERVAL_TICKS != 0
                || frameWorld != client.world || !PortalRenderState.remoteRenderingAvailable()) {
            return;
        }
        SEEN.clear();
        if (lookedThrough != null && !lookedThrough.isRemoved()) {
            SEEN.add(lookedThrough.getUuid());
        }
        Vec3d camera = client.gameRenderer.getCamera().getPos();
        for (int i = 0; i < DRAWN.size() && SEEN.size() < PortalSightPayload.MAX_PORTALS; i++) {
            BackroomsPortalEntity portal = DRAWN.get(i);
            if (portal != lookedThrough && !portal.isRemoved() && inSight(client, camera, portal)) {
                SEEN.add(portal.getUuid());
            }
        }
        RegistryKey<World> world = client.world.getRegistryKey();
        sinceSent += REPORT_INTERVAL_TICKS;
        if (!SEEN.equals(sent) || world != sentWorld || sinceSent >= HEARTBEAT_TICKS) {
            // Le paquet s'ecrit plus tard, sur le fil du reseau : il emporte sa propre liste.
            sent = List.copyOf(SEEN);
            ClientPlayNetworking.send(new PortalSightPayload(sent));
            sentWorld = world;
            sinceSent = 0;
        }
    }

    /**
     * {@code true} si un point du cadre se voit de la camera, sans bloc opaque entre eux. Composantes a la main : seul le
     * bout du rayon est cree, le parcours de vanilla n'acceptant que lui.
     */
    private static boolean inSight(MinecraftClient client, Vec3d camera, BackroomsPortalEntity portal) {
        ClientWorld world = client.world;
        PortalFacing facing = portal.facingSource();
        Vec3d center = portal.getPos();
        for (int i = 0; i < FRAME_SAMPLES; i++) {
            double w = (i / (FRAME_SAMPLES - 1.0) - 0.5) * portal.width() * FRAME_INSET;
            for (int j = 0; j < FRAME_SAMPLES; j++) {
                double h = (j / (FRAME_SAMPLES - 1.0) - 0.5) * portal.height() * FRAME_INSET;
                double x = center.x + facing.axisW.x * w + facing.axisH.x * h;
                double y = center.y + facing.axisW.y * w + facing.axisH.y * h;
                double z = center.z + facing.axisW.z * w + facing.axisH.z * h;
                double dx = camera.x - x;
                double dy = camera.y - y;
                double dz = camera.z - z;
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double margin = length > 0.0 ? SIGHT_MARGIN / length : 0.0;
                Vec3d end = new Vec3d(x + dx * margin, y + dy * margin, z + dz * margin);
                if (!BlockView.raycast(camera, end, world, BLOCKS_SIGHT, CLEAR_SIGHT)) {
                    return true;
                }
            }
        }
        return false;
    }
}
