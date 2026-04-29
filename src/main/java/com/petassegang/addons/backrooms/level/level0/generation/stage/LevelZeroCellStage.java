package com.petassegang.addons.backrooms.level.level0.generation.stage;

/**
 * Etape de pipeline capable d'evaluer une cellule logique.
 *
 * <p>Cette interface reste volontairement minimale : une etape cellule lit un
 * {@link LevelZeroCellContext} et produit une valeur purement deterministe,
 * sans effet de bord.</p>
 *
 * @param <T> type produit par l'etape
 */
public interface LevelZeroCellStage<T> {

    /**
     * Evalue la cellule logique fournie.
     *
     * @param context contexte canonique de cellule
     * @return resultat de l'etape
     */
    T sample(LevelZeroCellContext context);
}
