package com.petassegang.addons.perf.client;

import net.minecraft.client.MinecraftClient;

import com.petassegang.addons.perf.ModPerformanceMonitor;

/**
 * Pont client vers le moniteur de performance (FPS en fin de tick client).
 */
public final class ClientPerformanceMonitorHook {

    private ClientPerformanceMonitorHook() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Echantillonne l'etat client en fin de tick.
     *
     * @param client client courant
     */
    public static void onEndClientTick(MinecraftClient client) {
        ModPerformanceMonitor.onClientTick(client.getCurrentFps());
    }
}
