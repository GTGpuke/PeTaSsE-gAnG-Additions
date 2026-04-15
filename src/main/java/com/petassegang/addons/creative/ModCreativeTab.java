package com.petassegang.addons.creative;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import com.petassegang.addons.init.ModItems;
import com.petassegang.addons.util.ModConstants;

/**
 * Onglet creatif personnalise pour PeTaSsE_gAnG_Additions.
 */
public final class ModCreativeTab {

    /** L'onglet creatif principal de la PetasseGang. */
    public static final ItemGroup PETASSEGANG_TAB = Registry.register(
            Registries.ITEM_GROUP,
            Identifier.of(ModConstants.MOD_ID, "petassegang"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.petasse_gang_additions.petassegang"))
                    .icon(() -> new ItemStack(ModItems.GANG_BADGE))
                    .entries(ModCreativeTab::displayItems)
                    .build()
    );

    /**
     * Peuple l'onglet creatif avec tous les items enregistres.
     */
    private static void displayItems(ItemGroup.DisplayContext context, ItemGroup.Entries entries) {
        entries.add(ModItems.GANG_BADGE);
        entries.add(ModItems.CURSED_SNACK);
        entries.add(ModItems.LEVEL_ZERO_WALLPAPER);
        entries.add(ModItems.LEVEL_ZERO_WALLPAPER_AGED);
        entries.add(ModItems.LEVEL_ZERO_WALLPAPER_ADAPTIVE);
        entries.add(ModItems.LEVEL_ZERO_DAMP_CARPET);
        entries.add(ModItems.LEVEL_ZERO_DAMP_CARPET_AGED);
        entries.add(ModItems.LEVEL_ZERO_CEILING_TILE);
        entries.add(ModItems.LEVEL_ZERO_FLUORESCENT_LIGHT);
        entries.add(ModItems.CURSED_LOG);
        entries.add(ModItems.CURSED_PLANKS);
        entries.add(ModItems.CURSED_LEAVES);
        entries.add(ModItems.CURSED_SAPLING);
    }

    /**
     * Force le chargement de la classe et l'enregistrement de l'onglet.
     */
    public static void register() {
        ModConstants.LOGGER.debug("Onglet creatif du mod enregistre.");
    }

    private ModCreativeTab() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
