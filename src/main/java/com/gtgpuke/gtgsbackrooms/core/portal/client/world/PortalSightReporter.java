package com.gtgpuke.gtgsbackrooms.core.portal.client.world;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.gtgpuke.gtgsbackrooms.core.ModConfig;
import com.gtgpuke.gtgsbackrooms.core.perf.ModPerformanceMonitor;
import com.gtgpuke.gtgsbackrooms.core.portal.block.BackroomsPortalBlockEntity;
import com.gtgpuke.gtgsbackrooms.core.portal.client.view.PortalRenderState;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.core.portal.sync.PortalSight;
import com.gtgpuke.gtgsbackrooms.core.portal.sync.PortalSightPayload;

/**
 * Ce que le joueur regarde des sorties fugaces : celles que le rendu a dessinees a la derniere image, dont un point du
 * cadre se voit sans cube plein et opaque entre lui et la camera (le verre, les feuilles, une porte ouverte laissent voir),
 * et celle par laquelle la camera regarde en troisieme personne, passee de l'autre cote de son cadre.
 *
 * <p>Une sortie fugace n'existe pour le joueur que sous son regard : hors de vue plus de
 * {@value BackroomsPortalBlockEntity#EXIT_UNSEEN_TICKS} ticks, elle disparait pour lui (ni dessinee, ni traversee, ni
 * heurtee, ni visee, voir {@link ClientPortalIndex#portals}). Tant qu'elle existe (un autre joueur la regarde), elle revient
 * des l'image ou il la voit de nouveau ({@link #revealed}). Qui vient de passer un portail garde celles de l'arrivee le
 * temps de se retourner. Le serveur, qui ne la detruit que quand plus personne ne la regarde, recoit les sorties fugaces
 * que le joueur voit quand cela change, au changement de monde, et au moins chaque seconde.
 *
 * <p>Sans rendu distant (stencil refuse, rendu coupe apres une erreur), le joueur ne voit aucune sortie fugace et rien ne
 * part. Juste apres une traversee, la derniere image est encore celle de l'ancien monde : rien n'est juge avant une image du
 * nouveau, sans quoi le serveur recevrait, pour le monde d'arrivee, ce qu'on voyait au depart.
 */
public final class PortalSightReporter {

    /** Delai minimal entre deux rapports qui changent ; un monde nouveau part aussitot. */
    private static final int REPORT_INTERVAL_TICKS = 4;
    /** Delai au bout duquel le rapport repart, meme inchange : le serveur ne garde pas un rapport trop vieux. */
    private static final int HEARTBEAT_TICKS = 20;
    /** Points du cadre essayes par cote, une grille de trois sur trois, un peu en retrait de ses bords. */
    private static final int FRAME_SAMPLES = 3;
    private static final double FRAME_INSET = 0.9;
    /** Le regard s'arrete un peu avant le plan : un mur juste derriere ne le cache pas. */
    private static final double SIGHT_MARGIN = 0.1;
    /** Blocs parcourus au plus par un regard, bien au-dela de la distance de rendu des portails ; au-dela, rien ne le coupe. */
    private static final int MAX_SIGHT_STEPS = 512;
    /** Section du moniteur de performance : le jugement du regard, a chaque tick. */
    private static final String PERF_SIGHT = "client.portal.sight";
    private static final List<BackroomsPortalEntity> DRAWN = new ArrayList<>();
    /** Sorties fugaces vues a ce tick, liste reprise d'un tick a l'autre ; un rapport qui part en emporte une copie. */
    private static final List<UUID> SEEN = new ArrayList<>();
    private static final BlockPos.Mutable CURSOR = new BlockPos.Mutable();
    private static @Nullable BackroomsPortalEntity lookedThrough;
    /** Monde du joueur a la derniere image : les portails notes sont les siens. */
    private static @Nullable ClientWorld frameWorld;
    private static List<UUID> sent = List.of();
    private static @Nullable RegistryKey<World> sentWorld;
    private static long ticks;
    private static int sinceSent;
    /** Tick du dernier passage de portail du joueur : les sorties fugaces de l'arrivee tiennent le temps qu'il se retourne. */
    private static long arrivedAt = Long.MIN_VALUE / 2;

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

    /**
     * Sortie fugace cachee au joueur, dans le champ de cette image : si un point de son cadre se voit de la camera, elle
     * existe de nouveau pour lui, des cette image.
     *
     * @return {@code true} si elle est de nouveau montree, a dessiner
     */
    public static boolean revealed(ClientWorld world, Vec3d camera, BackroomsPortalEntity portal) {
        if (!inSight(world, camera, portal)) {
            return false;
        }
        portal.noteSeen(ticks);
        portal.setHiddenFromPlayer(false);
        ClientPortalIndex.refreshShown(world);
        return true;
    }

    /** Le joueur vient de passer un portail : les sorties fugaces de l'arrivee tiennent le temps qu'il se retourne. */
    public static void onArrival() {
        arrivedAt = ticks;
    }

    /** Hors partie : plus rien de vu ni d'envoye. */
    public static void clear() {
        DRAWN.clear();
        lookedThrough = null;
        frameWorld = null;
        sent = List.of();
        sentWorld = null;
        arrivedAt = Long.MIN_VALUE / 2;
    }

