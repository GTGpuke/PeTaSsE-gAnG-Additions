package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroVerticalSlice;
import com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroVerticalLayout;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroBaseboardStyle;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroLayoutSampler;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroConnectedDetailVariant;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroWallFixture;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroWallPropProfile;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroWallPropResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie la resolution des petits details muraux.
 */
@DisplayName("Wall prop resolver du Level 0")
class BackroomsLevelZeroWallPropResolverTest {

    private static final LevelZeroVerticalSlice LEGACY_VERTICAL_SLICE =
            LevelZeroVerticalSlice.legacySingleLayer();

    @Test
    @DisplayName("La plinthe d'un mur expose reste deterministe")
    void testBaseboardOnExposedWall() {
        LevelZeroWallPropResolver resolver = new LevelZeroWallPropResolver();

        LevelZeroWallPropProfile first = resolver.resolve(
                false,
                true,
                LevelZeroLayoutSampler.NORTH_MASK,
                LEGACY_VERTICAL_SLICE,
                12,
                -7);
        LevelZeroWallPropProfile second = resolver.resolve(
                false,
                true,
                LevelZeroLayoutSampler.NORTH_MASK,
                LEGACY_VERTICAL_SLICE,
                12,
                -7);

        assertEquals(first, second,
                "La presence et le style de plinthe doivent rester deterministes.");
    }

    @Test
    @DisplayName("Une colonne traversable ne recoit pas de detail mural")
    void testWalkableColumnHasNoWallProps() {
        LevelZeroWallPropResolver resolver = new LevelZeroWallPropResolver();

        LevelZeroWallPropProfile profile = resolver.resolve(
                true,
                true,
                LevelZeroLayoutSampler.NORTH_MASK,
                LEGACY_VERTICAL_SLICE,
                4,
                9);

        assertFalse(profile.hasBaseboard(),
                "Une colonne traversable ne doit pas recevoir de plinthe murale.");
        assertFalse(profile.hasFixture(),
                "Une colonne traversable ne doit pas recevoir de prise ou d'interrupteur.");
    }

    @Test
    @DisplayName("La face de fixture choisie reste une face visible")
    void testFixtureFaceStaysVisible() {
        LevelZeroWallPropResolver resolver = new LevelZeroWallPropResolver();

        for (int i = 0; i < 512; i++) {
            LevelZeroWallPropProfile profile = resolver.resolve(
                    false,
                    true,
                    LevelZeroLayoutSampler.NORTH_MASK | LevelZeroLayoutSampler.EAST_MASK,
                    LEGACY_VERTICAL_SLICE,
                    i,
                    -i);
            if (!profile.hasFixture()) {
                continue;
            }
            assertTrue(profile.fixtureFaceMask() == LevelZeroLayoutSampler.NORTH_MASK
                            || profile.fixtureFaceMask() == LevelZeroLayoutSampler.EAST_MASK,
                    "Une fixture ne doit jamais pointer vers une face invisible.");
            assertTrue(profile.fixtureY() == LevelZeroVerticalLayout.floorY() + 1
                            || profile.fixtureY() == LevelZeroVerticalLayout.floorY() + 2,
                    "La hauteur d'une fixture doit rester dans les deux positions prevues.");
            return;
        }
    }

    @Test
    @DisplayName("Le resolver reste deterministe")
    void testResolverDeterministic() {
        LevelZeroWallPropResolver resolver = new LevelZeroWallPropResolver();

        LevelZeroWallPropProfile first = resolver.resolve(
                false,
                true,
                LevelZeroLayoutSampler.SOUTH_MASK | LevelZeroLayoutSampler.WEST_MASK,
                LEGACY_VERTICAL_SLICE,
                33,
                44);
        LevelZeroWallPropProfile second = resolver.resolve(
                false,
                true,
                LevelZeroLayoutSampler.SOUTH_MASK | LevelZeroLayoutSampler.WEST_MASK,
                LEGACY_VERTICAL_SLICE,
                33,
                44);

        assertEquals(first, second,
                "Les petits details muraux doivent rester deterministes a seed fixe.");
    }

    @Test
    @DisplayName("La variante de plinthe peut etre derivee des faces exposees")
    void testBaseboardVariantPreparedForFineAssets() {
        LevelZeroWallPropProfile profile = new LevelZeroWallPropProfile(
                LevelZeroLayoutSampler.NORTH_MASK | LevelZeroLayoutSampler.EAST_MASK,
                LevelZeroBaseboardStyle.WHITE,
                LevelZeroWallFixture.NONE,
                0,
                0);

        assertEquals(LevelZeroConnectedDetailVariant.Shape.CORNER, profile.baseboardVariant().shape(),
                "La plinthe doit deja exposer une variante connectee exploitable plus tard.");
    }

    @Test
    @DisplayName("Les styles de plinthe restent dans les variantes attendues")
    void testBaseboardStyleUsesExpectedVariants() {
        for (int x = 0; x < 128; x++) {
            LevelZeroWallPropProfile profile = LevelZeroWallPropResolver.resolveBaseboardAtWorld(
                    x,
                    0,
                    LevelZeroLayoutSampler.NORTH_MASK,
                    LEGACY_VERTICAL_SLICE);
            assertTrue(profile.hasBaseboard(),
                    "La plinthe doit maintenant etre presente sur tous les murs visibles.");
            assertEquals(LevelZeroBaseboardStyle.WHITE,
                    profile.baseboardStyle(),
                    "La seule variante restante doit etre la plinthe blanche.");
        }
    }
}
