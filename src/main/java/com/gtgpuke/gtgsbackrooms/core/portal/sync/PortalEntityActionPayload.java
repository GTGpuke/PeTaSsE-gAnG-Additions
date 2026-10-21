package com.gtgpuke.gtgsbackrooms.core.portal.sync;

import java.util.UUID;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;

/**
 * Client vers serveur : le joueur frappe ou utilise une entity visee a travers un portail.
 *
 * @param portal portail a travers lequel le joueur vise
 * @param dimension dimension de la cible, celle que le client associe au portail
 * @param entityId identifiant reseau de la cible dans cette dimension
 * @param action frapper ou utiliser (main principale puis secondaire, comme vanilla)
 */
public record PortalEntityActionPayload(UUID portal, Identifier dimension, int entityId, Action action) implements CustomPayload {

    /** Ce que le joueur fait de l'entity. */
    public enum Action {
        ATTACK,
        INTERACT
    }

    public static final CustomPayload.Id<PortalEntityActionPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "portal_entity_action"));

    public static final PacketCodec<PacketByteBuf, PortalEntityActionPayload> CODEC =
            PacketCodec.of(PortalEntityActionPayload::write, PortalEntityActionPayload::read);

    private void write(PacketByteBuf buf) {
        buf.writeUuid(this.portal);
        buf.writeIdentifier(this.dimension);
        buf.writeVarInt(this.entityId);
        buf.writeEnumConstant(this.action);
    }

    private static PortalEntityActionPayload read(PacketByteBuf buf) {
        return new PortalEntityActionPayload(buf.readUuid(), buf.readIdentifier(), buf.readVarInt(), buf.readEnumConstant(Action.class));
    }

    @Override
    public CustomPayload.Id<PortalEntityActionPayload> getId() {
        return ID;
    }
}
