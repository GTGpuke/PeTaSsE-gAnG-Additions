package com.gtgpuke.gtgsbackrooms.core.registry;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;

/**
 * Onglet creatif du mod : la baguette de portail, puis les blocs du Level 0. Le bloc portail n'y est pas : on cree un
 * portail avec la baguette.
 */
public final class ModCreativeTab {

    /** Items de l'onglet, dans l'ordre. */
    public static final List<Item> ENTRIES = List.of(
            ModItems.PORTAL_WAND,
            ModItems.LEVEL_ZERO_WALLPAPER,
            ModItems.LEVEL_ZERO_WALLPAPER_AGED,
            ModItems.LEVEL_ZERO_WALLPAPER_ADAPTIVE,
            ModItems.LEVEL_ZERO_DAMP_CARPET,
            ModItems.LEVEL_ZERO_DAMP_CARPET_AGED,
            ModItems.LEVEL_ZERO_CEILING_TILE,
            ModItems.LEVEL_ZERO_FLUORESCENT_LIGHT);

    /** Onglet creatif du mod, a l'icone du papier peint du Level 0. */
    public static final ItemGroup TAB = Registry.register(
            Registries.ITEM_GROUP,
            Identifier.of(ModConstants.MOD_ID, "main"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup." + ModConstants.MOD_ID + ".main"))
                    .icon(() -> new ItemStack(ModItems.LEVEL_ZERO_WALLPAPER))
                    .entries((context, output) -> ENTRIES.forEach(output::add))
                    .build());

    private ModCreativeTab() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Force le chargement de la classe, donc l'enregistrement de l'onglet. */
    public static void register() {
        ModConstants.LOGGER.debug("Onglet creatif du mod enregistre.");
    }
}
