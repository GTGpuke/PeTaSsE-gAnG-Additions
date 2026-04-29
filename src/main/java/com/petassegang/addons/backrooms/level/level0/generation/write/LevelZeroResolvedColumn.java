package com.petassegang.addons.backrooms.level.level0.generation.write;

import com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroVerticalSlice;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellState;
import com.petassegang.addons.backrooms.level.level0.generation.write.structure.LevelZeroStructureProfile;

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
