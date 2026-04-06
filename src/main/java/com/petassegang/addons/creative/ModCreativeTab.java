package com.petassegang.addons.creative;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.petassegang.addons.init.ModItems;
import com.petassegang.addons.util.ModConstants;

/**
 * Onglet créatif personnalisé pour PétasseGang Addons.
 *
 * <p>Tous les items du mod sont ajoutés ici via {@link #displayItems}.
 * Lors de l'ajout d'un nouvel item, l'ajouter dans le consumer {@code displayItems}.
 */
public final class ModCreativeTab {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModConstants.MOD_ID);

    /** L'onglet créatif principal de la PétasseGang. */
    public static final RegistryObject<CreativeModeTab> PETASSEGANG_TAB =
            CREATIVE_MODE_TABS.register("petassegang", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.petassegang_addons.petassegang"))
                            .icon(() -> new ItemStack(ModItems.GANG_BADGE.get()))
                            .displayItems(ModCreativeTab::displayItems)
                            .build()
            );

    /**
     * Peuple l'onglet créatif avec tous les items enregistrés.
     * Ajouter les nouveaux items ici dans l'ordre d'affichage souhaité.
     */
    private static void displayItems(CreativeModeTab.ItemDisplayParameters params,
                                     CreativeModeTab.Output output) {
        output.accept(ModItems.GANG_BADGE.get());
        // AJOUTER LES NOUVEAUX ITEMS EN DESSOUS DE CETTE LIGNE
    }

    /**
     * Enregistre le DeferredRegister sur le bus d'événements du mod.
     * Appelé une seule fois depuis le constructeur de {@code PetasseGangAddonsMod}.
     */
    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }

    private ModCreativeTab() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
