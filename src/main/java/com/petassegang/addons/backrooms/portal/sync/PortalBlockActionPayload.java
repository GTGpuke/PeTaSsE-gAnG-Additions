package com.petassegang.addons.backrooms.portal.sync;

import java.util.UUID;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;

import com.petassegang.addons.core.ModConstants;

/**
 * Client vers serveur : le joueur agit sur un bloc vise a travers un portail.
 *
 * @param portal portail a travers lequel le joueur vise
 * @param dimension dimension du bloc, celle que le client associe au portail
 * @param hit bloc, face et point touches par le regard, dans cette dimension
 * @param action debut ou abandon du minage, ou utilisation (main principale puis secondaire, comme vanilla)
 */
public record PortalBlockActionPayload(UUID portal, Identifier dimension, BlockHitResult hit, Action action) implements CustomPayload {

    /** Ce que le joueur fait du bloc. */
    public enum Action {
        START_MINING,
        ABORT_MINING,
        USE
    }

    public static final CustomPayload.Id<PortalBlockActionPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "portal_block_action"));

    public static final PacketCodec<PacketByteBuf, PortalBlockActionPayload> CODEC =
            PacketCodec.of(PortalBlockActionPayload::write, PortalBlockActionPayload::read);

    private void write(PacketByteBuf buf) {
        buf.writeUuid(this.portal);
        buf.writeIdentifier(this.dimension);
        buf.writeBlockHitResult(this.hit);
        buf.writeEnumConstant(this.action);
    }

    private static PortalBlockActionPayload read(PacketByteBuf buf) {
        return new PortalBlockActionPayload(buf.readUuid(), buf.readIdentifier(), buf.readBlockHitResult(), buf.readEnumConstant(Action.class));
    }

    @Override
    public CustomPayload.Id<PortalBlockActionPayload> getId() {
        return ID;
    }
}
