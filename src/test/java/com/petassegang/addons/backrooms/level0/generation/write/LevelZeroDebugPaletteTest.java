package com.petassegang.addons.backrooms.level0.generation.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import com.petassegang.addons.MinecraftTestBootstrap;
import com.petassegang.addons.backrooms.level0.block.LevelZeroWallpaperBlock;

@DisplayName("Palette de debug du Level 0")
class LevelZeroDebugPaletteTest {

    private static final int[] FACES = {
            LevelZeroWallpaperBlock.NORTH_MASK,
            LevelZeroWallpaperBlock.EAST_MASK,
            LevelZeroWallpaperBlock.SOUTH_MASK,
            LevelZeroWallpaperBlock.WEST_MASK,
    };

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("Les blocs de debug de geometrie sont du beton voyant")
    void geometryBlocks() {
        assertTrue(LevelZeroDebugPalette.gapFloor().isOf(Blocks.WHITE_CONCRETE), "Sol de gap.");
        assertTrue(LevelZeroDebugPalette.gapWall().isOf(Blocks.GRAY_CONCRETE), "Mur de gap.");
    }

    @Test
    @DisplayName("Chaque element mural a un bloc distinct par face, et aucun sans element")
    void fixtureBlocksAreDistinctPerFace() {
        assertNull(LevelZeroDebugPalette.fixture(LevelZeroWallProps.Fixture.NONE, LevelZeroWallpaperBlock.NORTH_MASK),
                "Pas de bloc sans element.");
        for (LevelZeroWallProps.Fixture fixture : new LevelZeroWallProps.Fixture[] {
                LevelZeroWallProps.Fixture.SWITCH, LevelZeroWallProps.Fixture.OUTLET}) {
            Set<BlockState> seen = new HashSet<>();
            for (int face : FACES) {
                BlockState state = LevelZeroDebugPalette.fixture(fixture, face);
                assertNotNull(state, fixture + " a un bloc pour la face " + face + ".");
                seen.add(state);
            }
            assertEquals(FACES.length, seen.size(), fixture + " : un bloc different par face pour lire l'orientation.");
        }
    }
}
