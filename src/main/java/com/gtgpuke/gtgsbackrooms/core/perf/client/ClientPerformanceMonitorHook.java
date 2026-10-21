package com.gtgpuke.gtgsbackrooms.core.perf.client;

import net.minecraft.client.MinecraftClient;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import com.gtgpuke.gtgsbackrooms.core.perf.ModPerformanceMonitor;
import com.gtgpuke.gtgsbackrooms.core.portal.client.view.PortalRenderState;

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
     * Une image du monde vient d'etre rendue : alimente la mesure des traversees. Le rendu complet d'une vue de portail
     * sous un pack de shaders finit aussi {@code WorldRenderer.render} : ce n'est pas une image.
     *
     * @param context contexte de rendu, non lu
     */
    public static void onFrameRendered(WorldRenderContext context) {
        if (!PortalRenderState.isRendering()) {
            ModPerformanceMonitor.onFrameRendered();
        }
    }
}
