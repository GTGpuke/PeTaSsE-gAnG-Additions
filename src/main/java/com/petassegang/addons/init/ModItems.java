package com.petassegang.addons.init;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.petassegang.addons.item.CursedSnackItem;
import com.petassegang.addons.item.GangBadgeItem;
import com.petassegang.addons.util.ModConstants;

/**
 * Registre central des items pour PeTaSsE_gAnG_Additions.
 *
 * <p>Tous les items doivent etre declares ici via {@link DeferredRegister}.
 * En MC 26.1, {@link Item.Properties#setId} doit etre appele avec la
 * {@link net.minecraft.resources.ResourceKey} de l'item avant sa construction.
 * Appeler {@link #register(BusGroup)} une seule fois depuis le constructeur du mod principal.
 */
public final class ModItems {

    /** DeferredRegister central pour tous les items du mod. */
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ModConstants.MOD_ID);

    /** Item pour le papier peint jauni du Level 0. */
    public static final RegistryObject<BlockItem> LEVEL_ZERO_WALLPAPER = ITEMS.register(
            "level_zero_wallpaper",
            () -> new BlockItem(ModBlocks.LEVEL_ZERO_WALLPAPER.get(),
                    new Item.Properties().setId(ITEMS.key("level_zero_wallpaper")))
    );

    /** Item pour le papier peint blanc du Level 0. */
    public static final RegistryObject<BlockItem> LEVEL_ZERO_WALLPAPER_AGED = ITEMS.register(
            "level_zero_wallpaper_aged",
            () -> new BlockItem(ModBlocks.LEVEL_ZERO_WALLPAPER_AGED.get(),
                    new Item.Properties().setId(ITEMS.key("level_zero_wallpaper_aged")))
    );

    /** Item de test pour le papier peint adaptatif interne du Level 0. */
    public static final RegistryObject<BlockItem> LEVEL_ZERO_WALLPAPER_ADAPTIVE = ITEMS.register(
            "level_zero_wallpaper_adaptive",
            () -> new BlockItem(ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE.get(),
                    new Item.Properties().setId(ITEMS.key("level_zero_wallpaper_adaptive")))
    );

    /** Item pour l'isolant interne des murs du Level 0. */
    public static final RegistryObject<BlockItem> LEVEL_ZERO_WALL_INSULATION = ITEMS.register(
            "level_zero_wall_insulation",
            () -> new BlockItem(ModBlocks.LEVEL_ZERO_WALL_INSULATION.get(),
                    new Item.Properties().setId(ITEMS.key("level_zero_wall_insulation")))
    );

    /** Item pour la moquette humide du Level 0. */
    public static final RegistryObject<BlockItem> LEVEL_ZERO_DAMP_CARPET = ITEMS.register(
            "level_zero_damp_carpet",
            () -> new BlockItem(ModBlocks.LEVEL_ZERO_DAMP_CARPET.get(),
                    new Item.Properties().setId(ITEMS.key("level_zero_damp_carpet")))
    );

    /** Item pour la moquette rouge du second biome du Level 0. */
    public static final RegistryObject<BlockItem> LEVEL_ZERO_DAMP_CARPET_AGED = ITEMS.register(
            "level_zero_damp_carpet_aged",
            () -> new BlockItem(ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED.get(),
                    new Item.Properties().setId(ITEMS.key("level_zero_damp_carpet_aged")))
    );

    /** Item pour la dalle de plafond du Level 0. */
    public static final RegistryObject<BlockItem> LEVEL_ZERO_CEILING_TILE = ITEMS.register(
            "level_zero_ceiling_tile",
            () -> new BlockItem(ModBlocks.LEVEL_ZERO_CEILING_TILE.get(),
                    new Item.Properties().setId(ITEMS.key("level_zero_ceiling_tile")))
    );

    /** Item pour le neon fluorescent du Level 0. */
    public static final RegistryObject<BlockItem> LEVEL_ZERO_FLUORESCENT_LIGHT = ITEMS.register(
            "level_zero_fluorescent_light",
            () -> new BlockItem(ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT.get(),
                    new Item.Properties().setId(ITEMS.key("level_zero_fluorescent_light")))
    );

    /**
     * Badge de la Gang, jeton officiel d'appartenance a la PetasseGang.
     * Taille de pile 1, rarete EPIC.
     */
    public static final RegistryObject<GangBadgeItem> GANG_BADGE = ITEMS.register(
            "gang_badge",
            () -> new GangBadgeItem(
                    new Item.Properties()
                            .setId(ITEMS.key("gang_badge"))
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)
            )
    );

    /**
     * Casse-croute Maudit, consommable qui retire 2 points de nourriture.
     * Toujours mangeable, meme le ventre plein.
     */
    public static final RegistryObject<CursedSnackItem> CURSED_SNACK = ITEMS.register(
            "cursed_snack",
            () -> new CursedSnackItem(
                    new Item.Properties()
                            .setId(ITEMS.key("cursed_snack"))
                            .stacksTo(16)
                            .food(new FoodProperties.Builder()
                                    .nutrition(0)
                                    .saturationModifier(0)
                                    .alwaysEdible()
                                    .build())
            )
    );

    /** Item pour le tronc de l'Arbre Maudit. */
    public static final RegistryObject<BlockItem> CURSED_LOG = ITEMS.register(
            "cursed_log",
            () -> new BlockItem(ModBlocks.CURSED_LOG.get(),
                    new Item.Properties().setId(ITEMS.key("cursed_log")))
    );

    /** Item pour les feuilles de l'Arbre Maudit. */
    public static final RegistryObject<BlockItem> CURSED_LEAVES = ITEMS.register(
            "cursed_leaves",
            () -> new BlockItem(ModBlocks.CURSED_LEAVES.get(),
                    new Item.Properties().setId(ITEMS.key("cursed_leaves")))
    );

    /** Item pour la pousse de l'Arbre Maudit. */
    public static final RegistryObject<BlockItem> CURSED_SAPLING = ITEMS.register(
            "cursed_sapling",
            () -> new BlockItem(ModBlocks.CURSED_SAPLING.get(),
                    new Item.Properties().setId(ITEMS.key("cursed_sapling")))
    );

    /** Item pour les planches de l'Arbre Maudit. */
    public static final RegistryObject<BlockItem> CURSED_PLANKS = ITEMS.register(
            "cursed_planks",
            () -> new BlockItem(ModBlocks.CURSED_PLANKS.get(),
                    new Item.Properties().setId(ITEMS.key("cursed_planks")))
    );

    /**
     * Enregistre le DeferredRegister sur le bus d'evenements du mod.
     *
     * @param modBusGroup le groupe de bus d'evenements du mod
     */
    public static void register(BusGroup modBusGroup) {
        ITEMS.register(modBusGroup);
    }

    private ModItems() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
