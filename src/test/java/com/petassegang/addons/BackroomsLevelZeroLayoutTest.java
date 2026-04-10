package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie les invariants du layout du Level 0.
 */
@DisplayName("Layout du Level 0")
class BackroomsLevelZeroLayoutTest {

    @Test
    @DisplayName("La zone d'origine reste toujours traversable")
    void testSpawnAreaIsWalkable() {
        LevelZeroLayout layout = LevelZeroLayout.generate(0, 0, 12345L);
        assertTrue(layout.isWalkable(0, 0),
                "La case d'origine doit rester traversable pour eviter un spawn dans un mur.");
        assertTrue(layout.isWalkable(8, 8),
                "Le centre du chunk d'origine doit rester traversable.");
    }

    @Test
    @DisplayName("Une cellule logique occupe bien une surface de trois par trois blocs")
    void testCellScaleIsThreeByThree() {
        LevelZeroLayout layout = LevelZeroLayout.generate(6, 9, 424242L);
        boolean origin = layout.isWalkable(3, 6);

        assertEquals(origin, layout.isWalkable(4, 6),
                "Les blocs voisins sur l'axe X doivent partager la meme cellule logique.");
        assertEquals(origin, layout.isWalkable(5, 6),
                "La cellule logique doit couvrir trois blocs sur l'axe X.");
        assertEquals(origin, layout.isWalkable(3, 7),
                "Les blocs voisins sur l'axe Z doivent partager la meme cellule logique.");
        assertEquals(origin, layout.isWalkable(3, 8),
                "La cellule logique doit couvrir trois blocs sur l'axe Z.");
        assertEquals(origin, layout.isWalkable(5, 8),
                "La surface complete de trois par trois blocs doit rester coherente.");
    }

    @Test
    @DisplayName("Les bordures se raccordent entre chunks voisins")
    void testChunkBordersStayConsistent() {
        LevelZeroLayout left = LevelZeroLayout.generate(4, 7, 998877L);
        LevelZeroLayout right = LevelZeroLayout.generate(5, 7, 998877L);
        LevelZeroLayout north = LevelZeroLayout.generate(4, 7, 998877L);
        LevelZeroLayout south = LevelZeroLayout.generate(4, 8, 998877L);

        for (int localZ = 0; localZ < LevelZeroLayout.CHUNK_SIZE; localZ++) {
            assertEquals(left.isWalkable(LevelZeroLayout.CHUNK_SIZE - 1, localZ),
                    right.isWalkable(0, localZ),
                    "La bordure est-ouest doit rester coherente entre deux chunks voisins.");
        }

        for (int localX = 0; localX < LevelZeroLayout.CHUNK_SIZE; localX++) {
            assertEquals(north.isWalkable(localX, LevelZeroLayout.CHUNK_SIZE - 1),
                    south.isWalkable(localX, 0),
                    "La bordure nord-sud doit rester coherente entre deux chunks voisins.");
        }
    }
}
