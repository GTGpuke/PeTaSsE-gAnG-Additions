package com.petassegang.addons.backrooms;

import net.minecraft.util.Identifier;

import com.petassegang.addons.core.ModConstants;

/**
 * Constantes partagees pour les Backrooms.
 */
public final class BackroomsConstants {

    /** Identifiant de la dimension du Level 0. */
    public static final String LEVEL_ZERO_DIMENSION = "backrooms_level_0";

    /** Identifiant du type de dimension du Level 0. */
    public static final String LEVEL_ZERO_DIMENSION_TYPE = "backrooms_level_0_type";

    /** Identifiant du generateur de chunk du Level 0. */
    public static final String LEVEL_ZERO_CHUNK_GENERATOR = "backrooms_level_zero";

    /** Cle du random principal du layout. */
    public static final Identifier LEVEL_ZERO_LAYOUT_RANDOM =
            Identifier.of(ModConstants.MOD_ID, "backrooms_level_zero_layout");

    /** Echelle de conversion entre une cellule du script et les blocs Minecraft. */
    public static final int LEVEL_ZERO_CELL_SCALE = 3;

    /** Couche de bedrock. */
    public static final int LEVEL_ZERO_BEDROCK_Y = 60;

    /** Sous-sol technique. */
    public static final int LEVEL_ZERO_SUBFLOOR_Y = 61;

    /** Sol visible. */
    public static final int LEVEL_ZERO_FLOOR_Y = 62;

    /** Debut de l'air jouable. */
    public static final int LEVEL_ZERO_AIR_MIN_Y = 63;

    /** Fin de l'air jouable. */
    public static final int LEVEL_ZERO_AIR_MAX_Y = 66;

    /** Plafond du niveau. */
    public static final int LEVEL_ZERO_CEILING_Y = 67;

    /** Profondeur totale du generateur. */
    public static final int LEVEL_ZERO_GEN_DEPTH = 96;

    private BackroomsConstants() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }
}
