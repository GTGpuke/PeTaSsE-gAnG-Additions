package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorData;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorGenerator;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorRoomKind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie les metadonnees de salles remontees par le generateur de secteurs.
 */
@DisplayName("Sector generator du Level 0")
class BackroomsLevelZeroSectorGeneratorTest {

    @Test
    @DisplayName("Les metadonnees de salle restent deterministes")
    void testSectorRoomKindsStayDeterministic() {
        LevelZeroSectorGenerator generator = new LevelZeroSectorGenerator(
                64, 64, 64 * 64,
                0.7D, 40, 0.5D,
                2, 4, 10,
                1, 6, 12, 2, 4,
                1, 3, 6, 3, 8);

        LevelZeroSectorData first = generator.generate(2, -1, 123456L);
        LevelZeroSectorData second = generator.generate(2, -1, 123456L);

        for (int z = 0; z < 64; z++) {
            for (int x = 0; x < 64; x++) {
                assertEquals(first.isWalkable(x, z), second.isWalkable(x, z),
                        "La traversabilite d'un secteur doit rester deterministe.");
                assertEquals(first.roomKind(x, z), second.roomKind(x, z),
                        "Le type de salle d'un secteur doit rester deterministe.");
            }
        }
    }

    @Test
    @DisplayName("Une room rectangulaire marque bien des cellules en RECT_ROOM")
    void testRectRoomsMarkCells() {
        LevelZeroSectorGenerator generator = new LevelZeroSectorGenerator(
                48, 48, 48 * 48,
                0.0D, 0, 0.5D,
                1, 8, 8,
                0, 1, 1, 2, 2,
                0, 3, 3, 3, 3);

        LevelZeroSectorData sector = generator.generate(0, 0, 9988L);

        assertTrue(countRoomKind(sector, 48, 48, LevelZeroSectorRoomKind.RECT_ROOM) > 0,
                "Une generation avec room rectangulaire doit marquer des cellules RECT_ROOM.");
    }

    @Test
    @DisplayName("Une room a piliers marque bien des cellules en PILLAR_ROOM")
    void testPillarRoomsMarkCells() {
        LevelZeroSectorGenerator generator = new LevelZeroSectorGenerator(
                48, 48, 48 * 48,
                0.0D, 0, 0.5D,
                0, 1, 1,
                1, 10, 10, 3, 3,
                0, 3, 3, 3, 3);

        LevelZeroSectorData sector = generator.generate(1, 2, 5544L);

        assertTrue(countRoomKind(sector, 48, 48, LevelZeroSectorRoomKind.PILLAR_ROOM) > 0,
                "Une generation avec room a piliers doit marquer des cellules PILLAR_ROOM.");
    }

    @Test
    @DisplayName("Une room custom marque bien des cellules en CUSTOM_ROOM")
    void testCustomRoomsMarkCells() {
        LevelZeroSectorGenerator generator = new LevelZeroSectorGenerator(
                64, 64, 64 * 64,
                0.0D, 0, 0.5D,
                0, 1, 1,
                0, 1, 1, 2, 2,
                1, 5, 5, 10, 10);

        LevelZeroSectorData sector = generator.generate(-3, 4, 6677L);

        assertTrue(countRoomKind(sector, 64, 64, LevelZeroSectorRoomKind.CUSTOM_ROOM) > 0,
                "Une generation avec room custom doit marquer des cellules CUSTOM_ROOM.");
    }

    private static int countRoomKind(LevelZeroSectorData sector,
                                     int width,
                                     int height,
                                     LevelZeroSectorRoomKind expectedKind) {
        int count = 0;
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                if (sector.roomKind(x, z) == expectedKind) {
                    count++;
                }
            }
        }
        return count;
    }
}
