package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.mixin.client.CameraAccessor;

/**
 * Camera et plan de decoupe de la vue a travers un portail.
 *
 * <p>La camera distante est l'image de la camera du joueur par le passage du portail ; sa matrice de vue se
 * construit comme celle de {@code GameRenderer}. La projection reste celle du joueur : ce qui se trouve derriere le
 * plan du portail d'arrivee est coupe par les shaders ({@link ShaderClipInjector}), auxquels on donne ce plan dans
 * l'espace camera.
 */
public final class PortalViewTransform {

    private static final Quaternionf SCRATCH_ROTATION = new Quaternionf();
    private static final Quaternionf SCRATCH_PORTAL = new Quaternionf();
    private static final Matrix3f SCRATCH_PORTAL_MATRIX = new Matrix3f();
    private static final Vector3f SCRATCH_NORMAL = new Vector3f();

    private PortalViewTransform() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Regard de la camera distante ({@code x} = yaw, {@code y} = pitch). */
    public static Vec2f remoteLook(BackroomsPortalEntity portal, float yaw, float pitch) {
        return PortalFacing.transformLook(portal.facingSource(), portal.facingDestination(), yaw, pitch);
    }

    /**
     * Oriente la camera d'une vue : le regard transpose (lacet et tangage, que lisent ceux qui n'ont besoin que du regard),
     * puis sa rotation complete, celle de la camera source suivie du passage du portail. D'un sol a un mur, le passage ne
     * tourne pas autour de la verticale : l'image se penche, et un lacet et un tangage seuls la montreraient de travers.
     *
     * @param target camera de la vue
     * @param portal portail par lequel la vue est vue
     * @param source camera qui regarde par ce portail : celle du joueur, ou celle de la vue qui le contient
     * @return regard transpose de la camera de la vue ({@code x} = yaw, {@code y} = pitch)
     */
    public static Vec2f aim(Camera target, BackroomsPortalEntity portal, Camera source) {
        Vec2f look = remoteLook(portal, source.getYaw(), source.getPitch());
        ((CameraAccessor) target).backrooms$setRotation(look.x, look.y);
        portalRotation(portal.facingSource(), portal.facingDestination(), SCRATCH_PORTAL).mul(source.getRotation());
        target.getRotation().set(SCRATCH_PORTAL);
        updatePlanes(target);
        return look;
    }

    /** Les trois directions que vanilla tire de la rotation d'une camera (devant, haut, gauche), refaites d'apres elle. */
    public static void updatePlanes(Camera camera) {
        Quaternionf rotation = camera.getRotation();
        camera.getHorizontalPlane().set(0.0f, 0.0f, -1.0f).rotate(rotation);
        camera.getVerticalPlane().set(0.0f, 1.0f, 0.0f).rotate(rotation);
        camera.getDiagonalPlane().set(-1.0f, 0.0f, 0.0f).rotate(rotation);
    }

    /**
     * Roulis qui separe, a la traversee, l'image vue par le cadre (la camera transposee par le portail, roulis compris) de
     * celle que le joueur peut avoir (lacet et tangage transposes, sans roulis), en radians autour de l'axe du regard.
     * Nul entre deux cadres debout, et quand le regard devient vertical (son cap garde deja le haut de l'image).
     *
     * @param source orientation du portail traverse
     * @param destination orientation de l'arrivee
     * @param yaw lacet du joueur avant la traversee, en degres
     * @param pitch tangage du joueur avant la traversee, en degres
     * @return roulis de depart de la camera du joueur ({@link CrossingRoll})
     */
    public static float crossingRoll(PortalFacing source, PortalFacing destination, float yaw, float pitch) {
        Quaternionf ideal = portalRotation(source, destination, new Quaternionf()).mul(cameraRotation(yaw, pitch, new Quaternionf()));
        Vec2f look = PortalFacing.transformLook(source, destination, yaw, pitch);
        // Rotation, dans le repere de la camera du joueur apres la traversee, qui mene a l'image vue : autour de son regard.
        Quaternionf local = cameraRotation(look.x, look.y, new Quaternionf()).conjugate().mul(ideal);
        // Un quaternion et son oppose sont la meme rotation : lu avec w negatif, un roulis nul vaudrait un tour entier, que
        // la camera deroulerait a chaque traversee. Le signe qui rend w positif garde le roulis entre -pi et pi.
        double sign = Math.copySign(1.0, local.w());
        return (float) (2.0 * Math.atan2(sign * local.z(), sign * local.w()));
    }

