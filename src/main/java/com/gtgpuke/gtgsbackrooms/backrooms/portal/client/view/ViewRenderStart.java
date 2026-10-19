package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderTickCounter;

/**
 * Debut de rendu a blanc d'un renderer de vue. Des mods posent un etat par renderer en tete de
 * {@code WorldRenderer.render} (le pipeline d'un mod de shaders, le contexte des evenements de rendu du monde de Fabric)
 * et l'attendent dans les methodes de rendu que la vue appelle ensuite sur son renderer ({@code renderSky},
 * {@code renderLayer}...) ; un renderer de vue, qui ne passe jamais par {@code render}, les faisait echouer au premier
 * dessin. Avant de dessiner une vue, {@code RemoteViewRenderer} appelle donc {@code render} sur son renderer avec les
 * parametres du rendu du joueur, et {@code MixinWorldRenderStart} l'arrete en tete de methode, apres ces mods : leur
 * etat est pose, rien d'autre du rendu vanilla ne tourne, et ce qu'ils capturent (matrices, fraction de tick) reste
 * celui du joueur. Cette classe tient le drapeau et les parametres de l'image ; thread de rendu seulement.
 */
public final class ViewRenderStart {

    private static boolean starting;
    private static @Nullable RenderTickCounter tickCounter;
    private static @Nullable Camera camera;
    private static @Nullable GameRenderer gameRenderer;
    private static @Nullable LightmapTextureManager lightmap;
    private static @Nullable Matrix4f positionMatrix;
    private static @Nullable Matrix4f projection;

    private ViewRenderStart() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Parametres du rendu du monde du joueur, gardes le temps du rendu des portails de l'image.
     *
     * @param frameTickCounter compteur de ticks du rendu
     * @param frameCamera camera du joueur
     * @param frameGameRenderer renderer de jeu
     * @param frameLightmap gestionnaire de lightmap
     * @param framePositionMatrix matrice de vue du joueur
     * @param frameProjection projection du joueur
     */
    public static void beginFrame(
            RenderTickCounter frameTickCounter,
            Camera frameCamera,
            GameRenderer frameGameRenderer,
            LightmapTextureManager frameLightmap,
            Matrix4f framePositionMatrix,
            Matrix4f frameProjection) {
        tickCounter = frameTickCounter;
        camera = frameCamera;
        gameRenderer = frameGameRenderer;
        lightmap = frameLightmap;
        positionMatrix = framePositionMatrix;
        projection = frameProjection;
    }

    /** Fin du rendu des portails de l'image : plus de parametres, aucun debut de rendu possible. */
    public static void endFrame() {
        tickCounter = null;
        camera = null;
        gameRenderer = null;
        lightmap = null;
        positionMatrix = null;
        projection = null;
    }

    /** {@code true} entre {@link #beginFrame} et {@link #endFrame} : un debut de rendu a blanc est possible. */
    public static boolean hasFrame() {
        return tickCounter != null;
    }

    /** {@code true} pendant un debut de rendu a blanc : {@code MixinWorldRenderStart} arrete alors {@code render} en tete. */
    public static boolean isStarting() {
        return starting;
    }

    /** Pose ou leve le drapeau du debut de rendu a blanc ; a lever dans un {@code finally}. */
    public static void setStarting(boolean value) {
        starting = value;
    }

    /** Compteur de ticks du rendu du joueur ; nul hors image. */
    public static @Nullable RenderTickCounter tickCounter() {
        return tickCounter;
    }

    /** Camera du joueur ; nulle hors image. */
    public static @Nullable Camera camera() {
        return camera;
    }

    /** Renderer de jeu ; nul hors image. */
    public static @Nullable GameRenderer gameRenderer() {
        return gameRenderer;
    }

    /** Gestionnaire de lightmap ; nul hors image. */
    public static @Nullable LightmapTextureManager lightmap() {
        return lightmap;
    }

    /** Matrice de vue du joueur ; nulle hors image. */
    public static @Nullable Matrix4f positionMatrix() {
        return positionMatrix;
    }

    /** Projection du joueur ; nulle hors image. */
    public static @Nullable Matrix4f projection() {
        return projection;
    }
}
