package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Objets GL differes des renderers de vue")
class LazyVertexBuffersTest {

    @AfterEach
    void reset() {
        LazyVertexBuffers.setActive(false);
    }

    @Test
    @DisplayName("Le drapeau ne vaut que le temps de la creation d'un renderer de vue")
    void flagFollowsActivation() {
        assertFalse(LazyVertexBuffers.active(), "Inactif par defaut : les tampons du monde du joueur reservent leurs objets GL d'emblee.");
        LazyVertexBuffers.setActive(true);
        assertTrue(LazyVertexBuffers.active(), "Actif pendant la creation d'un renderer de vue.");
        LazyVertexBuffers.setActive(false);
        assertFalse(LazyVertexBuffers.active(), "Rendu inactif ensuite.");
    }
}
