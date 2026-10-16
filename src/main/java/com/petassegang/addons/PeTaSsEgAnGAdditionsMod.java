package com.petassegang.addons;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import com.petassegang.addons.backrooms.level0.generation.layout.LevelZeroLayout;
import com.petassegang.addons.backrooms.portal.block.BackroomsDoorBlockEntity;
import com.petassegang.addons.backrooms.portal.debug.DebugPortalCommand;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntityRegistry;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntityType;
import com.petassegang.addons.backrooms.portal.interaction.RemoteInteractionHandler;
import com.petassegang.addons.backrooms.portal.sync.PortalViewTracker;
import com.petassegang.addons.backrooms.portal.transition.DestinationLoader;
import com.petassegang.addons.backrooms.portal.transition.TeleportExecutor;
import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.init.ModBlockEntities;
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.init.ModChunkGenerators;
import com.petassegang.addons.init.ModCreativeTab;
import com.petassegang.addons.init.ModItems;
import com.petassegang.addons.init.ModNetworking;
import com.petassegang.addons.perf.ModPerformanceMonitor;

/**
 * Point d'entree commun (client et serveur) du mod.
 */
public class PeTaSsEgAnGAdditionsMod implements ModInitializer {

    @Override
    public void onInitialize() {
        String version = FabricLoader.getInstance().getModContainer(ModConstants.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
        ModConstants.LOGGER.info("Initialisation de {} v{}.", ModConstants.MOD_NAME, version);

        registerContent();
        registerServerHooks();
        initPortals(ModConfig.ENABLE_PORTAL_FEATURE);

        ModConstants.LOGGER.info("Initialisation de {} terminee.", ModConstants.MOD_NAME);
    }

    /** Enregistre tout le contenu : blocs avant items, block entities apres blocs. */
    public static void registerContent() {
        ModChunkGenerators.register();
        ModBlocks.register();
        ModBlockEntities.register();
        BackroomsPortalEntityType.register();
        ModItems.register();
        ModCreativeTab.register();
        ModNetworking.register();
    }

    private static void registerServerHooks() {
        ServerTickEvents.START_SERVER_TICK.register(server -> ModPerformanceMonitor.onServerTickStart());
        ServerTickEvents.END_SERVER_TICK.register(server -> ModPerformanceMonitor.onServerTickEnd());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> LevelZeroLayout.clearCache());
    }

    /** Portails actives ou non par configuration ; sans portails, la porte reste decorative. */
    static void initPortals(boolean enabled) {
        BackroomsDoorBlockEntity.setPortalsEnabled(enabled);
        if (enabled) {
            initPortalRuntime();
        } else {
            ModConstants.LOGGER.info("[Portal] Portails desactives par configuration : la porte reste decorative.");
        }
    }

    /** Portails : index des portails charges, prechargement, synchronisation client, teleportation. */
    private static void initPortalRuntime() {
        BackroomsPortalEntityRegistry.init();
        DestinationLoader.init();
        PortalViewTracker.init();
        TeleportExecutor.init();
        RemoteInteractionHandler.init();
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            DebugPortalCommand.register();
            ModConstants.LOGGER.info("[Portal] Commande /backrooms portal enregistree (developpement).");
        }
        ModConstants.LOGGER.info("[Portal] Portails actifs.");
    }
}
