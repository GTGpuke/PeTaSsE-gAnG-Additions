package com.petassegang.addons.world.backrooms.level0.stage;

/**
 * Etape de pipeline capable d'evaluer une region logique.
 *
 * <p>Les etapes regionales travaillent sur une fenetre plus large que le
 * simple chunk, afin de reconstruire une vue deterministe partagee par les
 * extractions locales ulterieures.</p>
 *
 * @param <T> type produit par l'etape
 */
public interface LevelZeroRegionStage<T> {

    /**
     * Evalue la region logique fournie.
     *
     * @param context contexte canonique de region
     * @return resultat de l'etape
     */
    T sample(LevelZeroRegionContext context);
}
