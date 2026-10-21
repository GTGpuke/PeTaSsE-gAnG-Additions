package com.gtgpuke.gtgsbackrooms.core.portal.client.view;

/**
 * Pendant la creation d'un renderer de vue, ses tampons de sommets ne reservent leurs objets GL qu'au premier usage.
 *
 * <p>Un {@code WorldRenderer} cree une grille de sections a la distance de rendu du joueur, et chaque section quatre
 * tampons de sommets, soit douze objets GL par section : des dizaines de milliers d'appels au pilote dans la meme image,
 * un a-coup de plusieurs dizaines de millisecondes a l'apparition d'une dimension vue par un portail. Une vue ne construit
 * jamais qu'une petite part de sa grille (la fenetre synchronisee autour de l'arrivee) : les objets GL des autres sections
 * ne serviraient a rien. Le drapeau est pose par le thread de rendu, le temps de la creation ({@code MixinVertexBuffer}).
 */
public final class LazyVertexBuffers {

    private static boolean active;

    private LazyVertexBuffers() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** {@code true} pendant la creation d'un renderer de vue : un tampon neuf attend son premier usage pour exister. */
    public static boolean active() {
        return active;
    }

    /** Pose ou leve le drapeau ; a lever dans un {@code finally}. */
    public static void setActive(boolean value) {
        active = value;
    }
}
