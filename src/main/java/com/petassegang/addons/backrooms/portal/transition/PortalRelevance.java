package com.petassegang.addons.backrooms.portal.transition;

import net.minecraft.util.math.Vec3d;

/**
 * Decide si un portail compte pour un observateur : assez proche et devant lui.
 *
 * <p>Ce critere unique pilote le prechargement des chunks destination, la
 * synchronisation vers le client et le rendu : un portail dans le dos du joueur
 * ou trop loin ne coute rien.
 */
public final class PortalRelevance {

    /** Distance en dessous de laquelle un portail compte quelle que soit la direction du regard. */
    public static final double ALWAYS_RELEVANT_DISTANCE = 4.0;

    private PortalRelevance() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * {@code true} si le portail est a portee et devant l'observateur.
     *
     * @param eyePosition position des yeux de l'observateur
     * @param lookVector direction du regard, non nulle
     * @param portalPosition centre du portail
     * @param maxDistance distance maximale en blocs
     * @return {@code true} si le portail doit etre pris en compte
     */
    public static boolean isRelevant(Vec3d eyePosition, Vec3d lookVector, Vec3d portalPosition, double maxDistance) {
        Vec3d toPortal = portalPosition.subtract(eyePosition);
        double distanceSquared = toPortal.lengthSquared();
        if (distanceSquared > maxDistance * maxDistance) {
            return false;
        }
        if (distanceSquared < ALWAYS_RELEVANT_DISTANCE * ALWAYS_RELEVANT_DISTANCE) {
            return true;
        }
        return lookVector.dotProduct(toPortal) > 0.0;
    }
}
