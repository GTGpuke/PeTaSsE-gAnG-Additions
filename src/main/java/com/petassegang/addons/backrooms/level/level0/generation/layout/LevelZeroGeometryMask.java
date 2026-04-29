package com.petassegang.addons.backrooms.level.level0.generation.layout;

/**
 * Helpers de masque pour les features de micro-geometrie.
 *
 * <p>Le masque sert a memoriser quelles anomalies fines sont actives sur une
 * cellule avant leur traduction finale en motif 3x3 ou en rendu debug.
 */
public final class LevelZeroGeometryMask {

    private LevelZeroGeometryMask() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Retourne un masque vide.
     *
     * @return masque sans feature
     */
    public static int none() {
        return 0;
    }

    /**
     * Ajoute une feature au masque.
     *
     * @param mask masque courant
     * @param feature feature a ajouter
     * @return masque enrichi
     */
    public static int with(int mask, LevelZeroGeometryFeature feature) {
        return mask | feature.bit();
    }

    /**
     * Indique si le masque contient une feature.
     *
     * @param mask masque a tester
     * @param feature feature recherchee
     * @return {@code true} si le bit est present
     */
    public static boolean has(int mask, LevelZeroGeometryFeature feature) {
        return (mask & feature.bit()) != 0;
    }
}
