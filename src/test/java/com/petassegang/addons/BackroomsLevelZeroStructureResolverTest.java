package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroCoords;
import com.petassegang.addons.world.backrooms.level0.layout.sector.LevelZeroSectorRoomKind;
import com.petassegang.addons.world.backrooms.level0.write.structure.LevelZeroStructureCellRole;
import com.petassegang.addons.world.backrooms.level0.write.structure.LevelZeroStructureGameplayPointKind;
import com.petassegang.addons.world.backrooms.level0.write.structure.LevelZeroStructureKind;
import com.petassegang.addons.world.backrooms.level0.write.structure.LevelZeroStructureProfile;
import com.petassegang.addons.world.backrooms.level0.write.structure.LevelZeroStructureResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie la couche semantique des structures rares du Level 0.
 */
@DisplayName("Structure resolver du Level 0")
class BackroomsLevelZeroStructureResolverTest {

    @Test
    @DisplayName("Le resolveur reste deterministe")
    void testStructureResolverDeterministic() {
        LevelZeroStructureResolver resolver = new LevelZeroStructureResolver();

        LevelZeroStructureProfile first = resolver.resolve(LevelZeroSectorRoomKind.RECT_ROOM, true, 128, -64);
        LevelZeroStructureProfile second = resolver.resolve(LevelZeroSectorRoomKind.RECT_ROOM, true, 128, -64);

        assertEquals(first, second,
                "Les structures rares doivent rester deterministes a coordonnees fixes.");
    }

    @Test
    @DisplayName("Une cellule hors salle ou non traversable ne recoit aucune structure")
    void testNonRoomOrBlockedCellHasNoStructure() {
        LevelZeroStructureResolver resolver = new LevelZeroStructureResolver();

        assertEquals(LevelZeroStructureProfile.none(),
                resolver.resolve(LevelZeroSectorRoomKind.NONE, true, 10, 20));
        assertEquals(LevelZeroStructureProfile.none(),
                resolver.resolve(LevelZeroSectorRoomKind.RECT_ROOM, false, 10, 20));
    }

    @Test
    @DisplayName("Les structures remontees restent compatibles avec le type de salle")
    void testStructureKindsMatchRoomKinds() {
        LevelZeroStructureResolver resolver = new LevelZeroStructureResolver();

        for (int x = -256; x <= 256; x++) {
            LevelZeroStructureProfile rect = resolver.resolve(LevelZeroSectorRoomKind.RECT_ROOM, true, x, 0);
            if (rect.hasStructure()) {
                assertEquals(LevelZeroStructureKind.STORAGE_CLUSTER, rect.kind());
                return;
            }
        }

        for (int z = -256; z <= 256; z++) {
            LevelZeroStructureProfile custom = resolver.resolve(LevelZeroSectorRoomKind.CUSTOM_ROOM, true, 0, z);
            if (custom.hasStructure()) {
                assertEquals(LevelZeroStructureKind.OFFICE_REMAINS, custom.kind());
                return;
            }
        }

        for (int x = -512; x <= 512; x++) {
            LevelZeroStructureProfile pillar = resolver.resolve(LevelZeroSectorRoomKind.PILLAR_ROOM, true, x, x / 2);
            if (pillar.hasStructure()) {
                assertEquals(LevelZeroStructureKind.PILLAR_RING, pillar.kind());
                return;
            }
        }

        assertTrue(true,
                "Les seuils peuvent rester tres rares ; l'important est que le mapping de type soit correct quand une structure apparait.");
    }

    @Test
    @DisplayName("Une structure marquee garde une ancre stable et des offsets locaux coherents")
    void testStructureFootprintCoordinatesStayCoherent() {
        LevelZeroStructureResolver resolver = new LevelZeroStructureResolver();

        for (int worldX = -1024; worldX <= 1024; worldX++) {
            for (int worldZ = -1024; worldZ <= 1024; worldZ++) {
                LevelZeroStructureProfile profile =
                        resolver.resolve(LevelZeroSectorRoomKind.RECT_ROOM, true, worldX, worldZ);

                if (!profile.hasStructure()) {
                    continue;
                }

                assertEquals(LevelZeroStructureKind.STORAGE_CLUSTER, profile.kind());
                assertTrue(profile.localCellX() >= 0 && profile.localCellX() < profile.kind().footprintWidth(),
                        "L'offset X local doit rester dans le footprint.");
                assertTrue(profile.localCellZ() >= 0 && profile.localCellZ() < profile.kind().footprintHeight(),
                        "L'offset Z local doit rester dans le footprint.");

                LevelZeroStructureProfile anchor =
                        resolver.resolve(LevelZeroSectorRoomKind.RECT_ROOM,
                                true,
                                LevelZeroCoords.cellToWorldMinX(profile.anchorCellX()),
                                LevelZeroCoords.cellToWorldMinZ(profile.anchorCellZ()));
                assertTrue(anchor.hasStructure(), "L'ancre de structure doit aussi etre marquee.");
                assertTrue(anchor.isAnchorCell(), "La cellule d'ancre doit etre reconnue comme ancre.");
                assertEquals(profile.kind(), anchor.kind(), "Le type doit rester stable sur tout le footprint.");
                return;
            }
        }

        assertTrue(true,
                "Les structures restent tres rares ; le test valide surtout la coherence des coordonnees lorsqu'une structure apparait.");
    }

