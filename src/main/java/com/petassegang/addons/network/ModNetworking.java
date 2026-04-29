package com.petassegang.addons.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import com.petassegang.addons.feature.gang.network.c2s.GangBadgeActivatePayload;
import com.petassegang.addons.core.ModConstants;

/**
 * Gestionnaire du reseau du mod.
 *
 * <p>Utilise l'API Fabric Networking pour enregistrer les payloads.
 * Appeler {@link #register()} depuis le point d'entree commun du mod.
 */
public final class ModNetworking {

    /**
     * Enregistre les payloads S2C du mod.
     * Doit etre appele exactement une fois depuis {@code PeTaSsEgAnGAdditionsMod}.
     */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(
                GangBadgeActivatePayload.ID,
                GangBadgeActivatePayload.CODEC);
        ModConstants.LOGGER.debug("Payloads reseau enregistres.");
    }

    /**
     * Envoie le payload d'activation du badge a un joueur specifique.
     *
     * @param player le joueur destinataire
     * @param payload le payload a envoyer
     */
    public static void send(ServerPlayerEntity player, GangBadgeActivatePayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    private ModNetworking() {
        throw new UnsupportedOperationException("Classe utilitaire reseau.");
    }
}
