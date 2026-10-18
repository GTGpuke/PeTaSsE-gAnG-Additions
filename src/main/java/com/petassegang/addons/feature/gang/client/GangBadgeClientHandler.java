package com.petassegang.addons.feature.gang.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

import com.petassegang.addons.init.ModItems;

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
        ClientPlayerEntity player = client.player;
        if (player != null) {
            // Le badge sert de l'une ou l'autre main : l'animation montre celui qui est tenu.
            ItemStack main = player.getMainHandStack();
            client.gameRenderer.showFloatingItem(main.isOf(ModItems.GANG_BADGE) ? main : player.getOffHandStack());
        }
    }
}
