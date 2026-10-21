package com.gtgpuke.gtgsbackrooms.core.registry;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalSettings;
import com.gtgpuke.gtgsbackrooms.core.portal.item.PortalWandItem;

/**
 * Registre des items du mod : la baguette de portail, puis l'item de chaque bloc du Level 0. Le bloc portail n'a pas
 * d'item : la baguette le pose.
 */
public final class ModItems {

    /** Baguette de portail : pose des blocs portail aux reglages qu'elle porte, choisis dans son ecran, sans s'user. */
    public static final PortalWandItem PORTAL_WAND = register("portal_wand", new PortalWandItem(
            new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON).component(ModDataComponents.PORTAL_SETTINGS, PortalSettings.DEFAULT)));

    public static final BlockItem LEVEL_ZERO_WALLPAPER = blockItem("level_zero_wallpaper", ModBlocks.LEVEL_ZERO_WALLPAPER);
    public static final BlockItem LEVEL_ZERO_WALLPAPER_AGED =
            blockItem("level_zero_wallpaper_aged", ModBlocks.LEVEL_ZERO_WALLPAPER_AGED);
    public static final BlockItem LEVEL_ZERO_WALLPAPER_ADAPTIVE =
            blockItem("level_zero_wallpaper_adaptive", ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE);
    public static final BlockItem LEVEL_ZERO_DAMP_CARPET = blockItem("level_zero_damp_carpet", ModBlocks.LEVEL_ZERO_DAMP_CARPET);
    public static final BlockItem LEVEL_ZERO_DAMP_CARPET_AGED =
            blockItem("level_zero_damp_carpet_aged", ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED);
    public static final BlockItem LEVEL_ZERO_CEILING_TILE = blockItem("level_zero_ceiling_tile", ModBlocks.LEVEL_ZERO_CEILING_TILE);
    public static final BlockItem LEVEL_ZERO_FLUORESCENT_LIGHT =
            blockItem("level_zero_fluorescent_light", ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT);

    private ModItems() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Force le chargement de la classe, donc l'enregistrement des items. */
    public static void register() {
        ModConstants.LOGGER.debug("Items du mod enregistres.");
    }

    private static BlockItem blockItem(String name, Block block) {
        return register(name, new BlockItem(block, new Item.Settings()));
    }

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM, Identifier.of(ModConstants.MOD_ID, name), item);
    }
}
