package com.gtgpuke.gtgsbackrooms.backrooms.portal.sync;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionTypes;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;

/**
 * Serveur vers client : annonce une dimension distante avant d'en envoyer les chunks.
 *
 * <p>Le client cree alors un monde local pour cette dimension avec le bon type
 * de dimension (hauteur, ciel, lumiere ambiante).
 *
 * @param dimension identifiant de la dimension distante
 * @param dimensionType identifiant du type de dimension
 */
public record RemoteDimensionPayload(Identifier dimension, Identifier dimensionType) implements CustomPayload {

    public static final CustomPayload.Id<RemoteDimensionPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "portal_remote_dimension"));

    public static final PacketCodec<ByteBuf, RemoteDimensionPayload> CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC, RemoteDimensionPayload::dimension,
            Identifier.PACKET_CODEC, RemoteDimensionPayload::dimensionType,
            RemoteDimensionPayload::new);

    /** Annonce d'un monde serveur. */
    public static RemoteDimensionPayload of(ServerWorld world) {
        return new RemoteDimensionPayload(world.getRegistryKey().getValue(), dimensionTypeId(world));
    }

    /** Identifiant du type de dimension d'un monde ; l'overworld si le type n'est pas enregistre sous une cle. */
    public static Identifier dimensionTypeId(ServerWorld world) {
        return world.getDimensionEntry().getKey().map(RegistryKey::getValue).orElse(DimensionTypes.OVERWORLD.getValue());
    }

    @Override
    public CustomPayload.Id<RemoteDimensionPayload> getId() {
        return ID;
    }
}
