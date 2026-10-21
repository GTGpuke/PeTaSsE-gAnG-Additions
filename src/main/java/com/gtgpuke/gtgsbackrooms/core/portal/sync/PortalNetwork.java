package com.gtgpuke.gtgsbackrooms.core.portal.sync;

import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.Unpooled;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.gtgpuke.gtgsbackrooms.core.perf.ModPerformanceMonitor;

/**
 * Envoi des payloads des portails a un joueur. Le moniteur de performance actif, chaque payload est encode une fois de
 * plus avant de partir, pour compter ce que les portails coutent au reseau, par type ; inactif, l'envoi est celui de
 * Fabric, sans rien de plus.
 */
public final class PortalNetwork {

    /** Codecs des payloads serveur vers client des portails, pour mesurer la taille de ce qui part. */
    private static final Map<Identifier, PacketCodec<? super RegistryByteBuf, CustomPayload>> CODECS = new HashMap<>();

    private PortalNetwork() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Enregistre un payload serveur vers client aupres de Fabric, et garde son codec pour la mesure.
     *
     * @param id identifiant du payload
     * @param codec son codec
     * @param <T> type du payload
     */
    @SuppressWarnings("unchecked")
    public static <T extends CustomPayload> void registerS2C(CustomPayload.Id<T> id, PacketCodec<? super RegistryByteBuf, T> codec) {
        PayloadTypeRegistry.playS2C().register(id, codec);
        CODECS.put(id.id(), (PacketCodec<? super RegistryByteBuf, CustomPayload>) codec);
    }

    /**
     * Envoie un payload au joueur ; moniteur actif, sa taille encodee est comptee d'abord.
     *
     * @param player joueur
     * @param payload payload a envoyer
     */
    public static void send(ServerPlayerEntity player, CustomPayload payload) {
        if (ModPerformanceMonitor.isEnabled()) {
            ModPerformanceMonitor.onPayloadSent(payload.getId().id().getPath(), sizeOf(player.getRegistryManager(), payload));
        }
        ServerPlayNetworking.send(player, payload);
    }

    /**
     * Taille encodee d'un payload, en octets, zero pour un payload dont le codec n'est pas enregistre ici. Alloue un
     * tampon : reserve a la mesure, moniteur actif.
     *
     * @param registries registres de la connexion, dont certains codecs ont besoin
     * @param payload payload a mesurer
     * @return taille en octets
     */
    static int sizeOf(DynamicRegistryManager registries, CustomPayload payload) {
        PacketCodec<? super RegistryByteBuf, CustomPayload> codec = CODECS.get(payload.getId().id());
        if (codec == null) {
            return 0;
        }
        RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), registries);
        try {
            codec.encode(buf, payload);
            return buf.readableBytes();
        } finally {
            buf.release();
        }
    }
}
