package com.petassegang.addons.backrooms.portal.transition;

import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.config.ModConfig;

/**
 * Decide si un portail compte pour un observateur : a portee d'activation, ou plus loin mais devant lui.
 *
 * <p>Ce critere unique pilote le prechargement des chunks destination, la synchronisation vers le client et
 * le rendu. A portee d'activation, la direction du regard ne compte pas : on peut s'engager dans un portail
 * a reculons ou de cote, l'autre cote est deja la. Plus loin, un portail dans le dos ne coute rien.
 */
public final class PortalRelevance {

    /** Distance en dessous de laquelle un portail compte quelle que soit la direction du regard : la distance d'activation. */
    public static final double ALWAYS_RELEVANT_DISTANCE = ModConfig.PORTAL_ACTIVATION_RADIUS_BLOCKS;
    /** Distance au-dela de laquelle aucun portail ne compte cote serveur : la plus grande de l'activation et du rendu. */
    public static final double MAX_DISTANCE = Math.max(ModConfig.PORTAL_ACTIVATION_RADIUS_BLOCKS, ModConfig.PORTAL_RENDER_DISTANCE_BLOCKS);

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
        // Appele a chaque image et pour chaque joueur : composantes a la main, sans vecteur intermediaire.
        double dx = portalPosition.x - eyePosition.x;
        double dy = portalPosition.y - eyePosition.y;
        double dz = portalPosition.z - eyePosition.z;
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        if (distanceSquared > maxDistance * maxDistance) {
            return false;
        }
        if (distanceSquared < ALWAYS_RELEVANT_DISTANCE * ALWAYS_RELEVANT_DISTANCE) {
            return true;
        }
        return lookVector.x * dx + lookVector.y * dy + lookVector.z * dz > 0.0;
    }
}
