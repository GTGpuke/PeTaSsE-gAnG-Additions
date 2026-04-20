package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.init.ModItems;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifie que les champs publics de l'Arbre Maudit existent bien dans les
 * registres declaratifs.
 */
@DisplayName("Registre de l'Arbre Maudit")
class CursedTreeTest {

    @Test
    @DisplayName("CURSED_LOG existe dans ModBlocks")
    void testCursedLogFieldExists() throws NoSuchFieldException {
        assertNotNull(ModBlocks.class.getDeclaredField("CURSED_LOG"),
                "Le champ CURSED_LOG doit exister dans ModBlocks.");
    }

    @Test
    @DisplayName("CURSED_LEAVES existe dans ModBlocks")
    void testCursedLeavesFieldExists() throws NoSuchFieldException {
        assertNotNull(ModBlocks.class.getDeclaredField("CURSED_LEAVES"),
                "Le champ CURSED_LEAVES doit exister dans ModBlocks.");
    }

    @Test
    @DisplayName("CURSED_SAPLING existe dans ModBlocks")
    void testCursedSaplingFieldExists() throws NoSuchFieldException {
        assertNotNull(ModBlocks.class.getDeclaredField("CURSED_SAPLING"),
                "Le champ CURSED_SAPLING doit exister dans ModBlocks.");
    }

    @Test
    @DisplayName("CURSED_PLANKS existe dans ModBlocks")
    void testCursedPlanksFieldExists() throws NoSuchFieldException {
        assertNotNull(ModBlocks.class.getDeclaredField("CURSED_PLANKS"),
                "Le champ CURSED_PLANKS doit exister dans ModBlocks.");
    }

    @Test
    @DisplayName("CURSED_TREE_GROWER existe dans ModBlocks")
    void testCursedTreeGrowerFieldExists() throws NoSuchFieldException {
        assertNotNull(ModBlocks.class.getDeclaredField("CURSED_TREE_GROWER"),
                "Le champ CURSED_TREE_GROWER doit exister dans ModBlocks.");
    }

    @Test
    @DisplayName("Les BlockItems maudits existent dans ModItems")
    void testCursedBlockItemFieldsExist() throws NoSuchFieldException {
        assertNotNull(ModItems.class.getDeclaredField("CURSED_LOG"),
                "Le champ CURSED_LOG doit exister dans ModItems.");
        assertNotNull(ModItems.class.getDeclaredField("CURSED_LEAVES"),
                "Le champ CURSED_LEAVES doit exister dans ModItems.");
        assertNotNull(ModItems.class.getDeclaredField("CURSED_SAPLING"),
                "Le champ CURSED_SAPLING doit exister dans ModItems.");
        assertNotNull(ModItems.class.getDeclaredField("CURSED_PLANKS"),
                "Le champ CURSED_PLANKS doit exister dans ModItems.");
    }
}
