package com.petassegang.addons.backrooms.portal.client.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import net.minecraft.util.math.Vec3d;

/** L'etat est statique et la coupure du rendu distant est definitive pour la JVM : ce test passe en dernier. */
@DisplayName("Etat de rendu des portails")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PortalRenderStateTest {

    @AfterEach
    void endFrame() {
        PortalRenderState.endFrame();
        PortalRenderState.setBlockOutlineAllowed(false);
    }

    @Test
    @Order(1)
    @DisplayName("Une image de rendu pose puis efface la garde de reentrance et les cameras substituees")
    void frameLifecycle() {
        assertTrue(PortalRenderState.canRenderPortals(), "Rien ne s'oppose au rendu avant l'image.");
        PortalRenderState.beginFrame();
        assertFalse(PortalRenderState.canRenderPortals(), "Pas de rendu de portail dans un rendu de portail.");
        Vec3d override = new Vec3d(1.0, 2.0, 3.0);
        PortalRenderState.setCameraOverride(override);
        PortalRenderState.setViewCameraPos(override);
        PortalRenderState.setBlockOutlineAllowed(true);
        assertSame(override, PortalRenderState.cameraOverride(), "Camera substituee pour setupTerrain.");
        assertSame(override, PortalRenderState.viewCameraPos(), "Camera de la vue pour le ciel.");
        assertTrue(PortalRenderState.blockOutlineAllowed(), "Contour du bloc vise autorise.");
        PortalRenderState.endFrame();
        assertTrue(PortalRenderState.canRenderPortals(), "La garde est levee en fin d'image.");
        assertNull(PortalRenderState.cameraOverride(), "Plus de camera substituee.");
        assertNull(PortalRenderState.viewCameraPos(), "Plus de camera de vue.");
        assertTrue(PortalRenderState.blockOutlineAllowed(), "Le contour reste decide par le rendu principal.");
    }

    @Test
    @Order(2)
    @DisplayName("Les cameras des vues distantes et imbriquees sont reutilisees et distinctes")
    void camerasAreShared() {
        assertSame(PortalRenderState.remoteCamera(), PortalRenderState.remoteCamera(), "Une camera distante.");
        assertSame(PortalRenderState.nestedCamera(), PortalRenderState.nestedCamera(), "Une camera imbriquee.");
        assertNotSame(PortalRenderState.remoteCamera(), PortalRenderState.nestedCamera(), "Deux cameras differentes.");
    }

    @Test
    @Order(3)
    @DisplayName("Stencil refuse ou rendu en echec : le rendu distant est coupe pour la session, une seule fois")
    void failuresDisableRemoteRendering() {
        assertTrue(PortalRenderState.remoteRenderingAvailable(), "Disponible au depart.");
        PortalRenderState.disableRemoteRendering("Test sans cause.", null);
        assertFalse(PortalRenderState.remoteRenderingAvailable(), "Coupe apres un echec.");
        assertFalse(PortalRenderState.canRenderPortals(), "Plus de rendu de portail.");
        PortalRenderState.disableRemoteRendering("Test avec cause.", new IllegalStateException("Cause de test."));
        PortalRenderState.markStencilUnavailable(0);
        PortalRenderState.markStencilUnavailable(0);
        assertFalse(PortalRenderState.remoteRenderingAvailable(), "Toujours coupe, sans effet de bord.");
    }
}
