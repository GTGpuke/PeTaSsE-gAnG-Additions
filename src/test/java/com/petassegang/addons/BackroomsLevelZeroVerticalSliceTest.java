package com.petassegang.addons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroLayerStackLayout;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroVerticalLayout;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroVerticalSlice;

class BackroomsLevelZeroVerticalSliceTest {

    @Test
    @DisplayName("La slice legacy reproduit exactement les Y actuels")
    void shouldExposeLegacySliceWithCurrentYs() {
        LevelZeroVerticalSlice slice = LevelZeroVerticalSlice.legacySingleLayer();

        assertEquals(0, slice.layerIndex());
        assertEquals(LevelZeroVerticalLayout.bedrockY(), slice.bedrockY());
        assertEquals(LevelZeroVerticalLayout.subfloorY(), slice.subfloorY());
        assertEquals(LevelZeroVerticalLayout.floorY(), slice.floorY());
        assertEquals(LevelZeroVerticalLayout.airMinY(), slice.airMinY());
        assertEquals(LevelZeroVerticalLayout.airMaxY(), slice.airMaxY());
        assertEquals(LevelZeroVerticalLayout.ceilingY(), slice.ceilingY());
    }

    @Test
    @DisplayName("Les slices canoniques sont deja pretes pour une boucle multi-layer")
    void shouldExposeCanonicalSlicesReadyForFutureLoop() {
        assertEquals(5, LevelZeroLayerStackLayout.defaultSlices().size());

        LevelZeroVerticalSlice layerThree = LevelZeroLayerStackLayout.defaultSlices().get(3);
        assertEquals(3, layerThree.layerIndex());
        assertEquals(30, layerThree.baseY());
        assertEquals(30, layerThree.floorY());
        assertEquals(31, layerThree.airMinY());
        assertEquals(34, layerThree.airMaxY());
        assertEquals(35, layerThree.ceilingY());
    }
}
