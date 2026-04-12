package com.petassegang.addons;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.creative.ModCreativeTab;
import com.petassegang.addons.client.model.LevelZeroWallpaperModelHandler;
import com.petassegang.addons.init.ModBlockEntities;
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.init.ModChunkGenerators;
import com.petassegang.addons.init.ModItems;
import com.petassegang.addons.network.ModNetworking;
import com.petassegang.addons.util.ModConstants;

/**
 * Point d'entree du mod PeTaSsE_gAnG_Additions.
 *
 * <p>Responsabilites :
 * <ol>
 *   <li>Enregistrer tous les DeferredRegisters sur le bus d'evenements du mod.</li>
 *   <li>S'abonner aux evenements de cycle de vie (commonSetup, clientSetup).</li>
 *   <li>Enregistrer les configurations Forge.</li>
 * </ol>
 *
 * <p>Ne pas ajouter de logique de jeu ici, deleguer aux classes appropriees.
 */
@Mod(ModConstants.MOD_ID)
public class PeTaSsEgAnGAdditionsMod {

    /**
     * Constructeur du mod, enregistre les DeferredRegisters et s'abonne aux evenements.
     *
     * @param context contexte de chargement du mod fourni par Forge
     */
    public PeTaSsEgAnGAdditionsMod(FMLJavaModLoadingContext context) {
        ModConstants.LOGGER.info("Initialisation de {} v{}", ModConstants.MOD_NAME,
                PeTaSsEgAnGAdditionsMod.class.getPackage().getImplementationVersion());

        BusGroup modBusGroup = context.getModBusGroup();

        ModChunkGenerators.register(modBusGroup);
        ModBlockEntities.register(modBusGroup);
        ModBlocks.register(modBusGroup);
        ModItems.register(modBusGroup);
        ModCreativeTab.register(modBusGroup);

        ModNetworking.register();

        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            LevelZeroWallpaperModelHandler.register();
            FMLClientSetupEvent.getBus(modBusGroup).addListener(this::clientSetup);
        }

        context.registerConfig(Type.SERVER, ModConfig.SERVER_SPEC);
        context.registerConfig(Type.CLIENT, ModConfig.CLIENT_SPEC);
    }

    /**
     * Phase de configuration commune.
     */
    private void commonSetup(FMLCommonSetupEvent event) {
        ModConstants.LOGGER.debug("Configuration commune terminee pour {}.", ModConstants.MOD_ID);
    }

    /**
     * Phase de configuration client uniquement.
     */
    private void clientSetup(FMLClientSetupEvent event) {
        ModConstants.LOGGER.debug("Configuration client terminee pour {}.", ModConstants.MOD_ID);
    }
}
