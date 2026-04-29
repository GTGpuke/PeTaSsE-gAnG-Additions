package com.petassegang.addons.backrooms.level.level0.generation.write.structure;

/**
 * Types de structures prefabriquees legeres candidates pour le Level 0.
 *
 * <p>Cette couche reste purement semantique pour l'instant : elle ne modifie
 * pas le carve du layout et ne pose encore aucun bloc en jeu.
 */
public enum LevelZeroStructureKind {
    NONE(0, 0),
    STORAGE_CLUSTER(3, 3),
    OFFICE_REMAINS(4, 3),
    PILLAR_RING(5, 5);

    private final int footprintWidth;
    private final int footprintHeight;

    LevelZeroStructureKind(int footprintWidth, int footprintHeight) {
        this.footprintWidth = footprintWidth;
        this.footprintHeight = footprintHeight;
    }

    /**
     * Retourne la largeur logique de l'empreinte de structure, en cellules.
     *
     * @return largeur de footprint
     */
    public int footprintWidth() {
        return footprintWidth;
    }

    /**
     * Retourne la hauteur logique de l'empreinte de structure, en cellules.
     *
     * @return hauteur de footprint
     */
    public int footprintHeight() {
        return footprintHeight;
    }
}
