package com.petassegang.addons.network.packet;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import com.petassegang.addons.util.ModConstants;

/**
 * Payload serveur → client : declenche l'animation d'activation du badge
 * (style totem) avec la texture de l'item tenu en main.
 */
public record GangBadgeActivatePayload() implements CustomPayload {

    /** Identifiant unique du payload sur le reseau. */
    public static final CustomPayload.Id<GangBadgeActivatePayload> ID =
            new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "gang_badge_activate"));

    /** Codec sans donnees — le payload ne transporte aucune information. */
    public static final PacketCodec<PacketByteBuf, GangBadgeActivatePayload> CODEC =
            PacketCodec.unit(new GangBadgeActivatePayload());

    @Override
    public CustomPayload.Id<GangBadgeActivatePayload> getId() {
        return ID;
    }
}
