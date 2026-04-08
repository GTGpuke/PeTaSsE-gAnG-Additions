package com.petassegang.addons.client.handler;

import net.minecraft.client.Minecraft;

/**
 * Handler client-side pour les packets du Gang Badge.
 *
 * <p>Cette classe est dans le package {@code client} pour garantir qu'aucun
 * import Minecraft-client n'est chargé sur un serveur dédié. Elle n'est
 * instanciée que sur le client via la résolution paresseuse des classes JVM.
 */
public final class GangBadgeClientHandler {

    /**
     * Déclenche l'animation d'activation de l'item avec la texture du badge.
     * Appelé uniquement côté client via {@link com.petassegang.addons.network.packet.GangBadgeActivatePacket}.
     */
    public static void handle() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gameRenderer.displayItemActivation(mc.player.getMainHandItem());
        }
    }

    private GangBadgeClientHandler() {
        throw new UnsupportedOperationException("Classe utilitaire client.");
    }
}
