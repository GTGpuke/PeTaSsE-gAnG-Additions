package com.petassegang.addons.init;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;

import com.petassegang.addons.core.ModConstants;

/**
 * Onglets creatifs du mod : le contenu general et les portails.
 */
public final class ModCreativeTab {

    /** Items de l'onglet principal, dans l'ordre. */
    public static final List<Item> ENTRIES = List.of(
            ModItems.GANG_BADGE,
            ModItems.CURSED_SNACK,
            ModItems.LEVEL_ZERO_WALLPAPER,
            ModItems.LEVEL_ZERO_WALLPAPER_AGED,
            ModItems.LEVEL_ZERO_WALLPAPER_ADAPTIVE,
            ModItems.LEVEL_ZERO_DAMP_CARPET,
            ModItems.LEVEL_ZERO_DAMP_CARPET_AGED,
            ModItems.LEVEL_ZERO_CEILING_TILE,
            ModItems.LEVEL_ZERO_FLUORESCENT_LIGHT,
            ModItems.CURSED_LOG,
            ModItems.CURSED_PLANKS,
            ModItems.CURSED_LEAVES,
            ModItems.CURSED_SAPLING);

    /** Items de l'onglet des portails, dans l'ordre. */
    public static final List<Item> PORTAL_ENTRIES = List.of(ModItems.BACKROOMS_DOOR);

    /** Onglet creatif principal. */
    public static final ItemGroup PETASSEGANG_TAB = register("petassegang", ModItems.GANG_BADGE, ENTRIES);

    /** Onglet creatif des portails Backrooms. */
    public static final ItemGroup PORTAL_TAB = register("portals", ModItems.BACKROOMS_DOOR, PORTAL_ENTRIES);

    private ModCreativeTab() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Force le chargement de la classe, donc l'enregistrement des onglets. */
    public static void register() {
        ModConstants.LOGGER.debug("Onglets creatifs du mod enregistres.");
    }

    private static ItemGroup register(String name, Item icon, List<Item> entries) {
        return Registry.register(
                Registries.ITEM_GROUP,
                Identifier.of(ModConstants.MOD_ID, name),
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup." + ModConstants.MOD_ID + "." + name))
                        .icon(() -> new ItemStack(icon))
                        .entries((context, output) -> entries.forEach(output::add))
                        .build());
    }
}
