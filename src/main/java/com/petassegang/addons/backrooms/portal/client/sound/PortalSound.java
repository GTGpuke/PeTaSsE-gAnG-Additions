package com.petassegang.addons.backrooms.portal.client.sound;

/**
 * Regles du son qui traverse un portail : un son d'un monde distant ressort au point du
 * cadre d'arrivee qui correspond au point du cadre le plus proche de sa source, et le trajet de la source jusqu'au
 * cadre lui coute du volume. Le moteur de son attenue ensuite, comme pour tout son, du point de sortie a l'oreille.
 */
final class PortalSound {

    /** Portee d'un son par unite de volume, en blocs : celle de vanilla. */
    private static final double BLOCKS_PER_VOLUME = 16.0;
    /** Portee minimale d'un son, en blocs. */
    static final double MIN_RADIUS = 16.0;
    /** Portee maximale d'un son, en blocs. */
    static final double MAX_RADIUS = 64.0;
    /** Un son colle au cadre, juste derriere son plan, passe encore. */
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
}
