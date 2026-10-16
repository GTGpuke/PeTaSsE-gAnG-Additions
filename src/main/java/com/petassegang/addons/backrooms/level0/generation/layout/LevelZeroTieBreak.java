package com.petassegang.addons.backrooms.level0.generation.layout;

/**
 * Departage deterministe entre une cellule et sa voisine quand toutes deux pretendent a la meme feature.
 *
 * <p>Le meilleur score gagne ; a score egal, la cellule la plus a l'est, puis la plus au sud. Partage par les
 * grandes pieces et les gaps de couloir, pour que les deux tranchent de la meme facon.
 */
final class LevelZeroTieBreak {

    private LevelZeroTieBreak() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * {@code true} si la voisine l'emporte sur la cellule evaluee.
     *
     * @param selfScore score de la cellule evaluee
     * @param neighborScore score de la voisine
     * @param selfX coordonnee X de la cellule evaluee, en cellules
     * @param selfZ coordonnee Z de la cellule evaluee, en cellules
     * @param neighborX coordonnee X de la voisine, en cellules
     * @param neighborZ coordonnee Z de la voisine, en cellules
     */
    static boolean neighborWins(long selfScore, long neighborScore, int selfX, int selfZ, int neighborX, int neighborZ) {
        if (neighborScore != selfScore) {
            return neighborScore > selfScore;
        }
        if (neighborX != selfX) {
            return neighborX > selfX;
        }
        return neighborZ > selfZ;
    }
}
