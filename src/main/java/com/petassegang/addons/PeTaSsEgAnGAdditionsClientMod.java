package com.petassegang.addons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

import com.petassegang.addons.backrooms.level0.client.model.LevelZeroWallpaperModelHandler;
import com.petassegang.addons.backrooms.portal.client.entity.BackroomsPortalEntityRenderer;
import com.petassegang.addons.backrooms.portal.client.interaction.RemoteActions;
import com.petassegang.addons.backrooms.portal.client.interaction.RemoteTargeting;
import com.petassegang.addons.backrooms.portal.client.transition.ClientPortalCrossing;
import com.petassegang.addons.backrooms.portal.client.transition.EntityHandover;
import com.petassegang.addons.backrooms.portal.client.world.ClientPortalIndex;
import com.petassegang.addons.backrooms.portal.client.world.ClientPortalLight;
import com.petassegang.addons.backrooms.portal.client.world.RemoteSyncReceiver;
import com.petassegang.addons.backrooms.portal.client.world.RemoteWorldRegistry;
import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.feature.gang.GangBadgeActivatePayload;
import com.petassegang.addons.feature.gang.client.GangBadgeClientHandler;
import com.petassegang.addons.init.ModEntities;
import com.petassegang.addons.perf.client.ClientPerformanceMonitorHook;

/**
 * Point d'entree client du mod : rendu adaptatif du Level 0, reception des payloads, portails.
 */
public class PeTaSsEgAnGAdditionsClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModConstants.LOGGER.debug("Initialisation client de {}.", ModConstants.MOD_NAME);

        // Les recepteurs Fabric tournent deja sur le thread de rendu : le badge s'applique a reception.
        ClientPlayNetworking.registerGlobalReceiver(GangBadgeActivatePayload.ID, (payload, context) -> GangBadgeClientHandler.handle());
        ClientTickEvents.END_CLIENT_TICK.register(ClientPerformanceMonitorHook::onEndClientTick);
        WorldRenderEvents.END.register(ClientPerformanceMonitorHook::onFrameRendered);
        LevelZeroWallpaperModelHandler.register();

        // Le type d'entity portail est toujours enregistre : son renderer et ses payloads doivent l'etre aussi.
        EntityRendererRegistry.register(ModEntities.BACKROOMS_PORTAL, BackroomsPortalEntityRenderer::new);
        RemoteSyncReceiver.init();
        ClientPortalIndex.init();
        ClientPortalCrossing.init();
        EntityHandover.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) {
                endPortalSession();
                return;
            }
            ClientPortalCrossing.onClientTick();
            RemoteActions.onClientTick();
            RemoteWorldRegistry.tickRemoteWorlds(client);
            // Apres le tick de tous les mondes : une arrivee se juge sur la premiere image a venir, partie d'avant ce tick.
            EntityHandover.onClientTick();
            ClientPortalLight.onClientTick(client);
        });
    }

    /**
     * Hors partie, les portails oublient tout : l'evenement de deconnexion de Fabric peut tourner sur le thread reseau,
     * or les mondes distants liberent des ressources de rendu. La fin de partie se traite donc ici, sur le thread de
     * rendu, une fois le monde quitte ; sans rien a oublier, ces remises a zero ne coutent rien.
     */
    private static void endPortalSession() {
        ClientPortalCrossing.clear();
        EntityHandover.clear();
        RemoteActions.clear();
        RemoteTargeting.clear();
        RemoteWorldRegistry.syncWithMainWorld(null);
        ClientPortalIndex.clear();
    }
}
