package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;
import com.petassegang.addons.world.backrooms.level0.stage.geometry.LevelZeroLegacyGeometryStage;

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

        int tMask = stage.sample(context, LevelZeroCellTopology.T_JUNCTION);
        int crossMask = stage.sample(context, LevelZeroCellTopology.CROSSROAD);
        int legacyMask = stage.sample(context, LevelZeroCellTopology.JUNCTION);

        assertEquals(LevelZeroGeometryMask.none(), tMask,
                "Une jonction en T ne doit pas etre surchargee par de la pseudo-topologie.");
        assertEquals(LevelZeroGeometryMask.none(), crossMask,
                "Un carrefour ne doit pas etre surcharge par de la pseudo-topologie.");
        assertEquals(LevelZeroGeometryMask.none(), legacyMask,
                "L'alias legacy de jonction doit rester neutre.");
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
    @DisplayName("Les couloirs evitent toujours les alcoves parasites")
    void testCorridorsAvoidJunctionLikeFeatures() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(true);
        LevelZeroCellContext context = new LevelZeroCellContext(20, 5, 778899L);

        int mask = stage.sample(context, LevelZeroCellTopology.CORRIDOR);

        assertFalse(LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.ALCOVE),
                "Une alcove ne doit pas apparaitre dans un couloir droit simple.");
    }

    @Test
    @DisplayName("Les angles peuvent recevoir des details legers mais pas de pseudo-jonctions")
    void testAnglesOnlyUseLightFeatures() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(true);
        LevelZeroCellContext context = new LevelZeroCellContext(14, -2, 12345L);

        int mask = stage.sample(context, LevelZeroCellTopology.ANGLE);

        assertFalse(LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.HALF_WALL),
                "Un angle ne doit pas recevoir de demi-mur structurel.");
        assertFalse(LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.ALCOVE),
                "Un angle ne doit pas devenir une pseudo-alcove a ce stade.");
        assertFalse(LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.PINCH_1WIDE),
                "Un angle ne doit pas recevoir d'etranglement ponctuel.");
    }

    @Test
    @DisplayName("Les couloirs droits peuvent recevoir un demi-mur, mais jamais d'alcove")
    void testCorridorHalfWallIsAllowedButStillConservative() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(true);
        boolean sawHalfWall = false;

        for (int x = -32; x <= 32 && !sawHalfWall; x++) {
            for (int z = -32; z <= 32 && !sawHalfWall; z++) {
                int mask = stage.sample(new LevelZeroCellContext(x, z, 12345L), LevelZeroCellTopology.CORRIDOR);
                assertFalse(LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.ALCOVE),
                        "Un couloir droit ne doit jamais devenir une alcove.");
                if (LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.HALF_WALL)) {
                    sawHalfWall = true;
                }
            }
        }

        assertEquals(true, sawHalfWall,
                "Sur un echantillon raisonnable, on doit pouvoir rencontrer au moins un demi-mur de couloir.");
    }

    @Test
    @DisplayName("Les dead ends restent majoritairement normaux avec seulement quelques variantes")
    void testDeadEndsStayMostlyNormalAndUseLimitedFeatures() {
        LevelZeroLegacyGeometryStage stage = new LevelZeroLegacyGeometryStage(true);
        int featureCount = 0;
        boolean sawAlcove = false;
        boolean sawRecess = false;

        for (int x = -32; x <= 32; x++) {
            for (int z = -32; z <= 32; z++) {
                int mask = stage.sample(new LevelZeroCellContext(x, z, 54321L), LevelZeroCellTopology.DEAD_END);
                assertFalse(LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.HALF_WALL),
                        "Un dead end ne doit pas recevoir de demi-mur.");
                assertFalse(LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.PINCH_1WIDE),
                        "Un dead end ne doit pas recevoir d'etranglement 1-wide.");
                assertFalse(LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.OFFSET_WALL),
                        "Un dead end ne doit pas recevoir de decalage de mur a ce stade.");
                if (mask != LevelZeroGeometryMask.none()) {
                    featureCount++;
                    sawAlcove |= LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.ALCOVE);
                    sawRecess |= LevelZeroGeometryMask.has(mask, LevelZeroGeometryFeature.RECESS);
                }
            }
        }

        assertEquals(true, sawAlcove,
                "Sur un echantillon raisonnable, certains dead ends doivent encore produire une alcove.");
        assertEquals(true, sawRecess,
                "Sur un echantillon raisonnable, certains dead ends doivent maintenant pouvoir produire un retrait leger.");
        assertEquals(true, featureCount < 120,
                "Les dead ends doivent rester majoritairement normaux sur l'echantillon.");
    }
}
