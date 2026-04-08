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
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.util.ModConstants;

/**
 * Registre central des items pour PeTaSsE_gAnG_Additions.
 *
 * <p>Tous les items doivent être déclarés ici via {@link DeferredRegister}.
 * En MC 26.1, {@link Item.Properties#setId} doit être appelé avec la
 * {@link net.minecraft.resources.ResourceKey} de l'item avant sa construction.
 * Appeler {@link #register(BusGroup)} une seule fois depuis le constructeur
 * du mod principal.
 */
public final class ModItems {

    /** DeferredRegister central pour tous les items du mod. */
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ModConstants.MOD_ID);

    // ── Enregistrements des items ─────────────────────────────────────────────

    /**
     * Badge de la Gang — jeton officiel d'appartenance à la PétasseGang.
     * Taille de pile 1, rareté EPIC (brillance d'enchantement toujours visible).
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

    // ── Casse-croûte Maudit ───────────────────────────────────────────────────

    /**
     * Casse-croûte Maudit — consommable qui retire 2 points de nourriture.
     * Toujours mangeable, même le ventre plein.
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

    // ── Arbre Maudit (BlockItems) ─────────────────────────────────────────────

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

    // ── Méthode d'enregistrement ──────────────────────────────────────────────

    /**
     * Enregistre le DeferredRegister sur le bus d'événements du mod.
     * Doit être appelé exactement une fois, depuis le constructeur de {@code PeTaSsEgAnGAdditionsMod}.
     *
     * @param modBusGroup le groupe de bus d'événements du mod
     */
    public static void register(BusGroup modBusGroup) {
        ITEMS.register(modBusGroup);
    }

    private ModItems() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