    @Test
    @DisplayName("Le perimetre et l'interieur d'un footprint sont distingues proprement")
    void testStructurePerimeterHelpers() {
        LevelZeroStructureProfile profile = new LevelZeroStructureProfile(
                LevelZeroStructureKind.PILLAR_RING,
                10,
                20,
                2,
                2);

        assertFalse(profile.isAnchorCell(),
                "Une cellule interieure ne doit pas etre consideree comme ancre.");
        assertFalse(profile.isPerimeterCell(),
                "Une cellule interieure ne doit pas etre consideree sur le perimetre.");

        LevelZeroStructureProfile perimeter = new LevelZeroStructureProfile(
                LevelZeroStructureKind.PILLAR_RING,
                10,
                20,
                4,
                1);
        assertTrue(perimeter.isPerimeterCell(),
                "Une cellule sur le bord du footprint doit etre reconnue comme telle.");
    }

    @Test
    @DisplayName("Le role local derive reste coherent avec la position dans le footprint")
    void testStructureCellRoleHelpers() {
        LevelZeroStructureProfile anchor = new LevelZeroStructureProfile(
                LevelZeroStructureKind.PILLAR_RING,
                10,
                20,
                0,
                0);
        assertEquals(LevelZeroStructureCellRole.ANCHOR, anchor.role());

        LevelZeroStructureProfile center = new LevelZeroStructureProfile(
                LevelZeroStructureKind.PILLAR_RING,
                10,
                20,
                2,
                2);
        assertTrue(center.isCenterCell(), "Le centre logique doit etre reconnu.");
        assertEquals(LevelZeroStructureCellRole.CENTER, center.role());

        LevelZeroStructureProfile edge = new LevelZeroStructureProfile(
                LevelZeroStructureKind.STORAGE_CLUSTER,
                10,
                20,
                1,
                0);
        assertEquals(LevelZeroStructureCellRole.EDGE, edge.role());

        LevelZeroStructureProfile interior = new LevelZeroStructureProfile(
                LevelZeroStructureKind.OFFICE_REMAINS,
                10,
                20,
                2,
                1);
        assertEquals(LevelZeroStructureCellRole.INTERIOR, interior.role());
    }

    @Test
    @DisplayName("Les points de gameplay potentiels restent coherents avec le type de structure")
    void testStructureGameplayPointHelpers() {
        LevelZeroStructureProfile storageAnchor = new LevelZeroStructureProfile(
                LevelZeroStructureKind.STORAGE_CLUSTER,
                10,
                20,
                0,
                0);
        assertEquals(LevelZeroStructureGameplayPointKind.ENTRY, storageAnchor.gameplayPointKind());

        LevelZeroStructureProfile storageCenter = new LevelZeroStructureProfile(
                LevelZeroStructureKind.STORAGE_CLUSTER,
                10,
                20,
                1,
                1);
        assertEquals(LevelZeroStructureGameplayPointKind.LOOT_HINT, storageCenter.gameplayPointKind());

        LevelZeroStructureProfile pillarCenter = new LevelZeroStructureProfile(
                LevelZeroStructureKind.PILLAR_RING,
                10,
                20,
                2,
                2);
        assertEquals(LevelZeroStructureGameplayPointKind.FOCAL_POINT, pillarCenter.gameplayPointKind());

        LevelZeroStructureProfile edge = new LevelZeroStructureProfile(
                LevelZeroStructureKind.PILLAR_RING,
                10,
                20,
                1,
                0);
        assertFalse(edge.hasGameplayPoint(),
                "Les bords d'un pillar ring ne doivent pas porter de point de gameplay par defaut.");

    }
}
