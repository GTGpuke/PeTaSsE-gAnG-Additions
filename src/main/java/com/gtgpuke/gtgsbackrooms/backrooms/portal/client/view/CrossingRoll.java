package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import net.minecraft.client.render.Camera;

/**
 * Roulis de la camera du joueur juste apres une traversee. Minecraft ne penche pas la camera du joueur : d'un sol a un mur
 * (un portail qui tourne autour d'un axe horizontal), l'image vue par le cadre, penchee, se redresserait d'un coup a la
 * traversee. La camera part donc du roulis de cette image et se redresse en douceur ({@link #DURATION_MS}).
 */
public final class CrossingRoll {

    /** Duree du redressement, en millisecondes. */
    static final long DURATION_MS = 450L;
    /** En deca, le roulis ne se voit pas : rien a redresser. */
    private static final float NEGLIGIBLE = 1.0e-3f;

    private static float angle;
    private static long startedAt = -1L;

    private CrossingRoll() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Le joueur vient de traverser : sa camera part de ce roulis.
     *
     * @param roll roulis autour de l'axe du regard, en radians ({@link PortalViewTransform#crossingRoll})
     * @param now instant de la traversee, en millisecondes
     */
    public static void start(float roll, long now) {
        if (Math.abs(roll) < NEGLIGIBLE) {
            startedAt = -1L;
            return;
        }
        angle = roll;
        startedAt = now;
    }

    /**
     * Penche la camera du joueur de ce qui reste du roulis, juste apres son orientation par vanilla. Le reste decroit comme
     * le cube du temps restant : vite d'abord, doucement a la fin.
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
        camera.getRotation().rotateZ((float) (angle * left * left * left));
        PortalViewTransform.updatePlanes(camera);
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
