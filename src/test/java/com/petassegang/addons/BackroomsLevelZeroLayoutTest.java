package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroLayout;
import com.petassegang.addons.backrooms.level.level0.biome.LevelZeroSurfaceBiome;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellConnections;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellTag;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellState;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellTopology;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellMicroPattern;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorRoomKind;

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
    @DisplayName("Le biome de surface reste stable sur une large region")
    void testSurfaceBiomeStaysStableAcrossRegion() {
        LevelZeroSurfaceBiome reference = LevelZeroSurfaceBiome.sampleAtWorld(0, 0);
        LevelZeroSurfaceBiome neighbor = LevelZeroSurfaceBiome.sampleAtWorld(96, 96);

        assertEquals(reference, neighbor,
                "Une meme grande region cosmetique ne doit pas se fragmenter en petits carreaux.");
    }

    @Test
    @DisplayName("Le biome de surface peut etre retrouve depuis le bloc de sol")
    void testSurfaceBiomeCanBeRecoveredFromFloorVariant() {
        assertEquals(LevelZeroSurfaceBiome.BASE, LevelZeroSurfaceBiome.fromFloorVariant(LevelZeroLayout.SURFACE_VARIANT_BASE),
                "La moquette jaune doit correspondre au biome de base.");
        assertEquals(LevelZeroSurfaceBiome.RED, LevelZeroSurfaceBiome.fromFloorVariant(LevelZeroLayout.SURFACE_VARIANT_ALTERNATE),
                "La moquette rouge doit correspondre au biome alternatif.");
    }

    @Test
    @DisplayName("Le marquage de grande piece reste coherent dans une cellule logique")
    void testLargeRoomFlagStaysCoherentInsideLogicalCell() {
        LevelZeroLayout layout = LevelZeroLayout.generate(10, 6, 24680L);
        boolean largeRoom = layout.isLargeRoom(6, 9);

        assertEquals(largeRoom, layout.isLargeRoom(7, 9),
                "Le marquage de grande piece doit rester stable sur la meme cellule logique.");
        assertEquals(largeRoom, layout.isLargeRoom(7, 11),
                "Le marquage de grande piece doit couvrir toute la cellule de trois par trois blocs.");
    }

    @Test
    @DisplayName("Le tag semantique reste coherent avec le layout historique")
    void testCellTagMatchesHistoricalFlags() {
        LevelZeroLayout layout = LevelZeroLayout.generate(10, 6, 24680L);

        assertEquals(layout.isWalkable(6, 9) ? (layout.isLargeRoom(6, 9) ? LevelZeroCellTag.ROOM_LARGE : LevelZeroCellTag.CORRIDOR) : LevelZeroCellTag.WALL,
                layout.cellTag(6, 9),
                "Le tag doit refleter la combinaison walkable/largeRoom historique.");
        assertEquals(layout.cellTag(6, 9), layout.cellTag(7, 9),
                "Le tag doit rester stable sur toute la cellule logique.");
        assertEquals(layout.cellTag(6, 9), layout.cellTag(7, 11),
                "Le tag ne doit pas varier a l'interieur d'une meme cellule 3x3.");
    }

    @Test
    @DisplayName("L'etat de cellule regroupe bien les donnees historiques")
    void testCellStateMatchesHistoricalAccessors() {
        LevelZeroLayout layout = LevelZeroLayout.generate(8, 11, 13579L);
        LevelZeroCellState state = layout.cellState(7, 7);

        assertEquals(layout.cellTag(7, 7), state.tag(),
                "Le tag de l'etat de cellule doit correspondre a l'accesseur historique.");
        assertEquals(layout.surfaceBiome(7, 7), state.surfaceBiome(),
                "Le biome de surface de l'etat doit correspondre a l'accesseur historique.");
        assertEquals(layout.isLargeRoom(7, 7), state.largeRoom(),
                "Le flag grande piece de l'etat doit correspondre a l'accesseur historique.");
        assertEquals(layout.hasLight(7, 7), state.lighted(),
                "Le flag lumiere de l'etat doit correspondre a l'accesseur historique.");
        assertEquals(true, state.roomKind() != null,
                "Le type de salle legacy doit etre disponible dans l'etat de cellule.");
        assertEquals(true, state.roomKind() == LevelZeroSectorRoomKind.NONE || state.walkable(),
                "Un type de salle spécifique ne doit pas etre attache a un mur plein.");
        assertEquals(layout.isWalkable(7, 7), state.walkable(),
                "Le caractere traversable de l'etat doit rester coherent avec le layout.");
        assertEquals(true,
                layout.isLargeRoom(7, 7)
                        ? state.topology() == LevelZeroCellTopology.ROOM_LARGE
                        : state.topology() != LevelZeroCellTopology.WALL,
                "La topologie fine doit rester coherente avec l'etat semantique.");
        assertEquals(false, state.hasGeometryFeature(LevelZeroGeometryFeature.NONE),
                "Le masque geometrique ne doit pas repondre positivement a une pseudo-feature NONE.");
        assertEquals(true, state.connectionMask() == 0 || state.walkable(),
                "Un masque de connexions non vide doit correspondre a une cellule traversable.");
        assertEquals(false, state.hasConnection(LevelZeroCellConnections.NORTH) && !state.walkable(),
                "Un mur ne doit pas annoncer de connexion cardinale.");
        assertEquals(true, LevelZeroCellMicroPattern.isOpen(state.microPattern(), state.subCellX(), state.subCellZ()) == state.isMicroOpen(),
                "Le helper micro-geometrique doit rester coherent avec le motif 3x3 stocke.");
    }
}
