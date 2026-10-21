package com.gtgpuke.gtgsbackrooms.core.portal.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;

@DisplayName("Vue des entities distantes")
class RemoteEntityViewTest {

    private static final int BOTTOM_Y = 0;
    private static final int TOP_Y = 256;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("La boite de la fenetre couvre ses chunks sur toute la hauteur et n'est refaite que si une borne bouge")
    void windowBoxIsRebuiltOnlyWhenABoundMoves() {
        // Aucun joueur : seule la boite de la fenetre est exercee.
        RemoteEntityView view = new RemoteEntityView(null, World.NETHER, 0);
        LongOpenHashSet window = new LongOpenHashSet();
        window.add(ChunkPos.toLong(0, 0));
        Box previous = view.windowBox(BOTTOM_Y, TOP_Y, window);
        assertEquals(new Box(0.0, BOTTOM_Y, 0.0, 16.0, TOP_Y, 16.0), previous, "Un chunk : toute sa colonne.");
        assertSame(previous, view.windowBox(BOTTOM_Y, TOP_Y, window), "Bornes inchangees : la meme boite, sans allocation.");
        // La fenetre grandit d'un chunk vers l'ouest, le nord, l'est puis le sud : chaque borne bouge seule a son tour.
        long[] growth = {ChunkPos.toLong(-1, 0), ChunkPos.toLong(0, -1), ChunkPos.toLong(1, 0), ChunkPos.toLong(0, 1)};
        for (long chunk : growth) {
            window.add(chunk);
            Box next = view.windowBox(BOTTOM_Y, TOP_Y, window);
            assertNotSame(previous, next, "Une borne a bouge : la boite est refaite.");
            previous = next;
        }
        assertEquals(new Box(-16.0, BOTTOM_Y, -16.0, 32.0, TOP_Y, 32.0), previous, "Les cinq chunks en croix tiennent dans la boite.");
    }
}
