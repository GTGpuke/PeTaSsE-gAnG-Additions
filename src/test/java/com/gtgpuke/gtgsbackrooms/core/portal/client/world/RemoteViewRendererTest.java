package com.gtgpuke.gtgsbackrooms.core.portal.client.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;

@DisplayName("Ancrage de la grille de sections d'une vue")
class RemoteViewRendererTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("La grille garde son centre tant que l'ancrage reste a deux chunks pres, puis saute sur lui")
    void gridKeepsItsCenterWhileTheAnchorStaysClose() {
        int margin = RemoteViewRenderer.GRID_MARGIN_CHUNKS;
        assertEquals(10, RemoteViewRenderer.gridCenter(10, 10), "Ancrage au centre.");
        assertEquals(10, RemoteViewRenderer.gridCenter(10, 10 + margin), "Ancrage a la marge : on ne bouge pas.");
        assertEquals(10, RemoteViewRenderer.gridCenter(10, 10 - margin), "Idem de l'autre cote.");
        assertEquals(10 + margin + 1, RemoteViewRenderer.gridCenter(10, 10 + margin + 1), "Au-dela de la marge : la grille suit l'ancrage.");
        assertEquals(10 - margin - 1, RemoteViewRenderer.gridCenter(10, 10 - margin - 1), "Idem de l'autre cote.");
    }

    @Test
    @DisplayName("Une grille jamais centree se centre sur l'ancrage")
    void firstViewCentersOnTheAnchor() {
        assertEquals(42, RemoteViewRenderer.gridCenter(Integer.MIN_VALUE, 42), "Vanilla marque une grille vierge par la plus petite valeur.");
        assertEquals(-7, RemoteViewRenderer.gridCenter(Integer.MIN_VALUE, -7), "Y compris en coordonnees negatives.");
        assertEquals(0, RemoteViewRenderer.gridCenter(Integer.MIN_VALUE, 0), "Et sur l'origine, ou l'ecart a la valeur vierge deborderait.");
    }
}