    /** Rotation d'une camera vanilla regardant selon ce lacet et ce tangage, comme {@code Camera.setRotation}. */
    private static Quaternionf cameraRotation(float yaw, float pitch, Quaternionf out) {
        return out.rotationYXZ((float) Math.PI - (float) Math.toRadians(yaw), -(float) Math.toRadians(pitch), 0.0f);
    }

    /** Donne a une camera la rotation complete d'une autre, roulis compris, et les directions qui en derivent. */
    public static void copyRotation(Camera from, Camera to) {
        to.getRotation().set(from.getRotation());
        to.getHorizontalPlane().set(from.getHorizontalPlane());
        to.getVerticalPlane().set(from.getVerticalPlane());
        to.getDiagonalPlane().set(from.getDiagonalPlane());
    }

    /**
     * Rotation du passage d'un portail : celle qui envoie chaque direction de la source sur son image a l'arrivee
     * ({@link PortalFacing#transformVector}), sans allocation.
     */
    static Quaternionf portalRotation(PortalFacing source, PortalFacing destination, Quaternionf out) {
        SCRATCH_PORTAL_MATRIX.set(
                entry(source, destination, 0, 0), entry(source, destination, 1, 0), entry(source, destination, 2, 0),
                entry(source, destination, 0, 1), entry(source, destination, 1, 1), entry(source, destination, 2, 1),
                entry(source, destination, 0, 2), entry(source, destination, 1, 2), entry(source, destination, 2, 2));
        return out.setFromNormalized(SCRATCH_PORTAL_MATRIX);
    }

    /** Composante {@code row} de l'image de l'axe {@code column} du monde : W et N changent de signe, H est garde. */
    private static float entry(PortalFacing source, PortalFacing destination, int row, int column) {
        return (float) (-component(destination.axisW, row) * component(source.axisW, column)
                + component(destination.axisH, row) * component(source.axisH, column)
                - component(destination.normal, row) * component(source.normal, column));
    }

    private static double component(Vec3d vector, int index) {
        if (index == 0) {
            return vector.x;
        }
        return index == 1 ? vector.y : vector.z;
    }

    /**
     * Matrice de vue (rotation seule, camera a l'origine) d'une camera, comme {@code GameRenderer} la construit.
     *
     * @param camera camera dont on lit la rotation
     * @param out matrice de sortie, ecrasee
     * @return {@code out}
     */
    public static Matrix4f positionMatrix(Camera camera, Matrix4f out) {
        return out.rotation(camera.getRotation().conjugate(SCRATCH_ROTATION));
    }

    /**
     * Plan de decoupe en espace camera, celui de {@code ModelViewMat * Position} dans les shaders : {@code (x, y, z, w)}
     * tel que {@code dot(plan, p) >= 0} pour tout point {@code p} a garder.
     *
     * @param planeNormal normale du plan en coordonnees monde, vers le cote a garder
     * @param planePoint un point du plan
     * @param cameraPos position de la camera de la vue
     * @param positionMatrix matrice de vue (rotation seule) de cette camera
     * @param out plan de sortie, ecrase
     * @return {@code out}
     */
    public static Vector4f clipPlane(Vec3d planeNormal, Vec3d planePoint, Vec3d cameraPos, Matrix4f positionMatrix, Vector4f out) {
        positionMatrix.transformDirection((float) planeNormal.x, (float) planeNormal.y, (float) planeNormal.z, SCRATCH_NORMAL);
        // La camera etant a l'origine de cet espace, le terme constant est sa distance signee au plan, cote coupe.
        float distance = (float) -((planePoint.x - cameraPos.x) * planeNormal.x
                + (planePoint.y - cameraPos.y) * planeNormal.y
                + (planePoint.z - cameraPos.z) * planeNormal.z);
        return out.set(SCRATCH_NORMAL.x, SCRATCH_NORMAL.y, SCRATCH_NORMAL.z, distance);
    }
}
