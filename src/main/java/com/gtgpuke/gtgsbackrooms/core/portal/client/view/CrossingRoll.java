package com.gtgpuke.gtgsbackrooms.core.portal.client.view;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

import com.gtgpuke.gtgsbackrooms.mixin.client.CameraAccessor;

/**
 * Roulis et decalage de la camera du joueur juste apres une traversee. Minecraft ne penche pas la camera du joueur : d'un
 * sol a un mur (un portail qui tourne autour d'un axe horizontal), ou par un cadre d'arrivee retourne (tete en bas),
 * l'image vue par le cadre, penchee ou renversee, se redresserait d'un coup a la traversee. Et le corps ressort debout, la
 * ou il tient : l'oeil n'est pas toujours la ou le cadre montrait la vue (retourne, l'oeil passe du bas de l'ouverture au
 * haut). La camera part donc du roulis et de la place de cette image, puis se redresse et rejoint l'oeil en douceur
 * ({@link #DURATION_MS}).
 */
public final class CrossingRoll {

    /** Duree du redressement, en millisecondes. */
    static final long DURATION_MS = 450L;
    /** En deca, le roulis ne se voit pas : rien a redresser. */
    private static final float NEGLIGIBLE = 1.0e-3f;
    /** En deca (au carre, en blocs), le decalage ne se voit pas : rien a rejoindre. */
    private static final double NEGLIGIBLE_SHIFT_SQUARED = 0.05 * 0.05;

    private static float angle;
    /** Ecart de depart de la camera a l'oeil ; {@link Vec3d#ZERO} lui-meme sans ecart a rejoindre. */
    private static Vec3d shift = Vec3d.ZERO;
    private static long startedAt = -1L;

    private CrossingRoll() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Le joueur vient de traverser : sa camera part de ce roulis, sans decalage.
     *
     * @param roll roulis autour de l'axe du regard, en radians ({@link PortalViewTransform#crossingRoll})
     * @param now instant de la traversee, en millisecondes
     */
    public static void start(float roll, long now) {
        start(roll, Vec3d.ZERO, now);
    }

    /**
     * Le joueur vient de traverser : sa camera part de ce roulis et de ce decalage, celui de l'image de son oeil vue par le
     * cadre sur son oeil a l'arrivee.
     *
     * @param roll roulis autour de l'axe du regard, en radians ({@link PortalViewTransform#crossingRoll})
     * @param offset ecart de la camera a l'oeil, en blocs, dans le monde d'arrivee
     * @param now instant de la traversee, en millisecondes
     */
    public static void start(float roll, Vec3d offset, long now) {
        boolean rolls = Math.abs(roll) >= NEGLIGIBLE;
        boolean shifts = offset.lengthSquared() >= NEGLIGIBLE_SHIFT_SQUARED;
        if (!rolls && !shifts) {
            startedAt = -1L;
            return;
        }
        angle = rolls ? roll : 0.0f;
        shift = shifts ? offset : Vec3d.ZERO;
        startedAt = now;
    }

    /**
     * Penche et deplace la camera du joueur de ce qui reste du roulis et du decalage, juste apres son orientation et sa
     * place par vanilla. Le reste decroit comme le cube du temps restant : vite d'abord, doucement a la fin.
     *
     * @param camera camera du joueur
     * @param now instant de l'image, en millisecondes
     */
    public static void apply(Camera camera, long now) {
        if (startedAt < 0L) {
            return;
        }
        long elapsed = now - startedAt;
        if (elapsed >= DURATION_MS) {
            startedAt = -1L;
            return;
        }
        double left = 1.0 - Math.max(0L, elapsed) / (double) DURATION_MS;
        double remaining = left * left * left;
        if (angle != 0.0f) {
            camera.getRotation().rotateZ((float) (angle * remaining));
            PortalViewTransform.updatePlanes(camera);
        }
        if (shift != Vec3d.ZERO) {
            // Une position neuve par image, le temps du redressement seulement : vanilla en pose une a chaque image.
            ((CameraAccessor) camera).backrooms$setPos(camera.getPos().add(shift.x * remaining, shift.y * remaining, shift.z * remaining));
        }
    }

    /** {@code true} tant que la camera se redresse. */
    public static boolean active() {
        return startedAt >= 0L;
    }

    /** Deconnexion : plus rien a redresser. */
    public static void clear() {
        startedAt = -1L;
    }
}
