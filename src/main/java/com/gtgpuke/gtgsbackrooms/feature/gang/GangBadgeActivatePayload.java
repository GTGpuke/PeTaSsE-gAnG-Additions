package com.gtgpuke.gtgsbackrooms.feature.gang;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;

/**
 * Payload serveur vers client, sans donnees : declenche l'animation d'activation du badge.
 */
public record GangBadgeActivatePayload() implements CustomPayload {

    /** Instance unique : le payload ne transporte rien. */
    public static final GangBadgeActivatePayload INSTANCE = new GangBadgeActivatePayload();

    /** Identifiant reseau du payload. */
    public static final CustomPayload.Id<GangBadgeActivatePayload> ID =
            new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "gang_badge_activate"));

    /** Codec vide. */
    public static final PacketCodec<PacketByteBuf, GangBadgeActivatePayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public CustomPayload.Id<GangBadgeActivatePayload> getId() {
        return ID;
    }
}
