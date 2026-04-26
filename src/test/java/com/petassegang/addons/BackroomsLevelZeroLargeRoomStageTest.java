package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;
import com.petassegang.addons.world.backrooms.level0.stage.topology.LevelZeroLargeRoomStage;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifie les garanties de placement des grandes pieces du Level 0.
 */
@DisplayName("Large room stage du Level 0")
class BackroomsLevelZeroLargeRoomStageTest {

    @Test
    @DisplayName("Deux grandes pieces ne peuvent pas etre collees")
    void testLargeRoomsDoNotTouchEachOther() {
        LevelZeroLargeRoomStage stage = new LevelZeroLargeRoomStage();

        for (int x = -64; x <= 64; x++) {
            for (int z = -64; z <= 64; z++) {
                LevelZeroCellContext context = new LevelZeroCellContext(x, z, 12345L);
                if (!stage.sample(context)) {
                    continue;
                }

                for (int offsetX = -1; offsetX <= 1; offsetX++) {
                    for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                        if (offsetX == 0 && offsetZ == 0) {
                            continue;
                        }
                        LevelZeroCellContext neighbor = new LevelZeroCellContext(
                                x + offsetX,
                                z + offsetZ,
                                12345L);
                        assertFalse(stage.sample(neighbor),
                                "Une grande piece ne doit toucher aucune autre grande piece.");
                    }
                }
            }
        }
    }
}
