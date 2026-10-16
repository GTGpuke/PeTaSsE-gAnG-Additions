package com.petassegang.addons.init;

import net.minecraft.block.Block;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.feature.cursed.CursedSnackItem;
import com.petassegang.addons.feature.gang.GangBadgeItem;

/**
 * Registre des items du mod.
 */
public final class ModItems {

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

    /** Porte des Backrooms : une par passage, elle pose ses portails toute seule. */
    public static final BlockItem BACKROOMS_DOOR = blockItem("backrooms_door", ModBlocks.BACKROOMS_DOOR);

    /** Badge de la Gang : jeton d'appartenance, unique par pile, rarete EPIC. */
    public static final GangBadgeItem GANG_BADGE = register(
            "gang_badge",
            new GangBadgeItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));

    /** Casse-croute Maudit : consommable qui retire de la faim, toujours mangeable. */
    public static final CursedSnackItem CURSED_SNACK = register(
            "cursed_snack",
            new CursedSnackItem(new Item.Settings()
                    .maxCount(16)
                    .food(new FoodComponent.Builder().nutrition(0).saturationModifier(0f).alwaysEdible().build())));

    public static final BlockItem CURSED_LOG = blockItem("cursed_log", ModBlocks.CURSED_LOG);
    public static final BlockItem CURSED_LEAVES = blockItem("cursed_leaves", ModBlocks.CURSED_LEAVES);
    public static final BlockItem CURSED_SAPLING = blockItem("cursed_sapling", ModBlocks.CURSED_SAPLING);
    public static final BlockItem CURSED_PLANKS = blockItem("cursed_planks", ModBlocks.CURSED_PLANKS);

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
