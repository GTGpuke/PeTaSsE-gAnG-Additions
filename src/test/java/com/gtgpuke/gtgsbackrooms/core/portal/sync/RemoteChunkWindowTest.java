package com.gtgpuke.gtgsbackrooms.core.portal.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.ChunkPos;

@DisplayName("Fenetre de chunks distants")
class RemoteChunkWindowTest {

    @Test
    @DisplayName("La fenetre part du centre, grandit anneau par anneau et couvre le carre complet")
    void windowIsOrderedFromTheCenter() {
        LongLinkedOpenHashSet window = new LongLinkedOpenHashSet();
        RemoteChunkWindow.collect(3, -7, 2, window);
        LongArrayList out = new LongArrayList(window);
        assertEquals(25, out.size(), "Rayon 2 : 5x5 chunks.");
        assertEquals(ChunkPos.toLong(3, -7), out.getLong(0), "Le centre vient en premier.");
        for (int i = 1; i <= 8; i++) {
            long key = out.getLong(i);
            int distance = Math.max(Math.abs(ChunkPos.getPackedX(key) - 3), Math.abs(ChunkPos.getPackedZ(key) + 7));
            assertEquals(1, distance, "Les huit suivants sont a distance 1.");
        }
        LongLinkedOpenHashSet single = new LongLinkedOpenHashSet();
        RemoteChunkWindow.collect(0, 0, 0, single);
        assertEquals(1, single.size(), "Rayon 0 : le seul chunk central.");
    }

    @Test
    @DisplayName("Deux fenetres qui se chevauchent se cumulent dans un ensemble ordonne, la premiere en tete")
    void overlappingWindowsMerge() {
        LongLinkedOpenHashSet out = new LongLinkedOpenHashSet();
        RemoteChunkWindow.collect(0, 0, 1, out);
        RemoteChunkWindow.collect(1, 0, 1, out);
        assertEquals(12, out.size(), "3x3 + 3x3 avec six chunks communs = douze chunks.");
        assertEquals(ChunkPos.toLong(0, 0), out.firstLong(), "La premiere fenetre garde sa place en tete.");
    }

    @Test
    @DisplayName("Une fenetre tient dans la carte client tant qu'elle laisse la marge d'un rayon de chaque cote")
    void fitsAroundKeepsAMargin() {
        int limit = RemoteChunkWindow.MAP_RADIUS - 2 * 2;
        assertTrue(RemoteChunkWindow.fitsAround(0, 0, limit, 0, 2), "A la limite exacte.");
        assertFalse(RemoteChunkWindow.fitsAround(0, 0, limit + 1, 0, 2), "Un chunk trop loin.");
        assertTrue(RemoteChunkWindow.fitsAround(10, 10, 10, 10, 2), "Le foyer lui-meme tient toujours.");
        assertEquals(Math.max(2, RemoteChunkWindow.LOAD_DISTANCE) + 3, RemoteChunkWindow.MAP_RADIUS, "Regle vanilla du rayon de carte.");
    }
}
