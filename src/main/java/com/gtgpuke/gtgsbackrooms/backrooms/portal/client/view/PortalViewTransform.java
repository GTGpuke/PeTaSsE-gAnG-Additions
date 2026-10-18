package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;

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
    private static final Vector3f SCRATCH_NORMAL = new Vector3f();

    private PortalViewTransform() {
        throw new UnsupportedOperationException("Classe utilitaire.");
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
