package com.petassegang.addons.backrooms.portal.sync;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

import com.petassegang.addons.core.ModConstants;

/**
 * Serveur vers client : un chunk distant n'est plus visible par ce joueur, le client peut l'oublier.
 *
 * @param dimension identifiant de la dimension distante
 * @param chunkPos position du chunk
 */
public record RemoteChunkUnloadPayload(Identifier dimension, ChunkPos chunkPos) implements CustomPayload {

    public static final CustomPayload.Id<RemoteChunkUnloadPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "portal_remote_chunk_unload"));

    /** Un chunk tient dans un long ; le varlong reste compact pour les coordonnees proches de l'origine. */
    private static final PacketCodec<ByteBuf, ChunkPos> CHUNK_POS_CODEC = PacketCodecs.VAR_LONG.xmap(ChunkPos::new, ChunkPos::toLong);

    public static final PacketCodec<ByteBuf, RemoteChunkUnloadPayload> CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC, RemoteChunkUnloadPayload::dimension,
            CHUNK_POS_CODEC, RemoteChunkUnloadPayload::chunkPos,
            RemoteChunkUnloadPayload::new);

    @Override
    public CustomPayload.Id<RemoteChunkUnloadPayload> getId() {
        return ID;
    }
}
