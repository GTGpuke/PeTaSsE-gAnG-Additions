package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellConnections;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroConnectedDetailVariant;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroSurfaceDetail;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroSurfaceDetailProfile;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifie la normalisation des masques de details connectes.
 */
@DisplayName("Variantes connectees des surface details du Level 0")
class BackroomsLevelZeroSurfaceDetailVariantTest {

    @Test
    @DisplayName("Un detail sans voisin reste une variante SINGLE")
    void testSingleVariant() {
        LevelZeroSurfaceDetailProfile profile = new LevelZeroSurfaceDetailProfile(
                LevelZeroSurfaceDetail.FLOOR_STAIN,
                0,
                LevelZeroSurfaceDetail.NONE,
                0,
                LevelZeroSurfaceDetail.NONE,
                0);

        assertEquals(LevelZeroConnectedDetailVariant.Shape.SINGLE, profile.floorVariant().shape());
        assertEquals(0, profile.floorVariant().rotationQuarterTurns());
    }

    @Test
    @DisplayName("Un detail vertical devient une variante STRAIGHT")
    void testStraightVariant() {
        LevelZeroSurfaceDetailProfile profile = new LevelZeroSurfaceDetailProfile(
                LevelZeroSurfaceDetail.NONE,
                0,
                LevelZeroSurfaceDetail.WALL_DAMP,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH,
                LevelZeroSurfaceDetail.NONE,
                0);

        assertEquals(LevelZeroConnectedDetailVariant.Shape.STRAIGHT, profile.wallVariant().shape());
        assertEquals(0, profile.wallVariant().rotationQuarterTurns());
    }

    @Test
    @DisplayName("Un detail en angle devient une variante CORNER correctement orientee")
    void testCornerVariant() {
        LevelZeroSurfaceDetailProfile profile = new LevelZeroSurfaceDetailProfile(
                LevelZeroSurfaceDetail.NONE,
                0,
                LevelZeroSurfaceDetail.NONE,
                0,
                LevelZeroSurfaceDetail.CEILING_STAIN,
                LevelZeroCellConnections.EAST | LevelZeroCellConnections.SOUTH);

        assertEquals(LevelZeroConnectedDetailVariant.Shape.CORNER, profile.ceilingVariant().shape());
        assertEquals(1, profile.ceilingVariant().rotationQuarterTurns());
    }

    @Test
    @DisplayName("Un detail a trois connexions devient une variante TEE")
    void testTeeVariant() {
        LevelZeroConnectedDetailVariant variant = LevelZeroConnectedDetailVariant.fromConnectionMask(
                LevelZeroCellConnections.NORTH
                        | LevelZeroCellConnections.EAST
                        | LevelZeroCellConnections.SOUTH);

        assertEquals(LevelZeroConnectedDetailVariant.Shape.TEE, variant.shape());
        assertEquals(1, variant.rotationQuarterTurns());
    }

    @Test
    @DisplayName("Un detail a quatre connexions devient une variante CROSS")
    void testCrossVariant() {
        LevelZeroConnectedDetailVariant variant = LevelZeroConnectedDetailVariant.fromConnectionMask(
                LevelZeroCellConnections.NORTH
                        | LevelZeroCellConnections.EAST
                        | LevelZeroCellConnections.SOUTH
                        | LevelZeroCellConnections.WEST);

        assertEquals(LevelZeroConnectedDetailVariant.Shape.CROSS, variant.shape());
        assertEquals(0, variant.rotationQuarterTurns());
    }
}
