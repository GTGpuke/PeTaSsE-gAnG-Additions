package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.sector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Generateur de secteur du Level 0")
class LevelZeroSectorGeneratorTest {

    private static LevelZeroSectorGenerator.Settings small(double mazeFill, int numMazes,
                                                           int rectRooms, int pillarRooms, int customRooms) {
        return new LevelZeroSectorGenerator.Settings(
                48, 48,
                mazeFill, numMazes, 0.5D,
                rectRooms, 4, 10,
                pillarRooms, 8, 12, 2, 3,
                customRooms, 3, 6, 3, 8);
    }

    @Test
    @DisplayName("Les parametres historiques du Level 0 sont figes")
    void levelZeroSettingsAreFrozen() {
        LevelZeroSectorGenerator.Settings settings = LevelZeroSectorGenerator.Settings.LEVEL_ZERO;
        assertEquals(240, settings.cols(), "Largeur historique 1920/8.");
        assertEquals(135, settings.rows(), "Hauteur historique 1080/8.");
        assertEquals(240 * 135, settings.totalCells(), "Nombre total de cellules.");
        assertEquals(1000, settings.numMazes(), "Mille mini-labyrinthes superposes.");
        assertEquals(0.8D, settings.mazeFillPercentage(), 1e-12, "Remplissage cible de 80 %.");
    }

    @Test
    @DisplayName("Un secteur est deterministe pour une meme seed et differe entre seeds")
    void sectorIsDeterministic() {
        LevelZeroSectorGenerator generator = new LevelZeroSectorGenerator(small(0.7D, 40, 2, 1, 1));
        LevelZeroSectorData first = generator.generate(2, -1, 123456L);
        LevelZeroSectorData second = generator.generate(2, -1, 123456L);
        LevelZeroSectorData other = generator.generate(2, -1, 654321L);
        int differences = 0;
        for (int z = 0; z < 48; z++) {
            for (int x = 0; x < 48; x++) {
                assertEquals(first.isWalkable(x, z), second.isWalkable(x, z), "Traversabilite deterministe.");
                assertEquals(first.roomKind(x, z), second.roomKind(x, z), "Type de salle deterministe.");
                if (first.isWalkable(x, z) != other.isWalkable(x, z)) {
                    differences++;
                }
            }
        }
        assertTrue(differences > 0, "Une autre seed doit produire un labyrinthe different.");
    }

    @Test
    @DisplayName("Les mini-labyrinthes ouvrent une part du secteur proche de la cible")
    void mazeFillIsReached() {
        LevelZeroSectorGenerator generator = new LevelZeroSectorGenerator(small(0.5D, 200, 0, 0, 0));
        LevelZeroSectorData sector = generator.generate(0, 0, 777L);
        int open = 0;
        for (int z = 0; z < 48; z++) {
            for (int x = 0; x < 48; x++) {
                if (sector.isWalkable(x, z)) {
                    open++;
                }
            }
        }
        double ratio = open / (48.0D * 48.0D);
        assertTrue(ratio > 0.3D && ratio < 0.9D, "Le remplissage doit rester dans une plage jouable : " + ratio);
    }

    @Test
    @DisplayName("Chaque type de salle marque ses cellules")
    void roomsMarkCells() {
        assertTrue(count(new LevelZeroSectorGenerator(small(0.0D, 0, 1, 0, 0)).generate(0, 0, 9988L),
                LevelZeroSectorRoomKind.RECT_ROOM) > 0, "Une salle rectangulaire marque des cellules RECT_ROOM.");
        assertTrue(count(new LevelZeroSectorGenerator(small(0.0D, 0, 0, 1, 0)).generate(1, 2, 5544L),
                LevelZeroSectorRoomKind.PILLAR_ROOM) > 0, "Une salle a piliers marque des cellules PILLAR_ROOM.");
        assertTrue(count(new LevelZeroSectorGenerator(small(0.0D, 0, 0, 0, 1)).generate(-3, 4, 6677L),
                LevelZeroSectorRoomKind.CUSTOM_ROOM) > 0, "Une salle polygonale marque des cellules CUSTOM_ROOM.");
    }

    @Test
    @DisplayName("Une salle a piliers garde des piliers fermes a l'interieur")
    void pillarRoomsKeepPillars() {
        LevelZeroSectorData sector = new LevelZeroSectorGenerator(small(0.0D, 0, 0, 1, 0)).generate(1, 2, 5544L);
        int closedPillarCells = 0;
        for (int z = 0; z < 48; z++) {
            for (int x = 0; x < 48; x++) {
                if (sector.roomKind(x, z) == LevelZeroSectorRoomKind.PILLAR_ROOM && !sector.isWalkable(x, z)) {
                    closedPillarCells++;
                }
            }
        }
        assertTrue(closedPillarCells > 0, "Les piliers sont des cellules fermees marquees PILLAR_ROOM.");
    }

    private static int count(LevelZeroSectorData sector, LevelZeroSectorRoomKind kind) {
        int count = 0;
        for (int z = 0; z < 48; z++) {
            for (int x = 0; x < 48; x++) {
                if (sector.roomKind(x, z) == kind) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    @DisplayName("Un remplissage total epuise la frontiere sans boucler")
    void fullFillStopsWhenFrontierIsExhausted() {
        LevelZeroSectorGenerator generator = new LevelZeroSectorGenerator(small(1.0D, 1, 0, 0, 0));
        assertNotNull(generator.generate(0, 0, 1L), "Le secteur est produit meme sans atteindre 100 % de cellules.");
        assertNotNull(new LevelZeroSectorGenerator(small(1.0D, 3, 1, 1, 1)).generate(-1, 2, 99L), "Idem avec plusieurs labyrinthes.");
    }

    @Test
    @DisplayName("Un remplissage nul ne creuse aucun labyrinthe")
    void zeroFillDigsNothing() {
        assertNotNull(new LevelZeroSectorGenerator(small(0.0D, 1, 0, 0, 0)).generate(0, 0, 1L), "Le secteur existe, sans couloir de labyrinthe.");
    }
}
