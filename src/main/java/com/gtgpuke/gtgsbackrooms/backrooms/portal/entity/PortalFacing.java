package com.gtgpuke.gtgsbackrooms.backrooms.portal.entity;

import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

/**
 * Orientation d'un portail aligne sur une direction cardinale, et passage d'un portail a l'autre.
 *
 * <p>Chaque orientation definit une base orthonormee directe : largeur W, hauteur H, normale N = W x H.
 * La normale pointe vers le cote d'entree : le joueur se tient du cote +N et avance vers -N pour
 * traverser. A l'arrivee, il ressort du cote +N' du portail de destination, en s'eloignant de lui.
 * Ce qui est devant la source est donc derriere la destination ; le voyageur garde sa droite (vue de face, la
 * largeur W de la source s'inverse a l'arrivee) : le passage est une rotation de 180 degres autour de l'axe H, jamais
 * un miroir.
 */
public enum PortalFacing {
    NORTH(Direction.NORTH, new Vec3d(-1.0, 0.0, 0.0), new Vec3d(0.0, 1.0, 0.0)),
    SOUTH(Direction.SOUTH, new Vec3d(1.0, 0.0, 0.0), new Vec3d(0.0, 1.0, 0.0)),
    EAST(Direction.EAST, new Vec3d(0.0, 0.0, -1.0), new Vec3d(0.0, 1.0, 0.0)),
    WEST(Direction.WEST, new Vec3d(0.0, 0.0, 1.0), new Vec3d(0.0, 1.0, 0.0)),
    UP(Direction.UP, new Vec3d(1.0, 0.0, 0.0), new Vec3d(0.0, 0.0, -1.0)),
    DOWN(Direction.DOWN, new Vec3d(1.0, 0.0, 0.0), new Vec3d(0.0, 0.0, 1.0));

    /** Direction cardinale de la normale. */
    public final Direction direction;
    /** Axe de largeur du portail (unitaire). */
    public final Vec3d axisW;
    /** Axe de hauteur du portail (unitaire). */
    public final Vec3d axisH;
    /** Normale du portail, cote d'entree (unitaire). */
    public final Vec3d normal;

    /** Orientations, sans la copie de {@code values()} a chaque recherche. */
    private static final PortalFacing[] VALUES = values();

    PortalFacing(Direction direction, Vec3d axisW, Vec3d axisH) {
        this.direction = direction;
        this.axisW = axisW;
        this.axisH = axisH;
        this.normal = axisW.crossProduct(axisH);
    }

    /**
     * Orientation dont la normale suit une direction.
     *
     * @param direction direction cardinale, non nulle
     * @return orientation correspondante
     * @throws IllegalArgumentException si la direction est nulle
     */
    public static PortalFacing fromDirection(Direction direction) {
        for (PortalFacing facing : VALUES) {
            if (facing.direction == direction) {
                return facing;
            }
        }
        throw new IllegalArgumentException("Direction non geree : " + direction + ".");
    }

    /**
     * Orientation de la face arriere d'un portail : meme plan, normale opposee. Deux portails dos a dos, chacun vers la
     * face correspondante de l'autre cote, font la meme transformation : un trou qui se traverse dans les deux sens.
     */
    public PortalFacing opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
            case UP -> DOWN;
            case DOWN -> UP;
        };
    }

    /**
     * Fait passer un vecteur (vitesse, regard, deplacement) de la base de la source a celle de la destination.
     *
     * @param source orientation du portail traverse
     * @param destination orientation du portail d'arrivee
     * @param vector vecteur exprime dans le monde de la source
     * @return le meme vecteur exprime dans le monde de la destination
     */
    public static Vec3d transformVector(PortalFacing source, PortalFacing destination, Vec3d vector) {
        double w = dot(source.axisW, vector.x, vector.y, vector.z);
        double h = dot(source.axisH, vector.x, vector.y, vector.z);
        double n = dot(source.normal, vector.x, vector.y, vector.z);
        return new Vec3d(rotated(destination.axisW.x, destination.axisH.x, destination.normal.x, w, h, n),
                rotated(destination.axisW.y, destination.axisH.y, destination.normal.y, w, h, n),
                rotated(destination.axisW.z, destination.axisH.z, destination.normal.z, w, h, n));
    }

    /**
     * Fait passer un point du monde de la source au monde de la destination.
     *
     * @param source orientation du portail traverse
     * @param sourceCenter centre du portail traverse
     * @param destination orientation du portail d'arrivee
     * @param destinationCenter centre du portail d'arrivee
     * @param point point du monde de la source
     * @return point correspondant dans le monde de la destination
     */
    public static Vec3d transformPoint(
            PortalFacing source,
            Vec3d sourceCenter,
            PortalFacing destination,
            Vec3d destinationCenter,
            Vec3d point) {
        double x = point.x - sourceCenter.x;
        double y = point.y - sourceCenter.y;
        double z = point.z - sourceCenter.z;
        double w = dot(source.axisW, x, y, z);
        double h = dot(source.axisH, x, y, z);
        double n = dot(source.normal, x, y, z);
        return new Vec3d(destinationCenter.x + rotated(destination.axisW.x, destination.axisH.x, destination.normal.x, w, h, n),
                destinationCenter.y + rotated(destination.axisW.y, destination.axisH.y, destination.normal.y, w, h, n),
                destinationCenter.z + rotated(destination.axisW.z, destination.axisH.z, destination.normal.z, w, h, n));
    }

    /**
     * Fait passer un regard (yaw, pitch en degres) de la source a la destination.
     *
     * @param source orientation du portail traverse
     * @param destination orientation du portail d'arrivee
     * @param yaw lacet en degres, convention Minecraft
     * @param pitch tangage en degres, convention Minecraft
     * @return nouveau regard : {@code x} = yaw, {@code y} = pitch
     */
    public static Vec2f transformLook(PortalFacing source, PortalFacing destination, float yaw, float pitch) {
        // Regard en double precision : les tables de MathHelper sont trop grossieres pour un aller-retour exact.
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        double lookX = -Math.sin(yawRad) * cosPitch;
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * cosPitch;
        double w = dot(source.axisW, lookX, lookY, lookZ);
        double h = dot(source.axisH, lookX, lookY, lookZ);
        double n = dot(source.normal, lookX, lookY, lookZ);
        double x = rotated(destination.axisW.x, destination.axisH.x, destination.normal.x, w, h, n);
        double y = rotated(destination.axisW.y, destination.axisH.y, destination.normal.y, w, h, n);
        double z = rotated(destination.axisW.z, destination.axisH.z, destination.normal.z, w, h, n);
        float newPitch = (float) Math.toDegrees(-Math.asin(MathHelper.clamp(y, -1.0, 1.0)));
        float newYaw = (float) Math.toDegrees(Math.atan2(-x, z));
        return new Vec2f(newYaw, newPitch);
    }

    /** Composante d'un vecteur (x, y, z) le long d'un axe, dans l'ordre de {@code Vec3d.dotProduct}. */
    private static double dot(Vec3d axis, double x, double y, double z) {
        return x * axis.x + y * axis.y + z * axis.z;
    }

    /**
     * Une composante du vecteur (w, h, n) de la source dans la base de la destination : rotation de 180 degres autour de H,
     * W et N changent de signe, H est conserve. Memes operations, dans le meme ordre, que les {@code Vec3d} chaines : les
     * resultats sont identiques au bit pres, sans objet intermediaire.
     */
    private static double rotated(double axisW, double axisH, double normal, double w, double h, double n) {
        return axisW * -w + axisH * h + normal * -n;
    }
}
