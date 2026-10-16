package com.petassegang.addons.feature.gang.client;

import net.minecraft.client.MinecraftClient;

/**
 * Reception client du payload d'activation du badge : animation de totem avec l'item tenu.
 */
public final class GangBadgeClientHandler {

    private GangBadgeClientHandler() {
        throw new UnsupportedOperationException("Classe utilitaire client.");
    }

    /** Joue l'animation d'activation sur le client courant. */
    public static void handle() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.gameRenderer.showFloatingItem(client.player.getMainHandStack());
        }
    }
}
