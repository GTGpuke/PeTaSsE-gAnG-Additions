package com.gtgpuke.gtgsbackrooms.core.portal.sync;

import io.netty.buffer.ByteBuf;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;

/**
 * Serveur vers client : un bloc a change dans un chunk distant que ce joueur voit a travers un portail.
 *
 * @param dimension index de la dimension distante chez le joueur, celui de son annonce
 * @param pos position du bloc
 * @param state nouvel etat du bloc
 */
public record RemoteBlockUpdatePayload(int dimension, BlockPos pos, BlockState state) implements CustomPayload {

    public static final CustomPayload.Id<RemoteBlockUpdatePayload> ID =
            new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "portal_remote_block_update"));

    public static final PacketCodec<ByteBuf, RemoteBlockUpdatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT, RemoteBlockUpdatePayload::dimension,
            BlockPos.PACKET_CODEC, RemoteBlockUpdatePayload::pos,
            PacketCodecs.entryOf(Block.STATE_IDS), RemoteBlockUpdatePayload::state,
            RemoteBlockUpdatePayload::new);

    @Override
    public CustomPayload.Id<RemoteBlockUpdatePayload> getId() {
        return ID;
    }
}
