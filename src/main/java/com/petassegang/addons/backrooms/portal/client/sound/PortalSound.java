package com.petassegang.addons.backrooms.portal.client.sound;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;

/**
 * Regles du son qui traverse un portail : un son ressort au point du cadre, dans le monde de l'auditeur, le plus proche
 * de sa source vue par ce cadre (son image, derriere le plan), et le trajet de la source jusqu'au cadre lui coute du
 * volume. Le moteur de son attenue ensuite, comme pour tout son, du point de sortie a l'oreille.
 */
final class PortalSound {

    /** Portee d'un son par unite de volume, en blocs : celle de vanilla. */
    private static final double BLOCKS_PER_VOLUME = 16.0;
    /** Portee minimale d'un son, en blocs. */
    static final double MIN_RADIUS = 16.0;
    /** Portee maximale d'un son, en blocs. */
    static final double MAX_RADIUS = 64.0;
    /** Un son colle au cadre, juste de ce cote de son plan, passe encore ; un auditeur colle au cadre l'entend encore. */
    static final double BEHIND_TOLERANCE = 0.5;

    private PortalSound() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Portee d'un son de ce volume, bornee entre {@link #MIN_RADIUS} et {@link #MAX_RADIUS}. */
    static double radius(float volume) {
        return Math.min(MAX_RADIUS, Math.max(BLOCKS_PER_VOLUME * volume, MIN_RADIUS));
    }

    /**
     * Volume qui ressort du portail : le trajet jusqu'au cadre en a consomme un seizieme par bloc.
     *
     * @param volume volume du son a sa source
     * @param distanceToFrame distance de la source au point du cadre le plus proche
     * @return volume au point de sortie, nul si le son s'est eteint en chemin
     */
    static float volumeThroughPortal(float volume, double distanceToFrame) {
        return (float) Math.max(0.0, volume - distanceToFrame / BLOCKS_PER_VOLUME);
    }

    /**
     * Point du cadre par ou un son ressort d'un portail du monde de l'auditeur, celui par lequel il voit l'autre cote.
     * La source est prise par son image, la ou elle semble etre vue par le cadre, derriere son plan ; le son ressort au
     * point du cadre le plus proche de cette image. Il passe si la source est bien de l'autre cote, l'auditeur devant la
     * face, et l'un comme l'autre a portee du cadre.
     *
     * @param portal portail du monde de l'auditeur, vers le monde du son
     * @param image image de la source par ce portail ({@link BackroomsPortalEntity#fromDestination(Vec3d)})
     * @param listener position de l'auditeur
     * @param radius portee du son
     * @return point de sortie sur le cadre, ou {@code null} si ce portail ne laisse pas passer le son
     */
    static @Nullable Vec3d exitPoint(BackroomsPortalEntity portal, Vec3d image, Vec3d listener, double radius) {
        if (portal.signedDistance(image) > BEHIND_TOLERANCE || portal.signedDistance(listener) < -BEHIND_TOLERANCE) {
            return null;
        }
        Vec3d exit = portal.nearestFramePoint(image.x, image.y, image.z);
        double radiusSquared = radius * radius;
        return exit.squaredDistanceTo(image) > radiusSquared || exit.squaredDistanceTo(listener) > radiusSquared ? null : exit;
    }
}
