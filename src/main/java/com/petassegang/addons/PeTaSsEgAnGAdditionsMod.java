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
import com.petassegang.addons.init.ModItems;
import com.petassegang.addons.util.ModConstants;

/**
 * Point d'entrée du mod PeTaSsE_gAnG_Additions.
 *
 * <p>Responsabilités :
 * <ol>
 *   <li>Enregistrer tous les DeferredRegisters sur le bus d'événements du mod.</li>
 *   <li>S'abonner aux événements de cycle de vie (commonSetup, clientSetup).</li>
 *   <li>Enregistrer les configurations Forge.</li>
 * </ol>
 *
 * <p>Ne pas ajouter de logique de jeu ici — déléguer aux classes appropriées.
 */
@Mod(ModConstants.MOD_ID)
public class PeTaSsEgAnGAdditionsMod {

    public PeTaSsEgAnGAdditionsMod(FMLJavaModLoadingContext context) {
        ModConstants.LOGGER.info("Initialisation de {} v{}", ModConstants.MOD_NAME,
                PeTaSsEgAnGAdditionsMod.class.getPackage().getImplementationVersion());

        BusGroup modBusGroup = context.getModBusGroup();

        // Enregistrement des DeferredRegisters des items et de l'onglet créatif.
        ModItems.register(modBusGroup);
        ModCreativeTab.register(modBusGroup);

        // Abonnement aux événements de cycle de vie.
        FMLCommonSetupEvent.getBus(modBusGroup).addListener(this::commonSetup);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLClientSetupEvent.getBus(modBusGroup).addListener(this::clientSetup);
        }

        // Enregistrement des configurations serveur et client.
        context.registerConfig(Type.SERVER, ModConfig.SERVER_SPEC);
        context.registerConfig(Type.CLIENT, ModConfig.CLIENT_SPEC);
    }

    /**
     * Phase de configuration commune (client + serveur).
     * S'exécute après les événements de registre, avant le chargement du monde.
     */
    private void commonSetup(FMLCommonSetupEvent event) {
        ModConstants.LOGGER.debug("Configuration commune terminée pour {}.", ModConstants.MOD_ID);
    }

    /**
     * Phase de configuration client uniquement.
     * Utiliser pour enregistrer les renderers, raccourcis clavier, écrans, etc.
     */
    private void clientSetup(FMLClientSetupEvent event) {
        ModConstants.LOGGER.debug("Configuration client terminée pour {}.", ModConstants.MOD_ID);
    }
}
