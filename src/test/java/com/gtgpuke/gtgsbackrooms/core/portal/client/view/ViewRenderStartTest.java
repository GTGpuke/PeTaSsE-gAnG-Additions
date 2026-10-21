package com.gtgpuke.gtgsbackrooms.core.portal.client.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;

@DisplayName("Debut de rendu a blanc d'un renderer de vue")
class ViewRenderStartTest {

    @AfterEach
    void reset() {
        ViewRenderStart.setStarting(false);
        ViewRenderStart.endFrame();
    }

    @Test
    @DisplayName("Les parametres du rendu du joueur ne valent que le temps de l'image, le drapeau que le temps du debut")
    void frameParametersAndFlag() {
        assertFalse(ViewRenderStart.hasFrame(), "Hors image : aucun debut de rendu possible.");
        assertFalse(ViewRenderStart.isStarting(), "Aucun debut de rendu en cours.");
        RenderTickCounter counter = RenderTickCounter.ZERO;
        Camera camera = new Camera();
        Matrix4f position = new Matrix4f();
        Matrix4f projection = new Matrix4f();
        ViewRenderStart.beginFrame(counter, camera, null, null, position, projection);
        assertTrue(ViewRenderStart.hasFrame(), "Pendant l'image : debut de rendu possible.");
        assertSame(counter, ViewRenderStart.tickCounter(), "Compteur de ticks du joueur.");
        assertSame(camera, ViewRenderStart.camera(), "Camera du joueur.");
        assertNull(ViewRenderStart.gameRenderer(), "Renderer de jeu tel que donne (non constructible en test : null).");
        assertNull(ViewRenderStart.lightmap(), "Lightmap telle que donnee (non constructible en test : null).");
        assertSame(position, ViewRenderStart.positionMatrix(), "Matrice de vue du joueur.");
        assertSame(projection, ViewRenderStart.projection(), "Projection du joueur.");
        ViewRenderStart.setStarting(true);
        assertTrue(ViewRenderStart.isStarting(), "Debut de rendu en cours : render s'arrete en tete.");
        ViewRenderStart.setStarting(false);
        assertFalse(ViewRenderStart.isStarting(), "Debut de rendu termine.");
        ViewRenderStart.endFrame();
        assertFalse(ViewRenderStart.hasFrame(), "Fin d'image : plus de parametres.");
        assertNull(ViewRenderStart.tickCounter(), "Compteur oublie.");
        assertNull(ViewRenderStart.camera(), "Camera oubliee.");
        assertNull(ViewRenderStart.positionMatrix(), "Matrice oubliee.");
        assertNull(ViewRenderStart.projection(), "Projection oubliee.");
    }
}
