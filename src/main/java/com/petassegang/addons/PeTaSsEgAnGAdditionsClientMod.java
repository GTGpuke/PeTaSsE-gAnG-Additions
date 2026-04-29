package com.petassegang.addons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.petassegang.addons.perf.section.client.ClientPerformanceMonitorHook;
import com.petassegang.addons.feature.gang.client.GangBadgeClientHandler;
import com.petassegang.addons.backrooms.level.level0.client.model.LevelZeroBaseboardModelHandler;
import com.petassegang.addons.backrooms.level.level0.client.model.LevelZeroWallpaperModelHandler;
import com.petassegang.addons.feature.gang.network.c2s.GangBadgeActivatePayload;
import com.petassegang.addons.core.ModConstants;

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
        ClientTickEvents.END_CLIENT_TICK.register(ClientPerformanceMonitorHook::onEndClientTick);

        // Modele adaptatif du papier peint Level 0
        LevelZeroWallpaperModelHandler.register();
        // Modele adaptatif des plinthes du Level 0
        LevelZeroBaseboardModelHandler.register();
    }
}
