package com.gtgpuke.gtgsbackrooms.core.portal.sync;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalSettings;

/**
 * Client vers serveur : les reglages choisis dans l'ecran de la baguette de portail, pour la baguette tenue dans cette main.
 * Les reglages voyagent au format du composant ({@link PortalSettings#PACKET_CODEC}) : un champ hors bornes est refuse.
 *
 * @param hand main qui tient la baguette
 * @param settings reglages choisis
 */
public record PortalWandSettingsPayload(Hand hand, PortalSettings settings) implements CustomPayload {

    public static final CustomPayload.Id<PortalWandSettingsPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "portal_wand_settings"));

    /** Main sur un octet (principale ou non), puis les reglages. */
    public static final PacketCodec<ByteBuf, PortalWandSettingsPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL.xmap(main -> main ? Hand.MAIN_HAND : Hand.OFF_HAND, hand -> hand == Hand.MAIN_HAND),
            PortalWandSettingsPayload::hand,
            PortalSettings.PACKET_CODEC,
            PortalWandSettingsPayload::settings,
            PortalWandSettingsPayload::new);

    @Override
    public CustomPayload.Id<PortalWandSettingsPayload> getId() {
        return ID;
    }
}
