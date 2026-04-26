package com.petassegang.addons.world.backrooms.level0.write;

import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroVerticalSlice;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellState;
import com.petassegang.addons.world.backrooms.level0.write.structure.LevelZeroStructureProfile;

/**
 * Colonne totalement resolue, entre semantique de layout et ecriture finale.
 */
public record LevelZeroResolvedColumn(
        LevelZeroColumnCoordinates coordinates,
        LevelZeroVerticalSlice verticalSlice,
        LevelZeroCellState cellState,
        boolean exposedWallpaper,
        int faceMask,
        LevelZeroSurfaceDetailProfile surfaceDetails,
        LevelZeroWallPropProfile wallProps,
        LevelZeroStructureProfile structure,
        LevelZeroColumnMaterial material) {
}
