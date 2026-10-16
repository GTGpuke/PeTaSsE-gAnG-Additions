package com.petassegang.addons.init;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.item.ItemGroup;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureFlags;

import com.petassegang.addons.MinecraftTestBootstrap;
import com.petassegang.addons.core.ModConstants;

@DisplayName("Onglets creatifs du mod")
class ModCreativeTabTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("Chaque onglet du mod a une icone et liste ses objets une fois le contexte d'affichage connu")
    void tabsListTheirItems() {
        // Les onglets vanilla dereferencent des tags absents hors jeu : seuls ceux du mod sont remplis.
        ItemGroup.DisplayContext context = new ItemGroup.DisplayContext(FeatureFlags.DEFAULT_ENABLED_FEATURES, true, BuiltinRegistries.createWrapperLookup());
        int tabs = 0;
        for (ItemGroup group : Registries.ITEM_GROUP) {
            if (!Registries.ITEM_GROUP.getId(group).getNamespace().equals(ModConstants.MOD_ID)) {
                continue;
            }
            tabs++;
            group.updateEntries(context);
            assertFalse(group.getIcon().isEmpty(), "L'onglet a une icone.");
            assertFalse(group.getDisplayStacks().isEmpty(), "L'onglet liste des objets.");
        }
        assertTrue(tabs >= 2, "Le mod declare au moins ses deux onglets : " + tabs);
        assertTrue(ModCreativeTab.PORTAL_TAB.getDisplayStacks().stream().anyMatch(stack -> stack.isOf(ModItems.BACKROOMS_DOOR)),
                "La porte est dans l'onglet des portails.");
        assertTrue(ModCreativeTab.PORTAL_TAB.getIcon().isOf(ModItems.BACKROOMS_DOOR), "La porte est l'icone de l'onglet des portails.");
    }
}
