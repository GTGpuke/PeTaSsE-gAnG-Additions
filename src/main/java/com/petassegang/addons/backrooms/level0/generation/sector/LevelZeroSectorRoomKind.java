package com.petassegang.addons.backrooms.level0.generation.sector;

/**
 * Type de salle tamponnee sur une cellule par le generateur de secteur.
 */
public enum LevelZeroSectorRoomKind {
    NONE,
    RECT_ROOM,
    PILLAR_ROOM,
    CUSTOM_ROOM;

    private static final LevelZeroSectorRoomKind[] VALUES = values();

    /**
     * Retourne le type de salle correspondant a un ordinal stocke de maniere compacte.
     *
     * @param ordinal ordinal du type
     * @return type de salle
     */
    public static LevelZeroSectorRoomKind fromOrdinal(int ordinal) {
        return VALUES[ordinal];
    }
}