    /** Tick client : ce que le joueur regarde, les sorties fugaces qui existent pour lui, et le rapport s'il le faut. */
    @SuppressWarnings("try")
    public static void onClientTick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (!ModConfig.ENABLE_PORTAL_FEATURE || world == null || client.player == null) {
            return;
        }
        try (ModPerformanceMonitor.Scope ignored = ModPerformanceMonitor.scope(PERF_SIGHT)) {
            judge(client, world);
        }
    }

    private static void judge(MinecraftClient client, ClientWorld world) {
        ticks++;
        boolean judged = frameWorld == world && PortalRenderState.remoteRenderingAvailable();
        if (judged) {
            SEEN.clear();
            if (lookedThrough != null && !lookedThrough.isRemoved()) {
                see(lookedThrough);
            }
            Vec3d camera = client.gameRenderer.getCamera().getPos();
            for (int i = 0; i < DRAWN.size(); i++) {
                BackroomsPortalEntity portal = DRAWN.get(i);
                if (portal.fleeting() && portal != lookedThrough && !portal.isRemoved() && inSight(world, camera, portal)) {
                    see(portal);
                }
            }
        }
        boolean arriving = ticks - arrivedAt <= PortalSight.ARRIVAL_GRACE_TICKS;
        List<BackroomsPortalEntity> portals = ClientPortalIndex.allPortals(world);
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (portal.fleeting()) {
                portal.setHiddenFromPlayer(!arriving && ticks - portal.seenAt() > BackroomsPortalBlockEntity.EXIT_UNSEEN_TICKS);
            }
        }
        ClientPortalIndex.refreshShown(world);
        if (!judged) {
            return;
        }
        RegistryKey<World> key = world.getRegistryKey();
        sinceSent++;
        if (key != sentWorld || sinceSent >= HEARTBEAT_TICKS || sinceSent >= REPORT_INTERVAL_TICKS && !sameAsSent()) {
            // Un envoi, au plus tous les quatre ticks et au moins chaque seconde, emporte sa propre liste : le paquet
            // s'ecrit plus tard, sur le fil du reseau.
            sent = List.copyOf(SEEN);
            ClientPlayNetworking.send(new PortalSightPayload(key.getValue(), sent));
            sentWorld = key;
            sinceSent = 0;
        }
    }

    /** {@code true} si ce qui est vu a ce tick est ce qui a ete envoye ; par index, sans iterateur. */
    private static boolean sameAsSent() {
        if (SEEN.size() != sent.size()) {
            return false;
        }
        for (int i = 0; i < SEEN.size(); i++) {
            if (!SEEN.get(i).equals(sent.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Le joueur voit cette sortie fugace a ce tick ; un portail ordinaire n'est pas rapporte. */
    private static void see(BackroomsPortalEntity portal) {
        if (!portal.fleeting()) {
            return;
        }
        portal.noteSeen(ticks);
        if (SEEN.size() < PortalSightPayload.MAX_PORTALS && !SEEN.contains(portal.getUuid())) {
            SEEN.add(portal.getUuid());
        }
    }

    /** {@code true} si un point du cadre se voit de la camera, sans cube plein et opaque entre eux. */
    private static boolean inSight(ClientWorld world, Vec3d camera, BackroomsPortalEntity portal) {
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
                if (clearLine(world, camera.x, camera.y, camera.z, x + dx * margin, y + dy * margin, z + dz * margin)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** {@code true} si aucun cube plein et opaque ne coupe le segment : parcours bloc par bloc, sans allocation. */
    private static boolean clearLine(ClientWorld world, double x0, double y0, double z0, double x1, double y1, double z1) {
        int x = MathHelper.floor(x0);
        int y = MathHelper.floor(y0);
        int z = MathHelper.floor(z0);
        int stepX = Double.compare(x1, x0);
        int stepY = Double.compare(y1, y0);
        int stepZ = Double.compare(z1, z0);
        // Fraction du segment a parcourir pour franchir un bloc sur chaque axe, puis pour atteindre le prochain bord.
        double deltaX = stepX == 0 ? Double.MAX_VALUE : 1.0 / Math.abs(x1 - x0);
        double deltaY = stepY == 0 ? Double.MAX_VALUE : 1.0 / Math.abs(y1 - y0);
        double deltaZ = stepZ == 0 ? Double.MAX_VALUE : 1.0 / Math.abs(z1 - z0);
        double nextX = stepX == 0 ? Double.MAX_VALUE : (stepX > 0 ? x + 1 - x0 : x0 - x) * deltaX;
        double nextY = stepY == 0 ? Double.MAX_VALUE : (stepY > 0 ? y + 1 - y0 : y0 - y) * deltaY;
        double nextZ = stepZ == 0 ? Double.MAX_VALUE : (stepZ > 0 ? z + 1 - z0 : z0 - z) * deltaZ;
        for (int step = 0; step < MAX_SIGHT_STEPS; step++) {
            CURSOR.set(x, y, z);
            if (world.getBlockState(CURSOR).isOpaqueFullCube(world, CURSOR)) {
                return false;
            }
            // Aucun bord franchi avant le bout du segment : il s'arrete dans ce bloc.
            if (nextX > 1.0 && nextY > 1.0 && nextZ > 1.0) {
                return true;
            }
            if (nextX <= nextY && nextX <= nextZ) {
                x += stepX;
                nextX += deltaX;
            } else if (nextY <= nextZ) {
                y += stepY;
                nextY += deltaY;
            } else {
                z += stepZ;
                nextZ += deltaZ;
            }
        }
        return true;
    }
}
