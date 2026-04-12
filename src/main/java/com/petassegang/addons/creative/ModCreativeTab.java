package com.petassegang.addons.creative;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.petassegang.addons.init.ModItems;
import com.petassegang.addons.util.ModConstants;

/**
 * Onglet creatif personnalise pour PeTaSsE_gAnG_Additions.
 */
public final class ModCreativeTab {

    /** DeferredRegister pour les onglets creatifs du mod. */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ModConstants.MOD_ID);

    /** L'onglet creatif principal de la PetasseGang. */
    public static final RegistryObject<CreativeModeTab> PETASSEGANG_TAB =
            CREATIVE_MODE_TABS.register("petassegang", () ->
                    CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.petasse_gang_additions.petassegang"))
                            .icon(() -> new ItemStack(ModItems.GANG_BADGE.get()))
                            .displayItems(ModCreativeTab::displayItems)
                            .build()
            );

    /**
     * Peuple l'onglet creatif avec tous les items enregistres.
     */
    private static void displayItems(CreativeModeTab.ItemDisplayParameters params,
                                     CreativeModeTab.Output output) {
        output.accept(ModItems.GANG_BADGE.get());
        output.accept(ModItems.CURSED_SNACK.get());
        output.accept(ModItems.LEVEL_ZERO_WALLPAPER.get());
        output.accept(ModItems.LEVEL_ZERO_WALL_INSULATION.get());
        output.accept(ModItems.LEVEL_ZERO_DAMP_CARPET.get());
        output.accept(ModItems.LEVEL_ZERO_DAMP_CARPET_AGED.get());
        output.accept(ModItems.LEVEL_ZERO_CEILING_TILE.get());
        output.accept(ModItems.LEVEL_ZERO_FLUORESCENT_LIGHT.get());
        output.accept(ModItems.CURSED_LOG.get());
        output.accept(ModItems.CURSED_PLANKS.get());
        output.accept(ModItems.CURSED_LEAVES.get());
        output.accept(ModItems.CURSED_SAPLING.get());
    }

    /**
     * Enregistre le DeferredRegister sur le bus d'evenements du mod.
     *
     * @param modBusGroup le groupe de bus d'evenements du mod
     */
    public static void register(BusGroup modBusGroup) {
        CREATIVE_MODE_TABS.register(modBusGroup);
    }

    private ModCreativeTab() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
