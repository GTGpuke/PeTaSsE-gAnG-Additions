package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.block.Blocks;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.biome.LevelZeroSurfaceBiome;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.block.LevelZeroWallpaperBlock;
import com.gtgpuke.gtgsbackrooms.core.registry.ModBlocks;

@DisplayName("Palette de blocs du Level 0")
class LevelZeroBlockPaletteTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("La palette runtime utilise les blocs du mod")
    void registryPalette() {
        LevelZeroBlockPalette palette = LevelZeroBlockPalette.fromRegistry();
        assertTrue(palette.floor(LevelZeroSurfaceBiome.BASE).isOf(ModBlocks.LEVEL_ZERO_DAMP_CARPET), "Sol de base.");
        assertTrue(palette.floor(LevelZeroSurfaceBiome.RED).isOf(ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED), "Sol rouge.");
        assertTrue(palette.ceilingTile().isOf(ModBlocks.LEVEL_ZERO_CEILING_TILE), "Plafond.");
        assertTrue(palette.light().isOf(ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT), "Neon.");
        assertTrue(palette.bedrock().isOf(Blocks.BEDROCK), "Bedrock.");
        assertTrue(palette.air().isAir(), "Air.");
        assertTrue(palette.wallpaper(0).isOf(ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE), "Mur adaptatif.");
    }

    @Test
    @DisplayName("Le mur adaptatif porte le masque de faces demande")
    void wallpaperCarriesFaceMask() {
        LevelZeroBlockPalette palette = LevelZeroBlockPalette.fromRegistry();
        for (int mask = 0; mask <= LevelZeroWallpaperBlock.FULL_MASK; mask++) {
            assertEquals(mask, palette.wallpaper(mask).get(LevelZeroWallpaperBlock.FACE_MASK), "Masque " + mask + ".");
        }
        assertSame(palette.wallpaper(5), palette.wallpaper(5), "Les etats sont precalcules.");
        assertEquals(LevelZeroWallpaperBlock.FULL_MASK, palette.wallpaper(0xFF).get(LevelZeroWallpaperBlock.FACE_MASK),
                "Les bits hors masque sont ignores.");
    }

    @Test
    @DisplayName("Une palette sans propriete face_mask rend le meme mur pour tous les masques")
    void plainWallpaperIgnoresMask() {
        LevelZeroBlockPalette palette = new LevelZeroBlockPalette(
                Blocks.YELLOW_WOOL.getDefaultState(),
                Blocks.RED_WOOL.getDefaultState(),
                Blocks.CALCITE.getDefaultState(),
                Blocks.SEA_LANTERN.getDefaultState(),
                Blocks.END_STONE.getDefaultState(),
                Blocks.BEDROCK.getDefaultState());
        assertSame(palette.wallpaper(0), palette.wallpaper(LevelZeroWallpaperBlock.FULL_MASK), "Meme etat quel que soit le masque.");
        assertTrue(palette.wallpaper(3).isOf(Blocks.END_STONE), "Le bloc fourni est conserve.");
    }
}
