package com.petassegang.addons.perf.section.client;

import net.minecraft.client.MinecraftClient;

import com.petassegang.addons.perf.section.ModPerformanceMonitor;

/**
 * Pont client vers le moniteur de performance.
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
        if (client == null) {
            return;
        }
        ModPerformanceMonitor.onClientTick(client.getCurrentFps());
    }
}
