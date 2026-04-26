package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.block.backrooms.LevelZeroWallpaperBlock;
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.init.ModItems;
import com.petassegang.addons.world.backrooms.level0.LevelZeroChunkGenerator;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifie la presence declarative des registres du Level 0.
 */
@DisplayName("Registres du Level 0")
class BackroomsLevelZeroRegistryTest {

    @Test
    @DisplayName("Le codec du generateur de chunk du Level 0 est defini")
    void testChunkGeneratorCodecFieldExists() throws NoSuchFieldException {
        assertNotNull(LevelZeroChunkGenerator.class.getDeclaredField("CODEC"),
                "Le champ CODEC du generateur du Level 0 doit exister.");
    }

    @Test
    @DisplayName("Le bloc adaptatif expose toujours la propriete face_mask")
    void testLevelZeroAdaptiveBlockHasFaceMaskProperty() {
        assertNotNull(LevelZeroWallpaperBlock.FACE_MASK,
                "La propriete FACE_MASK doit etre non-null.");
    }

    @Test
    @DisplayName("Les champs de blocs du Level 0 existent dans ModBlocks")
    void testLevelZeroBlockFieldsExist() throws NoSuchFieldException {
        assertNotNull(ModBlocks.class.getDeclaredField("LEVEL_ZERO_WALLPAPER"),
                "Le champ LEVEL_ZERO_WALLPAPER doit exister.");
        assertNotNull(ModBlocks.class.getDeclaredField("LEVEL_ZERO_WALLPAPER_AGED"),
                "Le champ LEVEL_ZERO_WALLPAPER_AGED doit exister.");
        assertNotNull(ModBlocks.class.getDeclaredField("LEVEL_ZERO_WALLPAPER_ADAPTIVE"),
                "Le champ LEVEL_ZERO_WALLPAPER_ADAPTIVE doit exister.");
        assertNotNull(ModBlocks.class.getDeclaredField("LEVEL_ZERO_DAMP_CARPET"),
                "Le champ LEVEL_ZERO_DAMP_CARPET doit exister.");
        assertNotNull(ModBlocks.class.getDeclaredField("LEVEL_ZERO_DAMP_CARPET_AGED"),
                "Le champ LEVEL_ZERO_DAMP_CARPET_AGED doit exister.");
        assertNotNull(ModBlocks.class.getDeclaredField("LEVEL_ZERO_CEILING_TILE"),
                "Le champ LEVEL_ZERO_CEILING_TILE doit exister.");
        assertNotNull(ModBlocks.class.getDeclaredField("LEVEL_ZERO_FLUORESCENT_LIGHT"),
                "Le champ LEVEL_ZERO_FLUORESCENT_LIGHT doit exister.");
        assertNotNull(ModBlocks.class.getDeclaredField("LEVEL_ZERO_BASEBOARD"),
                "Le champ LEVEL_ZERO_BASEBOARD doit exister.");
    }

    @Test
    @DisplayName("Les champs d'items du Level 0 existent dans ModItems")
    void testLevelZeroItemFieldsExist() throws NoSuchFieldException {
        assertNotNull(ModItems.class.getDeclaredField("LEVEL_ZERO_WALLPAPER"),
                "Le champ LEVEL_ZERO_WALLPAPER doit exister.");
        assertNotNull(ModItems.class.getDeclaredField("LEVEL_ZERO_WALLPAPER_AGED"),
                "Le champ LEVEL_ZERO_WALLPAPER_AGED doit exister.");
        assertNotNull(ModItems.class.getDeclaredField("LEVEL_ZERO_WALLPAPER_ADAPTIVE"),
                "Le champ LEVEL_ZERO_WALLPAPER_ADAPTIVE doit exister.");
        assertNotNull(ModItems.class.getDeclaredField("LEVEL_ZERO_DAMP_CARPET"),
                "Le champ LEVEL_ZERO_DAMP_CARPET doit exister.");
        assertNotNull(ModItems.class.getDeclaredField("LEVEL_ZERO_DAMP_CARPET_AGED"),
                "Le champ LEVEL_ZERO_DAMP_CARPET_AGED doit exister.");
        assertNotNull(ModItems.class.getDeclaredField("LEVEL_ZERO_CEILING_TILE"),
                "Le champ LEVEL_ZERO_CEILING_TILE doit exister.");
        assertNotNull(ModItems.class.getDeclaredField("LEVEL_ZERO_FLUORESCENT_LIGHT"),
                "Le champ LEVEL_ZERO_FLUORESCENT_LIGHT doit exister.");
        assertNotNull(ModItems.class.getDeclaredField("LEVEL_ZERO_BASEBOARD"),
                "Le champ LEVEL_ZERO_BASEBOARD doit exister.");
    }
}
