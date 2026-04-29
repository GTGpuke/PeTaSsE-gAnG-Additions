package com.petassegang.addons.feature.gang.client;

import net.minecraft.client.MinecraftClient;

/**
 * Handler client-side pour les packets du Gang Badge.
 *
 * <p>Cette classe est dans le package {@code client} pour garantir qu'aucun
 * import Minecraft-client n'est charge sur un serveur dedie. Elle n'est
 * instanciee que sur le client via la resolution paresseuse des classes JVM.
 */
public final class GangBadgeClientHandler {

    /**
     * Declenche l'animation d'activation de l'item avec la texture du badge.
     * Appele uniquement cote client via {@link com.petassegang.addons.feature.gang.network.c2s.GangBadgeActivatePayload}.
     */
    public static void handle() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.gameRenderer.showFloatingItem(client.player.getMainHandStack());
        }
    }

    private GangBadgeClientHandler() {
        throw new UnsupportedOperationException("Classe utilitaire client.");
    }
}
