package com.petassegang.addons.world.backrooms.level0.write.structure;

import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroCoords;
import com.petassegang.addons.world.backrooms.level0.layout.sector.LevelZeroSectorRoomKind;
import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;

/**
 * Resolveur semantique des structures prefabriquees rares du Level 0.
 *
 * <p>Cette premiere version ne pose encore aucun bloc : elle identifie
 * seulement des candidats stables qui pourront etre exploites plus tard
 * sans changer la topologie.
 *
 * <p>TODO Level 0 : cette couche reste volontairement une fondation
 * semantique. Le vrai systeme de grosses structures faites a la main devra
 * reprendre plus tard ces points d'ancrage sans se limiter au `roomKind`
 * legacy.
 *
 * <p>Ce resolver repond donc a la question : "si une structure rare devait
 * exister ici plus tard, quel serait son footprint semantique ?".
 */
public final class LevelZeroStructureResolver {

    /**
     * Resolve un candidat de structure pour une colonne.
     *
     * @param roomKind type de salle legacy
     * @param walkable {@code true} si la colonne est traversable
     * @param worldX coordonnee monde X
     * @param worldZ coordonnee monde Z
     * @return profil de structure semantique
     */
    public LevelZeroStructureProfile resolve(LevelZeroSectorRoomKind roomKind,
                                             boolean walkable,
                                             int worldX,
                                             int worldZ) {
        if (!walkable) {
            return LevelZeroStructureProfile.none();
        }

        if (roomKind == LevelZeroSectorRoomKind.NONE) {
            return LevelZeroStructureProfile.none();
        }
        int cellX = LevelZeroCoords.worldToCellX(worldX);
        int cellZ = LevelZeroCoords.worldToCellZ(worldZ);
        return switch (roomKind) {
            case RECT_ROOM -> resolveFootprint(
                    LevelZeroStructureKind.STORAGE_CLUSTER,
                    cellX,
                    cellZ,
                    1,
                    2,
                    8);
            case CUSTOM_ROOM -> resolveFootprint(
                    LevelZeroStructureKind.OFFICE_REMAINS,
                    cellX,
                    cellZ,
                    3,
                    1,
                    6);
            case PILLAR_ROOM -> resolveFootprint(
                    LevelZeroStructureKind.PILLAR_RING,
                    cellX,
                    cellZ,
                    4,
                    4,
                    10);
            default -> LevelZeroStructureProfile.none();
        };
    }

    private LevelZeroStructureProfile resolveFootprint(LevelZeroStructureKind kind,
                                                       int cellX,
                                                       int cellZ,
                                                       int phaseX,
                                                       int phaseZ,
                                                       int maxRollExclusive) {
        int anchorCellX = cellX - Math.floorMod(cellX - phaseX, kind.footprintWidth());
        int anchorCellZ = cellZ - Math.floorMod(cellZ - phaseZ, kind.footprintHeight());
        if (!contains(kind, anchorCellX, anchorCellZ, cellX, cellZ)) {
            return LevelZeroStructureProfile.none();
        }

        long hash = StageRandom.mixLegacy(0L, StageRandom.Stage.STRUCTURES, anchorCellX, anchorCellZ);
        int roll = Math.floorMod(hash, 64);
        if (roll >= maxRollExclusive) {
            return LevelZeroStructureProfile.none();
        }

        return new LevelZeroStructureProfile(
                kind,
                anchorCellX,
                anchorCellZ,
                cellX - anchorCellX,
                cellZ - anchorCellZ);
    }

    private static boolean contains(LevelZeroStructureKind kind,
                                    int anchorCellX,
                                    int anchorCellZ,
                                    int cellX,
                                    int cellZ) {
        return cellX >= anchorCellX
                && cellX < anchorCellX + kind.footprintWidth()
                && cellZ >= anchorCellZ
                && cellZ < anchorCellZ + kind.footprintHeight();
    }
}
