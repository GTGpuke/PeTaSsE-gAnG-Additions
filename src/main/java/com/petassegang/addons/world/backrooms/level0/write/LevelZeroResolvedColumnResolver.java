package com.petassegang.addons.world.backrooms.level0.write;

import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellState;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroLayoutSampler;

/**
 * Resolveur de colonnes complet, entre layout et pipeline d'ecriture.
 */
public final class LevelZeroResolvedColumnResolver {

    private final LevelZeroBlockPalette blockPalette;

    /**
     * Construit un resolveur base sur la palette du Level 0.
     *
     * @param blockPalette palette de blocs
     */
    public LevelZeroResolvedColumnResolver(LevelZeroBlockPalette blockPalette) {
        this.blockPalette = blockPalette;
    }

    /**
     * Traduit une colonne semantique en colonne resolue complete.
     *
     * @param layout layout local du chunk
     * @param sampler sampler des murs adaptatifs
     * @param coordinates coordonnees completes de la colonne
     * @return colonne resolue complete
     */
    public LevelZeroResolvedColumn resolve(LevelZeroLayout layout,
                                           LevelZeroLayoutSampler sampler,
                                           LevelZeroColumnCoordinates coordinates) {
        LevelZeroCellState cellState = layout.cellState(coordinates.localX(), coordinates.localZ());
        boolean exposedWallpaper = sampler.isWallpaperExposed(coordinates.worldX(), coordinates.worldZ());
        int faceMask = exposedWallpaper
                ? sampler.sampleWallpaperFaceMask(coordinates.worldX(), coordinates.worldZ())
                : 0;
        return new LevelZeroResolvedColumn(
                coordinates,
                cellState,
                exposedWallpaper,
                faceMask,
                blockPalette.column(cellState, exposedWallpaper, faceMask));
    }
}
