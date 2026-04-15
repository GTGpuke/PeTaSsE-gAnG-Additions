package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import com.petassegang.addons.block.backrooms.LevelZeroWallpaperBlock;
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.init.ModItems;
import com.petassegang.addons.util.ModConstants;
import com.petassegang.addons.world.backrooms.level0.LevelZeroChunkGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie la presence des registres du Level 0.
 */
@DisplayName("Registres du Level 0")
class BackroomsLevelZeroRegistryTest {

    @Test
    @DisplayName("Le codec du generateur de chunk du Level 0 est defini")
    void testChunkGeneratorCodecNotNull() {
        assertNotNull(LevelZeroChunkGenerator.CODEC,
                "Le codec du generateur du Level 0 doit etre non-null.");
    }

    @Test
    @DisplayName("Le bloc adaptatif possede la propriete face_mask (0-15)")
    void testLevelZeroAdaptiveBlockHasFaceMaskProperty() {
        assertNotNull(LevelZeroWallpaperBlock.FACE_MASK,
                "La propriete FACE_MASK doit etre non-null.");
        assertEquals(0, ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE.getDefaultState().get(LevelZeroWallpaperBlock.FACE_MASK),
                "La valeur par defaut de face_mask doit etre 0.");
        assertTrue(
                ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE.getDefaultState().contains(LevelZeroWallpaperBlock.FACE_MASK),
                "Le bloc adaptatif doit contenir la propriete face_mask.");
    }

    @Test
    @DisplayName("Les blocs du Level 0 existent")
    void testLevelZeroBlocksRegistryObjectsNotNull() {
        assertNotNull(ModBlocks.LEVEL_ZERO_WALLPAPER,
                "Le bloc LEVEL_ZERO_WALLPAPER doit etre non-null.");
        assertNotNull(ModBlocks.LEVEL_ZERO_WALLPAPER_AGED,
                "Le bloc LEVEL_ZERO_WALLPAPER_AGED doit etre non-null.");
        assertNotNull(ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE,
                "Le bloc LEVEL_ZERO_WALLPAPER_ADAPTIVE doit etre non-null.");
        assertNotNull(ModBlocks.LEVEL_ZERO_DAMP_CARPET,
                "Le bloc LEVEL_ZERO_DAMP_CARPET doit etre non-null.");
        assertNotNull(ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED,
                "Le bloc LEVEL_ZERO_DAMP_CARPET_AGED doit etre non-null.");
        assertNotNull(ModBlocks.LEVEL_ZERO_CEILING_TILE,
                "Le bloc LEVEL_ZERO_CEILING_TILE doit etre non-null.");
        assertNotNull(ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT,
                "Le bloc LEVEL_ZERO_FLUORESCENT_LIGHT doit etre non-null.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_wallpaper"),
                Registries.BLOCK.getId(ModBlocks.LEVEL_ZERO_WALLPAPER),
                "L'identifiant du bloc level_zero_wallpaper doit etre correct.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_wallpaper_aged"),
                Registries.BLOCK.getId(ModBlocks.LEVEL_ZERO_WALLPAPER_AGED),
                "L'identifiant du bloc level_zero_wallpaper_aged doit etre correct.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_wallpaper_adaptive"),
                Registries.BLOCK.getId(ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE),
                "L'identifiant du bloc technique level_zero_wallpaper_adaptive doit etre correct.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_damp_carpet"),
                Registries.BLOCK.getId(ModBlocks.LEVEL_ZERO_DAMP_CARPET),
                "L'identifiant du bloc level_zero_damp_carpet doit etre correct.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_damp_carpet_aged"),
                Registries.BLOCK.getId(ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED),
                "L'identifiant du bloc level_zero_damp_carpet_aged doit etre correct.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_ceiling_tile"),
                Registries.BLOCK.getId(ModBlocks.LEVEL_ZERO_CEILING_TILE),
                "L'identifiant du bloc level_zero_ceiling_tile doit etre correct.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_fluorescent_light"),
                Registries.BLOCK.getId(ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT),
                "L'identifiant du bloc level_zero_fluorescent_light doit etre correct.");
    }

    @Test
    @DisplayName("Les BlockItems du Level 0 existent")
    void testLevelZeroItemsRegistryObjectsNotNull() {
        assertNotNull(ModItems.LEVEL_ZERO_WALLPAPER,
                "L'item LEVEL_ZERO_WALLPAPER doit etre non-null.");
        assertNotNull(ModItems.LEVEL_ZERO_WALLPAPER_AGED,
                "L'item LEVEL_ZERO_WALLPAPER_AGED doit etre non-null.");
        assertNotNull(ModItems.LEVEL_ZERO_WALLPAPER_ADAPTIVE,
                "L'item LEVEL_ZERO_WALLPAPER_ADAPTIVE doit etre non-null.");
        assertNotNull(ModItems.LEVEL_ZERO_DAMP_CARPET,
                "L'item LEVEL_ZERO_DAMP_CARPET doit etre non-null.");
        assertNotNull(ModItems.LEVEL_ZERO_DAMP_CARPET_AGED,
                "L'item LEVEL_ZERO_DAMP_CARPET_AGED doit etre non-null.");
        assertNotNull(ModItems.LEVEL_ZERO_CEILING_TILE,
                "L'item LEVEL_ZERO_CEILING_TILE doit etre non-null.");
        assertNotNull(ModItems.LEVEL_ZERO_FLUORESCENT_LIGHT,
                "L'item LEVEL_ZERO_FLUORESCENT_LIGHT doit etre non-null.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_wallpaper"),
                Registries.ITEM.getId(ModItems.LEVEL_ZERO_WALLPAPER),
                "L'identifiant de l'item level_zero_wallpaper doit etre correct.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_wallpaper_aged"),
                Registries.ITEM.getId(ModItems.LEVEL_ZERO_WALLPAPER_AGED),
                "L'identifiant de l'item level_zero_wallpaper_aged doit etre correct.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_wallpaper_adaptive"),
                Registries.ITEM.getId(ModItems.LEVEL_ZERO_WALLPAPER_ADAPTIVE),
                "L'identifiant de l'item level_zero_wallpaper_adaptive doit etre correct.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_damp_carpet"),
                Registries.ITEM.getId(ModItems.LEVEL_ZERO_DAMP_CARPET),
                "L'identifiant de l'item level_zero_damp_carpet doit etre correct.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_damp_carpet_aged"),
                Registries.ITEM.getId(ModItems.LEVEL_ZERO_DAMP_CARPET_AGED),
                "L'identifiant de l'item level_zero_damp_carpet_aged doit etre correct.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_ceiling_tile"),
                Registries.ITEM.getId(ModItems.LEVEL_ZERO_CEILING_TILE),
                "L'identifiant de l'item level_zero_ceiling_tile doit etre correct.");
        assertEquals(
                Identifier.of(ModConstants.MOD_ID, "level_zero_fluorescent_light"),
                Registries.ITEM.getId(ModItems.LEVEL_ZERO_FLUORESCENT_LIGHT),
                "L'identifiant de l'item level_zero_fluorescent_light doit etre correct.");
    }
}
