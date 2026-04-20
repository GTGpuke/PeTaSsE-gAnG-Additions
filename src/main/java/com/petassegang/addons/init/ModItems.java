package com.petassegang.addons.init;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import com.petassegang.addons.item.CursedSnackItem;
import com.petassegang.addons.item.GangBadgeItem;
import com.petassegang.addons.util.ModConstants;

/**
 * Registre central des items pour PeTaSsE_gAnG_Additions.
 */
public final class ModItems {

    /** Item pour le papier peint jauni du Level 0. */
    public static final BlockItem LEVEL_ZERO_WALLPAPER = register(
            "level_zero_wallpaper",
            new BlockItem(ModBlocks.LEVEL_ZERO_WALLPAPER, new Item.Settings()));

    /** Item pour le papier peint blanc du Level 0. */
    public static final BlockItem LEVEL_ZERO_WALLPAPER_AGED = register(
            "level_zero_wallpaper_aged",
            new BlockItem(ModBlocks.LEVEL_ZERO_WALLPAPER_AGED, new Item.Settings()));

    /** Item de test pour le papier peint adaptatif interne du Level 0. */
    public static final BlockItem LEVEL_ZERO_WALLPAPER_ADAPTIVE = register(
            "level_zero_wallpaper_adaptive",
            new BlockItem(ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE, new Item.Settings()));

    /** Item pour la moquette humide du Level 0. */
    public static final BlockItem LEVEL_ZERO_DAMP_CARPET = register(
            "level_zero_damp_carpet",
            new BlockItem(ModBlocks.LEVEL_ZERO_DAMP_CARPET, new Item.Settings()));

    /** Item pour la moquette rouge du second biome du Level 0. */
    public static final BlockItem LEVEL_ZERO_DAMP_CARPET_AGED = register(
            "level_zero_damp_carpet_aged",
            new BlockItem(ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED, new Item.Settings()));

    /** Item pour la dalle de plafond du Level 0. */
    public static final BlockItem LEVEL_ZERO_CEILING_TILE = register(
            "level_zero_ceiling_tile",
            new BlockItem(ModBlocks.LEVEL_ZERO_CEILING_TILE, new Item.Settings()));

    /** Item pour le neon fluorescent du Level 0. */
    public static final BlockItem LEVEL_ZERO_FLUORESCENT_LIGHT = register(
            "level_zero_fluorescent_light",
            new BlockItem(ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT, new Item.Settings()));

    /** Item pour la plinthe adaptative du Level 0. */
    public static final BlockItem LEVEL_ZERO_BASEBOARD = register(
            "level_zero_baseboard",
            new BlockItem(ModBlocks.LEVEL_ZERO_BASEBOARD, new Item.Settings()));

    /**
     * Badge de la Gang, jeton officiel d'appartenance a la PetasseGang.
     * Taille de pile 1, rarete EPIC.
     */
    public static final GangBadgeItem GANG_BADGE = register(
            "gang_badge",
            new GangBadgeItem(new Item.Settings().maxCount(1).rarity(Rarity.EPIC)));

    /**
     * Casse-croute Maudit, consommable qui retire 2 points de nourriture.
     * Toujours mangeable, meme le ventre plein.
     */
    public static final CursedSnackItem CURSED_SNACK = register(
            "cursed_snack",
            new CursedSnackItem(new Item.Settings()
                    .maxCount(16)
                    .food(new FoodComponent.Builder()
                            .nutrition(0)
                            .saturationModifier(0f)
                            .alwaysEdible()
                            .build())));

    /** Item pour le tronc de l'Arbre Maudit. */
    public static final BlockItem CURSED_LOG = register(
            "cursed_log",
            new BlockItem(ModBlocks.CURSED_LOG, new Item.Settings()));

    /** Item pour les feuilles de l'Arbre Maudit. */
    public static final BlockItem CURSED_LEAVES = register(
            "cursed_leaves",
            new BlockItem(ModBlocks.CURSED_LEAVES, new Item.Settings()));

    /** Item pour la pousse de l'Arbre Maudit. */
    public static final BlockItem CURSED_SAPLING = register(
            "cursed_sapling",
            new BlockItem(ModBlocks.CURSED_SAPLING, new Item.Settings()));

    /** Item pour les planches de l'Arbre Maudit. */
    public static final BlockItem CURSED_PLANKS = register(
            "cursed_planks",
            new BlockItem(ModBlocks.CURSED_PLANKS, new Item.Settings()));

    /**
     * Force le chargement de la classe et l'enregistrement des items.
     */
    public static void register() {
        ModConstants.LOGGER.debug("Items du mod enregistres.");
    }

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM, Identifier.of(ModConstants.MOD_ID, name), item);
    }

    private ModItems() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
