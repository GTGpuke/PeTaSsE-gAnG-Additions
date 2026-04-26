package com.petassegang.addons.world.backrooms.level0.layout;

/**
 * Features geometriques fines appliquees localement au-dessus de la grille 3x3.
 *
 * <p>La liste est volontairement remise a zero : aucune variante procedurale
 * n'est active tant que sa forme n'a pas ete ajoutee et validee une par une.
 * Le masque reste en place pour conserver une pipeline stable et faciliter la
 * prochaine variante.
 */
public enum LevelZeroGeometryFeature {

    /** Aucun ajustement geometrique local. */
    NONE(0);

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
