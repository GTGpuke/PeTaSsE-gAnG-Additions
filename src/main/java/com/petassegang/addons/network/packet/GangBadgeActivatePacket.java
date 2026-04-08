package com.petassegang.addons.network.packet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.NetworkProtocol;
import net.minecraftforge.network.SimpleChannel;

import com.petassegang.addons.client.handler.GangBadgeClientHandler;

/**
 * Packet serveur → client : déclenche l'animation d'activation du badge
 * (style totem) avec la texture de l'item tenu en main, non celle du totem vanilla.
 *
 * <p>Le handler délègue à {@link GangBadgeClientHandler} qui est dans le
 * package {@code client}. La JVM charge cette classe de façon paresseuse
 * (au premier appel), qui n'a lieu qu'une fois le client initialisé.
 * Le packet étant exclusivement {@code CLIENTBOUND}, le handler n'est
 * jamais invoqué côté serveur dédié.
 */
public record GangBadgeActivatePacket() {

    /** Codec sans données — le packet ne transporte aucune information. */
    public static final StreamCodec<RegistryFriendlyByteBuf, GangBadgeActivatePacket> STREAM_CODEC =
            StreamCodec.unit(new GangBadgeActivatePacket());

    /**
     * Gère le packet côté client : délègue à {@link GangBadgeClientHandler#handle()}.
     *
     * @param msg le packet reçu
     * @param ctx le contexte réseau Forge
     */
    public static void handle(GangBadgeActivatePacket msg, CustomPayloadEvent.Context ctx) {
        GangBadgeClientHandler.handle();
    }

    /**
     * Enregistre ce packet sur le canal réseau du mod via l'API {@code protocol()}.
     *
     * @param channel le canal SimpleChannel du mod
     */
    public static void register(SimpleChannel channel) {
        channel.protocol(NetworkProtocol.PLAY)
                .clientbound()
                .addMain(GangBadgeActivatePacket.class,
                        GangBadgeActivatePacket.STREAM_CODEC,
                        GangBadgeActivatePacket::handle)
                .build();
    }
}
