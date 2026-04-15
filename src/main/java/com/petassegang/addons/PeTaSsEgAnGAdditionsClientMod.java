package com.petassegang.addons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.petassegang.addons.client.handler.GangBadgeClientHandler;
import com.petassegang.addons.client.model.LevelZeroWallpaperModelHandler;
import com.petassegang.addons.network.packet.GangBadgeActivatePayload;
import com.petassegang.addons.util.ModConstants;

/**
 * Point d'entree client du mod PeTaSsE_gAnG_Additions.
 *
 * <p>Charge uniquement sur le client : rendu adaptatif du papier peint
 * et handler du packet Gang Badge.
 */
public class PeTaSsEgAnGAdditionsClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModConstants.LOGGER.debug("Initialisation client de {}.", ModConstants.MOD_NAME);

        // Handler reseau client : animation d'activation du badge
        ClientPlayNetworking.registerGlobalReceiver(
                GangBadgeActivatePayload.ID,
                (payload, context) -> context.client().execute(GangBadgeClientHandler::handle));

        // Modele adaptatif du papier peint Level 0
        LevelZeroWallpaperModelHandler.register();
    }
}
