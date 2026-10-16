package com.petassegang.addons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import com.petassegang.addons.backrooms.level0.client.model.LevelZeroWallpaperModelHandler;
import com.petassegang.addons.backrooms.portal.client.entity.BackroomsPortalEntityRenderer;
import com.petassegang.addons.backrooms.portal.client.interaction.CrossPortalActions;
import com.petassegang.addons.backrooms.portal.client.interaction.CrossPortalTargeting;
import com.petassegang.addons.backrooms.portal.client.transition.ClientPortalCrossing;
import com.petassegang.addons.backrooms.portal.client.world.ClientPortalIndex;
import com.petassegang.addons.backrooms.portal.client.world.RemoteChunkReceiver;
import com.petassegang.addons.backrooms.portal.client.world.RemoteWorldRegistry;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntityType;
import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.feature.gang.GangBadgeActivatePayload;
import com.petassegang.addons.feature.gang.client.GangBadgeClientHandler;
import com.petassegang.addons.perf.client.ClientPerformanceMonitorHook;

/**
 * Point d'entree client du mod : rendu adaptatif du Level 0, reception des payloads, portails.
 */
public class PeTaSsEgAnGAdditionsClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModConstants.LOGGER.debug("Initialisation client de {}.", ModConstants.MOD_NAME);

        ClientPlayNetworking.registerGlobalReceiver(
                GangBadgeActivatePayload.ID,
                (payload, context) -> context.client().execute(GangBadgeClientHandler::handle));
        ClientTickEvents.END_CLIENT_TICK.register(ClientPerformanceMonitorHook::onEndClientTick);
        LevelZeroWallpaperModelHandler.register();

        // Le type d'entity portail est toujours enregistre : son renderer et ses payloads doivent l'etre aussi.
        EntityRendererRegistry.register(BackroomsPortalEntityType.INSTANCE, BackroomsPortalEntityRenderer::new);
        RemoteChunkReceiver.register();
        ClientPortalIndex.register();
        ClientPortalCrossing.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPortalCrossing.onClientTick();
            CrossPortalActions.onClientTick();
            RemoteWorldRegistry.tickRemoteWorlds(client);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientPortalCrossing.reset();
            CrossPortalActions.reset();
            CrossPortalTargeting.clear();
            RemoteWorldRegistry.dispose();
            ClientPortalIndex.clear();
        });
    }
}
