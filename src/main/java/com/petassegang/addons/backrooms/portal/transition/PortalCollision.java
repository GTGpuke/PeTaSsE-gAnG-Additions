package com.petassegang.addons.backrooms.portal.transition;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntityRegistry;

/**
 * Collision a travers les portails ouverts : une entity engagee dans un cadre heurte aussi les blocs de l'autre cote.
 *
 * <p>Tant que ses pieds sont devant le plan et que sa boite touche le cadre, les blocs de l'autre cote, au-dela du plan
 * d'arrivee, sont ramenes de ce cote, derriere le plan. On ne s'engage pas la ou l'autre cote est un mur ou le battant
 * ouvert d'une porte, et un sol plus haut que le bas du cadre se monte comme une marche : personne ne ressort dans un
 * bloc. Les blocs de ce cote, derriere le plan, restent : le plan d'une porte passe au milieu de son bloc, et une entity
 * plus etroite qu'un bloc l'a franchi avant d'y toucher.
 *
 * <p>Le client l'applique a son joueur, qui decide de ses deplacements ; le serveur l'applique aux mobs, objets et
 * projectiles, pas aux joueurs : il refuserait des pas qu'un client sans monde distant charge aurait faits librement.
 */
public final class PortalCollision {

    /** Portails d'un monde et monde ou chacun mene : registre et mondes du serveur, ou index et mondes distants du client. */
    public interface Worlds {

        /** Portails charges dans ce monde, ou plus largement ; liste vivante, lue par index. */
        List<BackroomsPortalEntity> portals(World world);

        /** Monde d'arrivee d'un portail, ou {@code null} s'il n'est pas disponible de ce cote. */
        @Nullable World destination(BackroomsPortalEntity portal);
    }

    /** Cote serveur : le registre des portails charges, tous mondes confondus, et les mondes du serveur. */
    public static final Worlds SERVER_WORLDS = new Worlds() {
        @Override
        public List<BackroomsPortalEntity> portals(World world) {
            return BackroomsPortalEntityRegistry.all();
        }

        @Override
        public @Nullable World destination(BackroomsPortalEntity portal) {
            // Le registre ne tient que des portails serveur : leur monde connait le serveur.
            return portal.getServer().getWorld(portal.destinationDim());
        }
    };

    private static Worlds worlds = SERVER_WORLDS;

    private PortalCollision() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Branche les portails du client en plus de ceux du serveur (point d'entree client). */
    public static void setWorlds(Worlds clientAndServer) {
        worlds = clientAndServer;
    }

    /**
     * Ajoute aux collisions d'un deplacement les blocs de l'autre cote des portails ou l'entity est engagee.
     *
     * @param entity entity qui se deplace ; un joueur cote serveur n'est pas concerne
     * @param world monde de l'entity
     * @param collisions collisions vanilla du deplacement
     * @param movingBox boite de l'entity etiree par son deplacement
     * @return les collisions recues, ou une nouvelle liste qui les complete
     */
    public static List<VoxelShape> withOtherSide(Entity entity, World world, List<VoxelShape> collisions, Box movingBox) {
        if (entity instanceof ServerPlayerEntity) {
            return collisions;
        }
        List<BackroomsPortalEntity> portals = worlds.portals(world);
        List<VoxelShape> result = collisions;
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (portal.getWorld() != world || !engages(portal, entity, movingBox)) {
                continue;
            }
            World destination = worlds.destination(portal);
            if (destination == null) {
                continue;
            }
            if (result == collisions) {
                result = new ArrayList<>(collisions);
            }
            addOtherSide(portal, destination, movingBox, result);
        }
        return result;
    }

    /** L'entity a les pieds devant le plan d'un portail ouvert, et sa boite deplacee touche le cadre. */
    static boolean engages(BackroomsPortalEntity portal, Entity entity, Box movingBox) {
        return portal.teleportable()
                && portal.signedDistance(entity.getX(), entity.getY(), entity.getZ()) > 0.0
                && portal.detectionBox().intersects(movingBox);
    }

    /** Blocs de l'autre cote que la boite deplacee atteindrait au-dela du plan d'arrivee, ramenes de ce cote. */
    private static void addOtherSide(BackroomsPortalEntity portal, World destination, Box movingBox, List<VoxelShape> shapes) {
        Vec3d exitNormal = portal.facingDestination().normal;
        Vec3d exitCenter = portal.destinationPos();
        for (VoxelShape shape : destination.getBlockCollisions(null, toDestination(portal, movingBox))) {
            for (Box box : shape.getBoundingBoxes()) {
                Box beyond = exitSide(box, exitNormal, exitCenter);
                if (beyond != null) {
                    shapes.add(VoxelShapes.cuboid(fromDestination(portal, beyond)));
                }
            }
        }
    }

    /** Image d'une boite de ce cote dans le monde d'arrivee : les orientations suivent les axes, l'image d'une boite en est une. */
    static Box toDestination(BackroomsPortalEntity portal, Box box) {
        return new Box(portal.toDestination(new Vec3d(box.minX, box.minY, box.minZ)), portal.toDestination(new Vec3d(box.maxX, box.maxY, box.maxZ)));
    }

    /** Image de ce cote d'une boite du monde d'arrivee. */
    static Box fromDestination(BackroomsPortalEntity portal, Box box) {
        return new Box(portal.fromDestination(new Vec3d(box.minX, box.minY, box.minZ)), portal.fromDestination(new Vec3d(box.maxX, box.maxY, box.maxZ)));
    }

    /**
     * Partie d'une boite situee du cote de sortie d'un plan aligne sur un axe.
     *
     * @param box boite a couper
     * @param normal normale du plan, vers la sortie
     * @param center point du plan
     * @return la partie du cote de sortie, ou {@code null} si la boite est entierement derriere le plan
     */
    static @Nullable Box exitSide(Box box, Vec3d normal, Vec3d center) {
        double minX = normal.x > 0.0 ? Math.max(box.minX, center.x) : box.minX;
        double minY = normal.y > 0.0 ? Math.max(box.minY, center.y) : box.minY;
        double minZ = normal.z > 0.0 ? Math.max(box.minZ, center.z) : box.minZ;
        double maxX = normal.x < 0.0 ? Math.min(box.maxX, center.x) : box.maxX;
        double maxY = normal.y < 0.0 ? Math.min(box.maxY, center.y) : box.maxY;
        double maxZ = normal.z < 0.0 ? Math.min(box.maxZ, center.z) : box.maxZ;
        return minX < maxX && minY < maxY && minZ < maxZ ? new Box(minX, minY, minZ, maxX, maxY, maxZ) : null;
    }
}
