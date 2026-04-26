package com.petassegang.addons.world.backrooms.level0.coord;

import com.petassegang.addons.world.backrooms.BackroomsConstants;

/**
 * Facade nommee pour la geographie verticale actuelle du Level 0 monocouche.
 */
public final class LevelZeroVerticalLayout {

    /** Nombre de blocs simules dans un echantillon de colonne. */
    public static final int COLUMN_SAMPLE_HEIGHT = 128;

    private LevelZeroVerticalLayout() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Retourne la couche de bedrock basse.
     *
     * @return coordonnee Y de la bedrock
     */
    public static int bedrockY() {
        return BackroomsConstants.LEVEL_ZERO_BEDROCK_Y;
    }

    /**
     * Retourne la couche technique sous le sol.
     *
     * @return coordonnee Y du sous-sol
     */
    public static int subfloorY() {
        return BackroomsConstants.LEVEL_ZERO_SUBFLOOR_Y;
    }

    /**
     * Retourne la coordonnee Y du sol visible.
     *
     * @return coordonnee Y du sol
     */
    public static int floorY() {
        return BackroomsConstants.LEVEL_ZERO_FLOOR_Y;
    }

    /**
     * Retourne la coordonnee Y minimale de l'air jouable.
     *
     * @return debut de la zone d'air
     */
    public static int airMinY() {
        return BackroomsConstants.LEVEL_ZERO_AIR_MIN_Y;
    }

    /**
     * Retourne la coordonnee Y maximale de l'air jouable.
     *
     * @return fin de la zone d'air
     */
    public static int airMaxY() {
        return BackroomsConstants.LEVEL_ZERO_AIR_MAX_Y;
    }

    /**
     * Retourne la coordonnee Y du plafond.
     *
     * @return coordonnee Y du plafond
     */
    public static int ceilingY() {
        return BackroomsConstants.LEVEL_ZERO_CEILING_Y;
    }

    /**
     * Retourne la hauteur de monde actuelle de la dimension.
     *
     * @return hauteur logique de la dimension
     */
    public static int worldHeight() {
        return 128;
    }

    /**
     * Retourne le Y minimal actuel de la dimension.
     *
     * @return Y minimal
     */
    public static int minimumY() {
        return 0;
    }

    /**
     * Retourne le niveau de mer technique, qui correspond ici au sol du level.
     *
     * @return niveau de mer technique
     */
    public static int seaLevel() {
        return floorY();
    }

    /**
     * Retourne la hauteur utilisee pour le heightmap.
     *
     * @return hauteur solide max + 1
     */
    public static int heightmapTopY() {
        return ceilingY() + 1;
    }
}
