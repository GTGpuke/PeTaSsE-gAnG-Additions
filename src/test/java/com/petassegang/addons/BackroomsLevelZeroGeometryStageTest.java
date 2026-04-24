package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellConnections;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;
import com.petassegang.addons.world.backrooms.level0.stage.geometry.LevelZeroLegacyGeometryStage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifie que la couche de geometry noise est bien repartie d'une base vide.
 */
@DisplayName("Geometry stage du Level 0")
class BackroomsLevelZeroGeometryStageTest {

    @Test
    @DisplayName("Aucune variante geometrique n'est emise par defaut")
    void testGeometryStageEmitsNoVariantsYet() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(true);

        for (LevelZeroCellTopology topology : LevelZeroCellTopology.values()) {
            int mask = stage.sample(
                    new LevelZeroCellContext(12, -8, 998877L),
                    topology,
                    LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH);

            assertEquals(LevelZeroGeometryMask.none(), mask,
                    "La geometry stage doit rester vide tant que les variantes sont reconstruites une par une.");
        }
    }

    @Test
    @DisplayName("La desactivation explicite garde aussi un masque vide")
    void testDisabledGeometryStageReturnsEmptyMask() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(false);
        LevelZeroCellContext context = new LevelZeroCellContext(-4, 11, 24680L);

        int mask = stage.sample(context, LevelZeroCellTopology.CORRIDOR);

        assertEquals(LevelZeroGeometryMask.none(), mask,
                "Sans noise geometry, le masque doit etre vide.");
    }
}
