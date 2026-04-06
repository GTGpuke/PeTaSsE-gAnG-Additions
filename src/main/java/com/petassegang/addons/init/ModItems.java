package com.petassegang.addons.init;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.petassegang.addons.item.GangBadgeItem;
import com.petassegang.addons.util.ModConstants;

/**
 * Registre central des items pour PétasseGang Addons.
 *
 * <p>Tous les items doivent être déclarés ici en tant que champs {@link RegistryObject}
 * via {@link DeferredRegister}. Appeler {@link #register(IEventBus)} une seule fois
 * depuis le constructeur du mod principal.
 *
 * <p><b>Ajouter un nouvel item :</b> ajouter un champ {@code RegistryObject<Item>}
 * en suivant le pattern GANG_BADGE, puis l'ajouter à l'onglet créatif dans {@link com.petassegang.addons.creative.ModCreativeTab}.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ModConstants.MOD_ID);

    // ── Enregistrements des items ─────────────────────────────────────────────

    /**
     * Badge de la Gang — jeton officiel d'appartenance à la PétasseGang.
     * Taille de pile 1, rareté EPIC (brillance d'enchantement toujours visible).
     */
    public static final RegistryObject<Item> GANG_BADGE = ITEMS.register(
            "gang_badge",
            () -> new GangBadgeItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .rarity(Rarity.EPIC)
            )
    );

    // ── Méthode d'enregistrement ──────────────────────────────────────────────

    /**
     * Enregistre le DeferredRegister sur le bus d'événements du mod.
     * Doit être appelé exactement une fois, depuis le constructeur de {@code PetasseGangAddonsMod}.
     */
    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    private ModItems() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
