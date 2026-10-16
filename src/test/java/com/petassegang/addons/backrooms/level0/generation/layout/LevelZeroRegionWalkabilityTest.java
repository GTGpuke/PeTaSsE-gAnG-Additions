package com.petassegang.addons.backrooms.level0.generation.layout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.level0.generation.sector.LevelZeroSectorRoomKind;

@DisplayName("Fenetre de traversabilite du Level 0")
class LevelZeroRegionWalkabilityTest {

    @Test
    @DisplayName("La fenetre restitue traversabilite et type de salle dans ses bornes")
    void samplesInsideBounds() {
        LevelZeroRegionWalkability region = LevelZeroRegionWalkability.sample(-2, 3, 4, 6,
                (x, z) -> (x + z) % 2 == 0 ? 1 : 0,
                (x, z) -> x == 0 ? LevelZeroSectorRoomKind.PILLAR_ROOM.ordinal() : 0);

        assertTrue(region.contains(-2, 3), "Coin minimal inclus.");
        assertTrue(region.contains(4, 6), "Coin maximal inclus.");
        assertTrue(region.isWalkable(1, 3), "1+3 pair : ouvert.");
        assertFalse(region.isWalkable(2, 3), "2+3 impair : ferme.");
        assertEquals(LevelZeroSectorRoomKind.PILLAR_ROOM, region.roomKind(0, 4), "Type de salle restitue.");
        assertEquals(LevelZeroSectorRoomKind.NONE, region.roomKind(1, 4), "Sans salle.");
    }

    @Test
    @DisplayName("Hors de la fenetre, tout est mur sans salle")
    void outsideIsWall() {
        LevelZeroRegionWalkability region = LevelZeroRegionWalkability.of(0, 0, 2, 2, (x, z) -> 1);
        assertFalse(region.contains(3, 0), "Hors bornes.");
        assertFalse(region.isWalkable(3, 0), "Un voisin hors fenetre est un mur.");
        assertFalse(region.isWalkable(0, -1), "Un voisin hors fenetre est un mur.");
        assertEquals(LevelZeroSectorRoomKind.NONE, region.roomKind(-1, -1), "Pas de salle hors fenetre.");
        assertEquals(0, region.minCellX(), "Borne min X.");
        assertEquals(2, region.maxCellZ(), "Borne max Z.");
    }

    @Test
    @DisplayName("Les quatre bornes sont exposees")
    void boundsAccessors() {
        LevelZeroRegionWalkability region = LevelZeroRegionWalkability.of(-3, 4, 5, 9, (x, z) -> 1);
        assertEquals(-3, region.minCellX(), "Borne min X.");
        assertEquals(4, region.minCellZ(), "Borne min Z.");
        assertEquals(5, region.maxCellX(), "Borne max X.");
        assertEquals(9, region.maxCellZ(), "Borne max Z.");
    }
}
