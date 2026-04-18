package com.petassegang.addons.world.backrooms.level0.stage;

/**
 * Etape de pipeline capable d'evaluer une cellule logique.
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
