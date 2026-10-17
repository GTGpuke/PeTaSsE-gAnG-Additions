package com.petassegang.addons.perf.client;

import net.minecraft.client.MinecraftClient;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import com.petassegang.addons.perf.ModPerformanceMonitor;

/**
 * Pont client vers le moniteur de performance : FPS en fin de tick client, images rendues pour la mesure des traversees.
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

    /**
     * Une image du monde vient d'etre rendue : alimente la mesure des traversees.
     *
     * @param context contexte de rendu, non lu
     */
    public static void onFrameRendered(WorldRenderContext context) {
        ModPerformanceMonitor.onFrameRendered();
    }
}
