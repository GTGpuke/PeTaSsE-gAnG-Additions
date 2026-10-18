package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Coordonnees du Level 0")
class LevelZeroCoordsTest {

    @Test
    @DisplayName("Un chunk couvre 16 blocs, bornes incluses")
    void chunkBounds() {
        assertEquals(0, LevelZeroCoords.chunkStart(0), "Le chunk 0 commence en 0.");
        assertEquals(15, LevelZeroCoords.chunkEnd(0), "Le chunk 0 finit en 15.");
        assertEquals(-16, LevelZeroCoords.chunkStart(-1), "Le chunk -1 commence en -16.");
        assertEquals(-1, LevelZeroCoords.chunkEnd(-1), "Le chunk -1 finit en -1.");
    }

    @Test
    @DisplayName("Une cellule couvre 3 blocs et son centre est le bloc du milieu")
    void cellBounds() {
        assertEquals(3, LevelZeroCoords.CELL_SCALE, "L'echelle de cellule est 3.");
        assertEquals(6, LevelZeroCoords.cellToWorldMin(2), "La cellule 2 commence en 6.");
        assertEquals(7, LevelZeroCoords.cellCenterWorld(2), "Le centre de la cellule 2 est en 7.");
        assertEquals(-3, LevelZeroCoords.cellToWorldMin(-1), "La cellule -1 commence en -3.");
    }

    @ParameterizedTest(name = "monde {0}")
    @ValueSource(ints = {-7, -4, -3, -1, 0, 1, 2, 3, 5, 16, 17})
    @DisplayName("Aller-retour monde vers cellule et sous-cellule")
    void worldToCellRoundTrip(int worldCoord) {
        int cell = LevelZeroCoords.worldToCell(worldCoord);
        int sub = LevelZeroCoords.subCell(worldCoord);
        assertEquals(worldCoord, LevelZeroCoords.cellToWorldMin(cell) + sub,
                "La cellule et la sous-cellule doivent recomposer la coordonnee monde.");
        assertEquals(true, sub >= 0 && sub < LevelZeroCoords.CELL_SCALE,
                "La sous-cellule reste dans [0, 3[.");
    }

    @Test
    @DisplayName("La coordonnee locale de chunk reste positive pour les coordonnees negatives")
    void worldToLocalHandlesNegatives() {
        assertEquals(15, LevelZeroCoords.worldToLocal(-1), "-1 est le dernier bloc du chunk -1.");
        assertEquals(0, LevelZeroCoords.worldToLocal(-16), "-16 est le premier bloc du chunk -1.");
        assertEquals(5, LevelZeroCoords.worldToLocal(37), "37 est le bloc local 5 du chunk 2.");
    }
}
