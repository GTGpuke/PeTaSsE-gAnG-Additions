package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Grandes pieces du Level 0")
class LevelZeroLargeRoomsTest {

    @Test
    @DisplayName("Deux grandes pieces ne se touchent jamais, meme en diagonale")
    void largeRoomsNeverTouch() {
        int found = 0;
        for (int x = -64; x <= 64; x++) {
            for (int z = -64; z <= 64; z++) {
                LevelZeroCellContext context = new LevelZeroCellContext(x, z, 12345L, 0);
                if (!LevelZeroLargeRooms.isLargeRoom(context)) {
                    continue;
                }
                found++;
                for (int offsetX = -1; offsetX <= 1; offsetX++) {
                    for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        if (offsetX != 0 || offsetZ != 0) {
                            assertFalse(LevelZeroLargeRooms.isLargeRoom(context.offset(offsetX, offsetZ)),
                                    "Une grande piece ne doit toucher aucune autre grande piece.");
                        }
                    }
                }
            }
        }
        assertTrue(found > 100, "L'echantillon doit contenir de nombreuses grandes pieces : " + found);
    }

    @Test
    @DisplayName("Le marquage depend du layer")
    void markingDependsOnLayer() {
        int differences = 0;
        for (int x = -32; x <= 32; x++) {
            for (int z = -32; z <= 32; z++) {
                boolean layerZero = LevelZeroLargeRooms.isLargeRoom(new LevelZeroCellContext(x, z, 777L, 0));
                boolean layerTwo = LevelZeroLargeRooms.isLargeRoom(new LevelZeroCellContext(x, z, 777L, 2));
                if (layerZero != layerTwo) {
                    differences++;
                }
            }
        }
        assertTrue(differences > 0, "L'arbitrage entre candidates varie d'un layer a l'autre.");
    }
}
