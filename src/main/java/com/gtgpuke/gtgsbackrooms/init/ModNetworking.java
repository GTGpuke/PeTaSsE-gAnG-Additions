package com.gtgpuke.gtgsbackrooms.init;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.PortalBlockActionPayload;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.PortalCrossPayload;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.PortalEntityActionPayload;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.PortalSightPayload;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.PortalTeleportPayload;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.RemoteBlockBreakingPayload;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.RemoteBlockUpdatePayload;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.RemoteChunkPayload;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.RemoteChunkUnloadPayload;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.RemoteDimensionPayload;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.RemoteEntityPacketPayload;
import com.gtgpuke.gtgsbackrooms.core.ModConstants;
import com.gtgpuke.gtgsbackrooms.feature.gang.GangBadgeActivatePayload;

/**
 * Enregistrement des payloads reseau du mod (cote commun) : badge, synchronisation, traversee et interaction a travers les portails.
 */
public final class ModNetworking {

    private ModNetworking() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Enregistre les types de payloads ; a appeler une fois depuis le point d'entree commun. */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(GangBadgeActivatePayload.ID, GangBadgeActivatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoteDimensionPayload.ID, RemoteDimensionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoteChunkPayload.ID, RemoteChunkPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoteChunkUnloadPayload.ID, RemoteChunkUnloadPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoteBlockUpdatePayload.ID, RemoteBlockUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoteEntityPacketPayload.ID, RemoteEntityPacketPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoteBlockBreakingPayload.ID, RemoteBlockBreakingPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PortalTeleportPayload.ID, PortalTeleportPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PortalCrossPayload.ID, PortalCrossPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PortalEntityActionPayload.ID, PortalEntityActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PortalBlockActionPayload.ID, PortalBlockActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PortalSightPayload.ID, PortalSightPayload.CODEC);
        ModConstants.LOGGER.debug("Payloads reseau enregistres.");
    }
}
