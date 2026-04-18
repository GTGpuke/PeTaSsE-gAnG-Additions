package com.petassegang.addons.world.backrooms.level0.layout;

/**
 * Features geometriques fines appliquees localement au-dessus de la grille 3x3.
 */
public enum LevelZeroGeometryFeature {

    /** Aucun ajustement geometrique local. */
    NONE(0),
    /** Mur localement decale pour casser legerement l'alignement. */
    OFFSET_WALL(1 << 0),
    /** Demi-mur ponctuel. */
    HALF_WALL(1 << 1),
    /** Renfoncement ou leger retrait d'un mur. */
    RECESS(1 << 2),
    /** Alcove locale. */
    ALCOVE(1 << 3),
    /** Etranglement tres ponctuel a un bloc de large. */
    PINCH_1WIDE(1 << 4);

    private final int bit;

    LevelZeroGeometryFeature(int bit) {
        this.bit = bit;
    }

    /**
     * Retourne le bit associe a cette feature.
     *
     * @return bit du masque geometrique
     */
    public int bit() {
        return bit;
    }
}
