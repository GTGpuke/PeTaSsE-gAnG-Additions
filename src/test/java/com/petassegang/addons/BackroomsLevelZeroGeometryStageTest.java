package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroLegacyGeometryStage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifie que la geometry stage n'ajoute pas de bruit topologique excessif.
 */
@DisplayName("Geometry stage du Level 0")
class BackroomsLevelZeroGeometryStageTest {

    @Test
    @DisplayName("Les jonctions restent stables sans micro-geometrie structurelle")
    void testJunctionsStayClean() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(true);
        LevelZeroCellContext context = new LevelZeroCellContext(12, -8, 998877L);

        int mask = stage.sample(context, LevelZeroCellTopology.JUNCTION);

        assertEquals(LevelZeroGeometryMask.none(), mask,
                "Une jonction ne doit pas etre surchargee par de la pseudo-topologie.");
    }

    @Test
    @DisplayName("Les grandes pieces et murs ne recoivent pas d'anomalies")
    void testWallsAndLargeRoomsRemainUnchanged() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(true);
        LevelZeroCellContext context = new LevelZeroCellContext(3, 7, 12345L);

        assertEquals(LevelZeroGeometryMask.none(), stage.sample(context, LevelZeroCellTopology.WALL),
                "Un mur plein ne doit pas recevoir d'anomalie geometrique.");
        assertEquals(LevelZeroGeometryMask.none(), stage.sample(context, LevelZeroCellTopology.ROOM_LARGE),
                "Une grande piece historique doit rester stable a ce stade.");
    }

    @Test
    @DisplayName("La desactivation de la geometry stage force un masque vide")
    void testDisabledGeometryStageReturnsEmptyMask() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(false);
        LevelZeroCellContext context = new LevelZeroCellContext(-4, 11, 24680L);

        int mask = stage.sample(context, LevelZeroCellTopology.CORRIDOR);

        assertEquals(LevelZeroGeometryMask.none(), mask,
                "Sans noise geometry, le masque doit etre vide.");
    }

    @Test
    @DisplayName("Les couloirs ne produisent plus de demi-murs ni d'alcoves parasites")
    void testCorridorsAvoidJunctionLikeFeatures() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(true);
        LevelZeroCellContext context = new LevelZeroCellContext(20, 5, 778899L);

        int mask = stage.sample(context, LevelZeroCellTopology.CORRIDOR);

        assertFalse(LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.HALF_WALL),
                "Un couloir droit ne doit pas recevoir de demi-mur structurel.");
        assertFalse(LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.ALCOVE),
                "Une alcove ne doit pas apparaitre dans un couloir droit simple.");
    }
}
