package com.petassegang.addons.backrooms.portal.client.view;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;

/**
 * Camera et matrices de la vue a travers un portail.
 *
 * <p>La camera distante est l'image de la camera du joueur par le passage du portail ; sa matrice de
 * vue se construit comme celle de {@code GameRenderer}. La projection recoit un plan proche oblique
 * confondu avec le plan du portail d'arrivee, pour ne jamais dessiner ce qui se trouve derriere lui.
 */
public final class PortalViewTransform {

    /** En deca de cette distance au plan, le clipping oblique degenere : on garde la projection normale. */
    private static final float MIN_CLIP_DISTANCE = 0.01f;

    private static final Quaternionf SCRATCH_ROTATION = new Quaternionf();
    private static final Matrix4f SCRATCH_INVERSE = new Matrix4f();
    private static final Vector4f SCRATCH_PLANE = new Vector4f();
    private static final Vector4f SCRATCH_CORNER = new Vector4f();

    private PortalViewTransform() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Position de la camera distante : image de la camera du joueur a travers le portail. */
    public static Vec3d remoteCameraPos(BackroomsPortalEntity portal, Vec3d cameraPos) {
        return PortalFacing.transformPoint(
                portal.facingSource(), portal.getPos(), portal.facingDestination(), portal.destinationPos(), cameraPos);
    }

    /** Regard de la camera distante ({@code x} = yaw, {@code y} = pitch). */
    public static Vec2f remoteLook(BackroomsPortalEntity portal, float yaw, float pitch) {
        return PortalFacing.transformLook(portal.facingSource(), portal.facingDestination(), yaw, pitch);
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
     * Projection dont le plan proche est le plan donne (methode du plan proche oblique).
     *
     * <p>Le plan est decrit en coordonnees monde par sa normale, orientee vers le demi-espace visible,
     * et un point. Si la camera est deja du cote visible, la projection est copiee telle quelle.
     *
     * @param projection projection du joueur
     * @param positionMatrix matrice de vue (rotation seule) de la camera distante
     * @param planeNormal normale du plan, vers le cote a garder
     * @param planePoint un point du plan
     * @param cameraPos position de la camera distante
     * @param out matrice de sortie, ecrasee
     * @return {@code out}
     */
    public static Matrix4f obliqueProjection(
            Matrix4f projection,
            Matrix4f positionMatrix,
            Vec3d planeNormal,
            Vec3d planePoint,
            Vec3d cameraPos,
            Matrix4f out) {
        out.set(projection);
        // Distance signee de la camera au plan : negative quand elle est du cote a couper.
        float distance = (float) -((planePoint.x - cameraPos.x) * planeNormal.x
                + (planePoint.y - cameraPos.y) * planeNormal.y
                + (planePoint.z - cameraPos.z) * planeNormal.z);
        if (distance > -MIN_CLIP_DISTANCE) {
            return out;
        }
        SCRATCH_PLANE.set((float) planeNormal.x, (float) planeNormal.y, (float) planeNormal.z, 0.0f);
        positionMatrix.transform(SCRATCH_PLANE);
        SCRATCH_PLANE.w = distance;

        SCRATCH_INVERSE.set(projection).invert();
        SCRATCH_CORNER.set(Math.signum(SCRATCH_PLANE.x), Math.signum(SCRATCH_PLANE.y), 1.0f, 1.0f);
        SCRATCH_INVERSE.transform(SCRATCH_CORNER);
        float scale = 2.0f / SCRATCH_PLANE.dot(SCRATCH_CORNER);
        // La troisieme ligne (profondeur) devient le plan, exprime par rapport a la quatrieme ligne.
        out.m02(SCRATCH_PLANE.x * scale - projection.m03());
        out.m12(SCRATCH_PLANE.y * scale - projection.m13());
        out.m22(SCRATCH_PLANE.z * scale - projection.m23());
        out.m32(SCRATCH_PLANE.w * scale - projection.m33());
        return out;
    }
}
