package com.gtgpuke.gtgsbackrooms.init;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.block.PortalSettings;
import com.gtgpuke.gtgsbackrooms.core.ModConstants;

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

    /** Items de l'onglet des portails : le bloc portail, une pile par reglage propose. */
    public static final List<Item> PORTAL_ENTRIES = List.of(ModItems.BACKROOMS_PORTAL);

    /** Onglet creatif principal. */
    public static final ItemGroup MAIN_TAB = register("main", ModItems.GANG_BADGE,
            () -> ENTRIES.stream().map(ItemStack::new).toList());

    /** Onglet creatif des portails Backrooms : chacun de ses items dans chacun des reglages proposes. */
    public static final ItemGroup PORTAL_TAB = register("portals", ModItems.BACKROOMS_PORTAL,
            () -> PORTAL_ENTRIES.stream()
                    .flatMap(item -> PortalSettings.PRESETS.stream().map(settings -> settings.toStack(new ItemStack(item))))
                    .toList());

    private ModCreativeTab() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Force le chargement de la classe, donc l'enregistrement des onglets. */
    public static void register() {
        ModConstants.LOGGER.debug("Onglets creatifs du mod enregistres.");
    }

    private static ItemGroup register(String name, Item icon, Supplier<List<ItemStack>> entries) {
        return Registry.register(
                Registries.ITEM_GROUP,
                Identifier.of(ModConstants.MOD_ID, name),
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup." + ModConstants.MOD_ID + "." + name))
                        .icon(() -> new ItemStack(icon))
                        .entries((context, output) -> output.addAll(entries.get()))
                        .build());
    }
}
