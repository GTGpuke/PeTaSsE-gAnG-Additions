package com.petassegang.addons;

import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.init.ModItems;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie que les RegistryObject de l'Arbre Maudit sont non-null au chargement de la classe.
 */
@DisplayName("Registre de l'Arbre Maudit")
class CursedTreeTest {

    @Test
    @DisplayName("ModBlocks.BLOCKS DeferredRegister n'est pas null")
    void testBlocksDeferredRegisterNotNull() {
        assertNotNull(ModBlocks.BLOCKS,
                "Le DeferredRegister BLOCKS doit être créé de façon eagerly.");
    }

    @Test
    @DisplayName("CURSED_LOG RegistryObject n'est pas null")
    void testCursedLogRegistryObjectNotNull() {
        assertNotNull(ModBlocks.CURSED_LOG,
                "Le RegistryObject CURSED_LOG doit être non-null avant l'enregistrement.");
    }

    @Test
    @DisplayName("CURSED_LEAVES RegistryObject n'est pas null")
    void testCursedLeavesRegistryObjectNotNull() {
        assertNotNull(ModBlocks.CURSED_LEAVES,
                "Le RegistryObject CURSED_LEAVES doit être non-null avant l'enregistrement.");
    }

    @Test
    @DisplayName("CURSED_SAPLING RegistryObject n'est pas null")
    void testCursedSaplingRegistryObjectNotNull() {
        assertNotNull(ModBlocks.CURSED_SAPLING,
                "Le RegistryObject CURSED_SAPLING doit être non-null avant l'enregistrement.");
    }

    @Test
    @DisplayName("CURSED_PLANKS RegistryObject n'est pas null")
    void testCursedPlanksRegistryObjectNotNull() {
        assertNotNull(ModBlocks.CURSED_PLANKS,
                "Le RegistryObject CURSED_PLANKS doit être non-null avant l'enregistrement.");
    }

    @Test
    @DisplayName("CURSED_TREE_GROWER n'est pas null")
    void testCursedTreeGrowerNotNull() {
        assertNotNull(ModBlocks.CURSED_TREE_GROWER,
                "Le TreeGrower CURSED_TREE_GROWER doit être non-null.");
    }

    @Test
    @DisplayName("Items de blocs enregistrés (BlockItems non-null)")
    void testCursedBlockItemsRegistryObjectsNotNull() {
        assertNotNull(ModItems.CURSED_LOG,
                "Le RegistryObject CURSED_LOG item doit être non-null avant l'enregistrement.");
        assertNotNull(ModItems.CURSED_LEAVES,
                "Le RegistryObject CURSED_LEAVES item doit être non-null avant l'enregistrement.");
        assertNotNull(ModItems.CURSED_SAPLING,
                "Le RegistryObject CURSED_SAPLING item doit être non-null avant l'enregistrement.");
        assertNotNull(ModItems.CURSED_PLANKS,
                "Le RegistryObject CURSED_PLANKS item doit être non-null avant l'enregistrement.");
    }
}
