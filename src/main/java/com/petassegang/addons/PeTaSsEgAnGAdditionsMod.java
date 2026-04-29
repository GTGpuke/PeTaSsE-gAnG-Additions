package com.petassegang.addons;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import com.petassegang.addons.perf.section.ModPerformanceMonitor;
import com.petassegang.addons.creative.ModCreativeTab;
import com.petassegang.addons.init.ModBlockEntities;
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.init.ModChunkGenerators;
import com.petassegang.addons.init.ModItems;
import com.petassegang.addons.network.ModNetworking;
import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroLayout;

/**
 * Point d'entree commun du mod PeTaSsE_gAnG_Additions (Fabric).
 *
 * <p>Responsabilites :
 * <ol>
 *   <li>Enregistrer tous les blocs, items, block entities, chunk generators.</li>
 *   <li>Enregistrer l'onglet creatif et le reseau.</li>
 * </ol>
 */
public class PeTaSsEgAnGAdditionsMod implements ModInitializer {

    @Override
    public void onInitialize() {
        ModConstants.LOGGER.info("Initialisation de {} v{}.", ModConstants.MOD_NAME, ModConstants.MOD_ID);

        // Ordre important : blocs avant items, block entities apres blocs
        ModChunkGenerators.register();
        ModBlocks.register();
        ModBlockEntities.register();
        ModItems.register();
        ModCreativeTab.register();
        ModNetworking.register();
        ServerTickEvents.START_SERVER_TICK.register(server -> ModPerformanceMonitor.onServerTickStart());
        ServerTickEvents.END_SERVER_TICK.register(server -> ModPerformanceMonitor.onServerTickEnd());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LevelZeroLayout.clearCache());

        ModConstants.LOGGER.info("Initialisation de {} terminee.", ModConstants.MOD_NAME);
    }
}
