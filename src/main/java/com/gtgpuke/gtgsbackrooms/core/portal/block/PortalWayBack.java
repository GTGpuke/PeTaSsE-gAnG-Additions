package com.gtgpuke.gtgsbackrooms.core.portal.block;

/**
 * Retour d'un portail, tel qu'on le choisit : toujours (aller-retour), jamais (aller simple), ou tant qu'un joueur voit la
 * sortie (sortie fugace). Deux reglages le portent, {@link PortalSettings#oneWay()} et
 * {@link PortalSettings#fleetingExit()} ; un aller simple n'a pas de sortie, fugace ou non : c'est le meme portail.
 */
public enum PortalWayBack {
    ROUND_TRIP("round_trip"),
    ONE_WAY("one_way"),
    FLEETING_EXIT("fleeting_exit");

    private final String key;

    PortalWayBack(String key) {
        this.key = key;
    }

    /** Retour de ces reglages. */
    public static PortalWayBack of(PortalSettings settings) {
        if (settings.oneWay()) {
            return ONE_WAY;
        }
        return settings.fleetingExit() ? FLEETING_EXIT : ROUND_TRIP;
    }

    /** {@code true} pour un aller simple. */
    public boolean oneWay() {
        return this == ONE_WAY;
    }

    /** {@code true} pour une sortie fugace. */
    public boolean fleetingExit() {
        return this == FLEETING_EXIT;
    }

    /** Dernier segment des cles de langue qui le nomment (info-bulle, ecran de la baguette). */
    public String key() {
        return this.key;
    }
}
