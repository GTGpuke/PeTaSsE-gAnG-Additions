package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.resources.Identifier;

import com.petassegang.addons.init.ModBlockEntities;
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.init.ModChunkGenerators;
import com.petassegang.addons.init.ModItems;
import com.petassegang.addons.util.ModConstants;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifie la presence des registres du Level 0.
 */
@DisplayName("Registres du Level 0")
class BackroomsLevelZeroRegistryTest {

    @Test
    @DisplayName("Le generateur de chunk du Level 0 est enregistre")
    void testChunkGeneratorRegistryObjectNotNull() {
        assertNotNull(ModChunkGenerators.BACKROOMS_LEVEL_ZERO,
                "Le RegistryObject du generateur du Level 0 doit etre non-null.");
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "backrooms_level_zero"),
                ModChunkGenerators.BACKROOMS_LEVEL_ZERO.getId(),
                "L'identifiant du generateur de chunk du Level 0 doit etre correct."
        );
    }

    @Test
    @DisplayName("La block entity du papier peint du Level 0 est enregistree")
    void testLevelZeroWallpaperBlockEntityRegistryObjectNotNull() {
        assertNotNull(ModBlockEntities.LEVEL_ZERO_WALLPAPER,
                "Le RegistryObject de la block entity du papier peint doit etre non-null.");
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_wallpaper"),
                ModBlockEntities.LEVEL_ZERO_WALLPAPER.getId(),
                "L'identifiant de la block entity du papier peint du Level 0 doit etre correct."
        );
    }

    @Test
    @DisplayName("Les blocs du Level 0 existent")
    void testLevelZeroBlocksRegistryObjectsNotNull() {
        assertNotNull(ModBlocks.LEVEL_ZERO_WALLPAPER,
                "Le RegistryObject LEVEL_ZERO_WALLPAPER doit etre non-null.");
        assertNotNull(ModBlocks.LEVEL_ZERO_WALLPAPER_AGED,
                "Le RegistryObject LEVEL_ZERO_WALLPAPER_AGED doit etre non-null.");
        assertNotNull(ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE,
                "Le RegistryObject LEVEL_ZERO_WALLPAPER_ADAPTIVE doit etre non-null.");
        assertNotNull(ModBlocks.LEVEL_ZERO_WALL_INSULATION,
                "Le RegistryObject LEVEL_ZERO_WALL_INSULATION doit etre non-null.");
        assertNotNull(ModBlocks.LEVEL_ZERO_DAMP_CARPET,
                "Le RegistryObject LEVEL_ZERO_DAMP_CARPET doit etre non-null.");
        assertNotNull(ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED,
                "Le RegistryObject LEVEL_ZERO_DAMP_CARPET_AGED doit etre non-null.");
        assertNotNull(ModBlocks.LEVEL_ZERO_CEILING_TILE,
                "Le RegistryObject LEVEL_ZERO_CEILING_TILE doit etre non-null.");
        assertNotNull(ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT,
                "Le RegistryObject LEVEL_ZERO_FLUORESCENT_LIGHT doit etre non-null.");
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_wallpaper"),
                ModBlocks.LEVEL_ZERO_WALLPAPER.getId(),
                "L'identifiant du bloc level_zero_wallpaper doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_wallpaper_aged"),
                ModBlocks.LEVEL_ZERO_WALLPAPER_AGED.getId(),
                "L'identifiant du bloc level_zero_wallpaper_aged doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_wallpaper_adaptive"),
                ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE.getId(),
                "L'identifiant du bloc technique level_zero_wallpaper_adaptive doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_wall_insulation"),
                ModBlocks.LEVEL_ZERO_WALL_INSULATION.getId(),
                "L'identifiant du bloc interne level_zero_wall_insulation doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_damp_carpet"),
                ModBlocks.LEVEL_ZERO_DAMP_CARPET.getId(),
                "L'identifiant du bloc level_zero_damp_carpet doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_damp_carpet_aged"),
                ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED.getId(),
                "L'identifiant du bloc level_zero_damp_carpet_aged doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_ceiling_tile"),
                ModBlocks.LEVEL_ZERO_CEILING_TILE.getId(),
                "L'identifiant du bloc level_zero_ceiling_tile doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_fluorescent_light"),
                ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT.getId(),
                "L'identifiant du bloc level_zero_fluorescent_light doit etre correct."
        );
    }

    @Test
    @DisplayName("Les BlockItems du Level 0 existent")
    void testLevelZeroItemsRegistryObjectsNotNull() {
        assertNotNull(ModItems.LEVEL_ZERO_WALLPAPER,
                "Le RegistryObject item LEVEL_ZERO_WALLPAPER doit etre non-null.");
        assertNotNull(ModItems.LEVEL_ZERO_WALLPAPER_AGED,
                "Le RegistryObject item LEVEL_ZERO_WALLPAPER_AGED doit etre non-null.");
        assertNotNull(ModItems.LEVEL_ZERO_WALLPAPER_ADAPTIVE,
                "Le RegistryObject item LEVEL_ZERO_WALLPAPER_ADAPTIVE doit etre non-null.");
        assertNotNull(ModItems.LEVEL_ZERO_WALL_INSULATION,
                "Le RegistryObject item LEVEL_ZERO_WALL_INSULATION doit etre non-null.");
        assertNotNull(ModItems.LEVEL_ZERO_DAMP_CARPET,
                "Le RegistryObject item LEVEL_ZERO_DAMP_CARPET doit etre non-null.");
        assertNotNull(ModItems.LEVEL_ZERO_DAMP_CARPET_AGED,
                "Le RegistryObject item LEVEL_ZERO_DAMP_CARPET_AGED doit etre non-null.");
        assertNotNull(ModItems.LEVEL_ZERO_CEILING_TILE,
                "Le RegistryObject item LEVEL_ZERO_CEILING_TILE doit etre non-null.");
        assertNotNull(ModItems.LEVEL_ZERO_FLUORESCENT_LIGHT,
                "Le RegistryObject item LEVEL_ZERO_FLUORESCENT_LIGHT doit etre non-null.");
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_wallpaper"),
                ModItems.LEVEL_ZERO_WALLPAPER.getId(),
                "L'identifiant de l'item level_zero_wallpaper doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_wallpaper_aged"),
                ModItems.LEVEL_ZERO_WALLPAPER_AGED.getId(),
                "L'identifiant de l'item level_zero_wallpaper_aged doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_wallpaper_adaptive"),
                ModItems.LEVEL_ZERO_WALLPAPER_ADAPTIVE.getId(),
                "L'identifiant de l'item level_zero_wallpaper_adaptive doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_wall_insulation"),
                ModItems.LEVEL_ZERO_WALL_INSULATION.getId(),
                "L'identifiant de l'item level_zero_wall_insulation doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_damp_carpet"),
                ModItems.LEVEL_ZERO_DAMP_CARPET.getId(),
                "L'identifiant de l'item level_zero_damp_carpet doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_damp_carpet_aged"),
                ModItems.LEVEL_ZERO_DAMP_CARPET_AGED.getId(),
                "L'identifiant de l'item level_zero_damp_carpet_aged doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_ceiling_tile"),
                ModItems.LEVEL_ZERO_CEILING_TILE.getId(),
                "L'identifiant de l'item level_zero_ceiling_tile doit etre correct."
        );
        assertEquals(
                Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "level_zero_fluorescent_light"),
                ModItems.LEVEL_ZERO_FLUORESCENT_LIGHT.getId(),
                "L'identifiant de l'item level_zero_fluorescent_light doit etre correct."
        );
    }
}
