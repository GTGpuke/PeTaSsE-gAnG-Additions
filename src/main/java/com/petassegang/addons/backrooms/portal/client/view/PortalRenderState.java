package com.petassegang.addons.backrooms.portal.client.view;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.core.ModConstants;

/**
 * Etat partage du rendu des portails : garde de reentrance, camera distante substituee au joueur
 * dans {@code WorldRenderer.setupTerrain}, et disponibilite du rendu distant.
 *
 * <p>Si le stencil ne peut pas etre attache ou si un rendu distant leve une exception, le rendu
 * distant est coupe pour la session et les portails s'affichent comme des cadres sombres.
 */
public final class PortalRenderState {

    private static final Camera REMOTE_CAMERA = new Camera();
    private static final Camera NESTED_CAMERA = new Camera();
    private static boolean rendering;
    private static @Nullable Vec3d cameraOverride;
    private static @Nullable Vec3d viewCameraPos;
    private static boolean stencilAvailable = true;
    private static boolean remoteRenderingDisabled;
    private static boolean blockOutlineAllowed;

    private PortalRenderState() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** {@code true} si le rendu des portails peut avoir lieu dans cette frame. */
    public static boolean canRenderPortals() {
        return !rendering && remoteRenderingAvailable();
    }

    /** {@code true} tant que le stencil est disponible et qu'aucun rendu distant n'a echoue. */
    public static boolean remoteRenderingAvailable() {
        return stencilAvailable && !remoteRenderingDisabled;
    }

    public static void beginFrame() {
        rendering = true;
    }

    /** Vanilla dessine-t-il le contour du bloc vise dans cette frame (interface visible, pas spectateur) ? */
    public static boolean blockOutlineAllowed() {
        return blockOutlineAllowed;
    }

    public static void setBlockOutlineAllowed(boolean allowed) {
        blockOutlineAllowed = allowed;
    }

    public static void endFrame() {
        rendering = false;
        cameraOverride = null;
        viewCameraPos = null;
    }

    /** Camera reutilisee pour les vues distantes (position et rotation reecrites a chaque portail). */
    public static Camera remoteCamera() {
        return REMOTE_CAMERA;
    }

    /** Camera des vues imbriquees (un portail vu a travers un autre), reecrite a chaque portail imbrique. */
    public static Camera nestedCamera() {
        return NESTED_CAMERA;
    }

    /** Position qui remplace celle du joueur pendant {@code setupTerrain}, ou {@code null} hors rendu distant. */
    public static @Nullable Vec3d cameraOverride() {
        return cameraOverride;
    }

    public static void setCameraOverride(@Nullable Vec3d position) {
        cameraOverride = position;
    }

    /** Camera de la vue en cours de rendu, ou {@code null} : le ciel vanilla s'y rapporte au lieu du joueur. */
    public static @Nullable Vec3d viewCameraPos() {
        return viewCameraPos;
    }

    public static void setViewCameraPos(@Nullable Vec3d position) {
        viewCameraPos = position;
    }

    /** Appele par le mixin de framebuffer quand l'attachement depth-stencil est refuse par le pilote. */
    public static void markStencilUnavailable(int framebufferStatus) {
        if (stencilAvailable) {
            stencilAvailable = false;
            ModConstants.LOGGER.warn("[Portal] Stencil indisponible (statut framebuffer {}), les portails seront des cadres sombres.",
                    framebufferStatus);
        }
    }

    /** Coupe le rendu distant pour la session apres une exception, en la journalisant une seule fois. */
    public static void disableRemoteRendering(String reason, @Nullable Throwable cause) {
        if (!remoteRenderingDisabled) {
            remoteRenderingDisabled = true;
            // Sans cause, SLF4J ignore l'argument en trop : un seul appel pour les deux cas.
            ModConstants.LOGGER.error("[Portal] Rendu distant coupe pour la session : {}.", reason, cause);
        }
    }
}
