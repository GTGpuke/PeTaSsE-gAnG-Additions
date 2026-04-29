package com.petassegang.addons.backrooms.level.level0.generation.write.structure;

import com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroCoords;

/**
 * Profil semantique minimal d'une structure prefabriquee rare.
 */
public record LevelZeroStructureProfile(
        LevelZeroStructureKind kind,
        int anchorCellX,
        int anchorCellZ,
        int localCellX,
        int localCellZ) {

    /**
     * Profil vide sans structure candidate.
     *
     * @return profil vide
     */
    public static LevelZeroStructureProfile none() {
        return new LevelZeroStructureProfile(LevelZeroStructureKind.NONE, 0, 0, 0, 0);
    }

    /**
     * Retourne {@code true} si une structure prefabriquee est candidate.
     *
     * @return {@code true} si la colonne appartient a une structure rare
     */
    public boolean hasStructure() {
        return kind != LevelZeroStructureKind.NONE;
    }

    /**
     * Retourne {@code true} si la cellule courante correspond a l'origine
     * logique de l'empreinte de structure.
     *
     * @return {@code true} si la cellule est l'ancre locale
     */
    public boolean isAnchorCell() {
        return hasStructure() && localCellX == 0 && localCellZ == 0;
    }

    /**
     * Indique si la cellule courante se trouve sur le bord de l'empreinte de
     * structure.
     *
     * @return {@code true} si la cellule est sur le perimetre
     */
    public boolean isPerimeterCell() {
        if (!hasStructure()) {
            return false;
        }
        return localCellX == 0
                || localCellZ == 0
                || localCellX == kind.footprintWidth() - 1
                || localCellZ == kind.footprintHeight() - 1;
    }

    /**
     * Retourne le role local de la cellule dans le footprint de structure.
     *
     * @return role local derive
     */
    public LevelZeroStructureCellRole role() {
        if (!hasStructure()) {
            return LevelZeroStructureCellRole.NONE;
        }
        if (isAnchorCell()) {
            return LevelZeroStructureCellRole.ANCHOR;
        }
        if (isCenterCell()) {
            return LevelZeroStructureCellRole.CENTER;
        }
        if (isPerimeterCell()) {
            return LevelZeroStructureCellRole.EDGE;
        }
        return LevelZeroStructureCellRole.INTERIOR;
    }

    /**
     * Indique si la cellule courante correspond au centre logique du
     * footprint, quand celui-ci est definissable sur une seule cellule.
     *
     * @return {@code true} si la cellule est le centre discret du footprint
     */
    public boolean isCenterCell() {
        if (!hasStructure()) {
            return false;
        }
        if (kind.footprintWidth() % 2 == 0 || kind.footprintHeight() % 2 == 0) {
            return false;
        }
        return localCellX == kind.footprintWidth() / 2
                && localCellZ == kind.footprintHeight() / 2;
    }

    /**
     * Retourne l'origine monde X minimale du footprint.
     *
     * @return coordonnee monde X minimale
     */
    public int anchorWorldMinX() {
        return LevelZeroCoords.cellToWorldMinX(anchorCellX);
    }

    /**
     * Retourne l'origine monde Z minimale du footprint.
     *
     * @return coordonnee monde Z minimale
     */
    public int anchorWorldMinZ() {
        return LevelZeroCoords.cellToWorldMinZ(anchorCellZ);
    }

    /**
     * Retourne {@code true} si cette cellule porte un point de gameplay
     * potentiel.
     *
     * @return {@code true} si un point de gameplay est derive
     */
    public boolean hasGameplayPoint() {
        return gameplayPointKind() != LevelZeroStructureGameplayPointKind.NONE;
    }

    /**
     * Derive un point de gameplay potentiel a partir du type de structure et
     * de la position locale dans son footprint.
     *
     * @return type de point de gameplay potentiel
     */
    public LevelZeroStructureGameplayPointKind gameplayPointKind() {
        if (!hasStructure()) {
            return LevelZeroStructureGameplayPointKind.NONE;
        }

        return switch (kind) {
            case STORAGE_CLUSTER -> switch (role()) {
                case ANCHOR -> LevelZeroStructureGameplayPointKind.ENTRY;
                case CENTER, INTERIOR -> LevelZeroStructureGameplayPointKind.LOOT_HINT;
                default -> LevelZeroStructureGameplayPointKind.NONE;
            };
            case OFFICE_REMAINS -> switch (role()) {
                case ANCHOR -> LevelZeroStructureGameplayPointKind.ENTRY;
                case CENTER -> LevelZeroStructureGameplayPointKind.FOCAL_POINT;
                case INTERIOR -> LevelZeroStructureGameplayPointKind.UTILITY_HINT;
                default -> LevelZeroStructureGameplayPointKind.NONE;
            };
            case PILLAR_RING -> switch (role()) {
                case ANCHOR -> LevelZeroStructureGameplayPointKind.ENTRY;
                case CENTER -> LevelZeroStructureGameplayPointKind.FOCAL_POINT;
                default -> LevelZeroStructureGameplayPointKind.NONE;
            };
            default -> LevelZeroStructureGameplayPointKind.NONE;
        };
    }
}
