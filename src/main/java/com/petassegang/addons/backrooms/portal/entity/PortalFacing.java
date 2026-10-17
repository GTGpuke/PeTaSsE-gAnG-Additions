package com.petassegang.addons.backrooms.portal.entity;

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
 * Ce qui est devant la source est donc derriere la destination, et la droite devient la gauche :
 * le passage est une rotation de 180 degres autour de l'axe H, jamais un miroir.
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
        for (PortalFacing facing : values()) {
            if (facing.direction == direction) {
                return facing;
            }
        }
        throw new IllegalArgumentException("Direction non geree : " + direction + ".");
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
        double w = vector.dotProduct(source.axisW);
        double h = vector.dotProduct(source.axisH);
        double n = vector.dotProduct(source.normal);
        // Rotation de 180 degres autour de H : W et N changent de signe, H est conserve.
        return destination.axisW.multiply(-w)
                .add(destination.axisH.multiply(h))
                .add(destination.normal.multiply(-n));
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
        return destinationCenter.add(transformVector(source, destination, point.subtract(sourceCenter)));
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
        Vec3d transformed = transformVector(source, destination, lookVector(yaw, pitch));
        double clampedY = MathHelper.clamp(transformed.y, -1.0, 1.0);
        float newPitch = (float) Math.toDegrees(-Math.asin(clampedY));
        float newYaw = (float) Math.toDegrees(Math.atan2(-transformed.x, transformed.z));
        return new Vec2f(newYaw, newPitch);
    }

    /** Vecteur de regard en double precision (les tables de MathHelper sont trop grossieres pour un aller-retour exact). */
    private static Vec3d lookVector(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        return new Vec3d(-Math.sin(yawRad) * cosPitch, -Math.sin(pitchRad), Math.cos(yawRad) * cosPitch);
    }
}
