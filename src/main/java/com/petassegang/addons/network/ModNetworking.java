package com.petassegang.addons.network;

import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

import com.petassegang.addons.network.packet.GangBadgeActivatePacket;
import com.petassegang.addons.util.ModConstants;

/**
 * Gestionnaire du canal réseau du mod.
 *
 * <p>Utilise l'API {@link ChannelBuilder} de Forge pour créer un canal
 * {@code SimpleChannel}. Tous les packets sont enregistrés dans {@link #register()}.
 * Appeler {@link #register()} une seule fois depuis le constructeur du mod.
 */
public final class ModNetworking {

    /** Version du protocole réseau — incrémenter à chaque modification de l'API des packets. */
    private static final int PROTOCOL_VERSION = 1;

    /** Canal réseau principal du mod. */
    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ModConstants.MOD_ID + ":main")
            .networkProtocolVersion(PROTOCOL_VERSION)
            .optionalClient()
            .simpleChannel();

    /**
     * Enregistre tous les packets du mod sur le canal.
     * Doit être appelé exactement une fois, depuis le constructeur de {@code PeTaSsEgAnGAdditionsMod}.
     */
    public static void register() {
        GangBadgeActivatePacket.register(CHANNEL);
    }

    private ModNetworking() {
        throw new UnsupportedOperationException("Classe utilitaire réseau.");
    }
}
