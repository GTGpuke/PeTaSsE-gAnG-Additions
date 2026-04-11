package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;
import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;

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

    @Test
    @DisplayName("Le biome de surface reste deterministe pour un meme chunk")
    void testSurfaceBiomeIsDeterministic() {
        LevelZeroLayout first = LevelZeroLayout.generate(12, -3, 556677L);
        LevelZeroLayout second = LevelZeroLayout.generate(12, -3, 556677L);

        for (int localX = 0; localX < LevelZeroLayout.CHUNK_SIZE; localX += 5) {
            for (int localZ = 0; localZ < LevelZeroLayout.CHUNK_SIZE; localZ += 5) {
                assertEquals(first.surfaceBiome(localX, localZ), second.surfaceBiome(localX, localZ),
                        "Le biome de surface doit rester stable pour un meme seed et un meme chunk.");
            }
        }
    }

    @Test
    @DisplayName("Le biome de surface pilote bien les variantes de blocs")
    void testSurfaceBiomeControlsVariants() {
        LevelZeroLayout layout = LevelZeroLayout.generate(8, 11, 13579L);
        LevelZeroSurfaceBiome biome = layout.surfaceBiome(7, 7);

        assertEquals(biome.floorVariant(), layout.floorVariant(7, 7),
                "La variante de moquette doit venir du biome de surface.");
        assertEquals(biome.wallpaperVariant(), layout.wallpaperVariant(7, 7),
                "La variante de papier peint doit venir du biome de surface.");
    }

    @Test
    @DisplayName("Le marquage de grande piece reste coherent dans une cellule logique")
    void testLargeRoomFlagStaysCoherentInsideLogicalCell() {
        LevelZeroLayout layout = LevelZeroLayout.generate(10, 6, 24680L);
        boolean largeRoom = layout.isLargeRoom(6, 9);

        assertEquals(largeRoom, layout.isLargeRoom(7, 9),
                "Le marquage de grande piece doit rester stable sur la meme cellule logique.");
        assertEquals(largeRoom, layout.isLargeRoom(8, 11),
                "Le marquage de grande piece doit couvrir toute la cellule de trois par trois blocs.");
    }
}
