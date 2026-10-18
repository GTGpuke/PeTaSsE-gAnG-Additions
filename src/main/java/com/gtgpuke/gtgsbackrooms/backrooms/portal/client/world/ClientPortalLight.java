package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.light.PortalLightTransmission;
import com.gtgpuke.gtgsbackrooms.config.ModConfig;

/**
 * Lumiere transmise par les portails, cote client : le moteur de lumiere du client recalcule seul les blocs qui changent,
 * dans son monde comme dans les mondes vus par les portails. Le meme calcul que le serveur tourne donc pour chacun, avec
 * les portails et les lumieres que le client connait.
 */
public final class ClientPortalLight {

    /** Section propre au client : en solo, la passe du serveur se compte a part. */
    private static final String PERF_LIGHT = "portal.light.client";
    private static final PortalLightTransmission TRANSMISSION = new PortalLightTransmission(PERF_LIGHT, (source, key) -> RemoteWorldRegistry.worldOf(key));
    private static int ticks;

    private ClientPortalLight() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Tick client, un monde principal charge et les portails actives : une passe tous les
     * {@link PortalLightTransmission#UPDATE_INTERVAL_TICKS} ticks.
     */
    public static void onClientTick(MinecraftClient client) {
        if (!ModConfig.ENABLE_PORTAL_FEATURE || ++ticks % PortalLightTransmission.UPDATE_INTERVAL_TICKS != 0) {
            return;
        }
        ClientWorld main = client.world;
        TRANSMISSION.update(main, ClientPortalIndex.portals(main));
        for (int i = 0; i < RemoteWorldRegistry.remoteWorldCount(); i++) {
            ClientWorld remote = RemoteWorldRegistry.remoteWorld(i);
            TRANSMISSION.update(remote, ClientPortalIndex.portals(remote));
        }
    }
}
