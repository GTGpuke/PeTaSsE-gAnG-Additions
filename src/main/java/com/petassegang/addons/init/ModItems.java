package com.petassegang.addons.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.petassegang.addons.item.GangBadgeItem;
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

    // ── Méthode d'enregistrement ──────────────────────────────────────────────

    /**
     * Enregistre le DeferredRegister sur le bus d'événements du mod.
     * Doit être appelé exactement une fois, depuis le constructeur de {@code PeTaSsEgAnGAdditionsMod}.
     */
    public static void register(BusGroup modBusGroup) {
        ITEMS.register(modBusGroup);
    }

    private ModItems() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
