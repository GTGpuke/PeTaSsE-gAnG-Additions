package com.gtgpuke.gtgsbackrooms.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureFlags;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.core.ModConstants;

@DisplayName("Onglet creatif du mod")
class ModCreativeTabTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("L'onglet liste la baguette en tete, puis les blocs du Level 0")
    void tabListsTheWandThenTheLevelZeroBlocks() {
        // Les onglets vanilla dereferencent des tags absents hors jeu : seul celui du mod est rempli.
        ItemGroup.DisplayContext context = new ItemGroup.DisplayContext(FeatureFlags.DEFAULT_ENABLED_FEATURES, true, BuiltinRegistries.createWrapperLookup());
        ModCreativeTab.TAB.updateEntries(context);
        List<Item> shown = ModCreativeTab.TAB.getDisplayStacks().stream().map(ItemStack::getItem).toList();
        assertEquals(ModCreativeTab.ENTRIES, shown, "Les items de l'onglet, dans l'ordre.");
        assertEquals(ModItems.PORTAL_WAND, shown.get(0), "La baguette de portail en tete.");
        assertTrue(ModCreativeTab.TAB.getIcon().isOf(ModItems.LEVEL_ZERO_WALLPAPER), "L'icone est le papier peint du Level 0.");
        assertEquals(1, Registries.ITEM_GROUP.getIds().stream().filter(id -> id.getNamespace().equals(ModConstants.MOD_ID)).count(),
                "Le mod declare un seul onglet.");
    }
}
