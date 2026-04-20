package com.petassegang.addons.world.backrooms.level0.write;

import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroVerticalSlice;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellState;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroLayoutSampler;
import com.petassegang.addons.world.backrooms.level0.write.structure.LevelZeroStructureResolver;

/**
 * Frontiere entre le layout logique et l'ecriture bloc par bloc.
 *
 * <p>Cette classe concentre les dernieres lectures "metier" necessaires avant
 * le writer : exposition du wallpaper, faces visibles, props muraux,
 * structures semantiques et palette materielle. Une fois la colonne resolue,
 * les stages d'ecriture n'ont plus vocation a recalculer de decisions
 * globales ; ils doivent seulement traduire cet etat en blocs.
 */
public final class LevelZeroResolvedColumnResolver {

    private final LevelZeroBlockPalette blockPalette;
    private final LevelZeroSurfaceDetailResolver surfaceDetailResolver;
    private final LevelZeroWallPropResolver wallPropResolver;
    private final LevelZeroStructureResolver structureResolver;

    /**
     * Construit un resolveur base sur la palette du Level 0.
     *
     * @param blockPalette palette de blocs
     */
    public LevelZeroResolvedColumnResolver(LevelZeroBlockPalette blockPalette) {
        this.blockPalette = blockPalette;
        this.surfaceDetailResolver = new LevelZeroSurfaceDetailResolver();
        this.wallPropResolver = new LevelZeroWallPropResolver();
        this.structureResolver = new LevelZeroStructureResolver();
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
                                           LevelZeroColumnCoordinates coordinates,
                                           LevelZeroVerticalSlice verticalSlice) {
        LevelZeroCellState cellState = layout.cellState(coordinates.localX(), coordinates.localZ());
        // Les masques de wallpaper et d'exposition sont volontairement resolus
        // ici, avant l'ecriture, pour centraliser toute la lecture "globale"
        // du voisinage dans un seul endroit deterministe.
        boolean exposedWallpaper = sampler.isWallpaperExposed(coordinates.worldX(), coordinates.worldZ());
        int wallpaperFaceMask = exposedWallpaper
                ? sampler.sampleWallpaperFaceMask(coordinates.worldX(), coordinates.worldZ())
                : 0;
        int exposedFaceMask = exposedWallpaper
                ? sampler.sampleExposedFaceMask(coordinates.worldX(), coordinates.worldZ())
                : 0;
        // La colonne resolue sert de contrat entre le layout logique et le
        // writer : a partir d'ici, l'ecriture ne devrait plus recalculer de
        // decision metier, seulement traduire cet etat en blocs.
        return new LevelZeroResolvedColumn(
                coordinates,
                verticalSlice,
                cellState,
                exposedWallpaper,
                wallpaperFaceMask,
                surfaceDetailResolver.resolve(cellState, coordinates.worldX(), coordinates.worldZ()),
                wallPropResolver.resolve(
                        cellState.walkable(),
                        exposedWallpaper,
                        exposedFaceMask,
                        verticalSlice,
                        coordinates.worldX(),
                        coordinates.worldZ()),
                structureResolver.resolve(
                        cellState.roomKind(),
                        cellState.walkable(),
                        coordinates.worldX(),
                        coordinates.worldZ()),
                blockPalette.column(cellState, exposedWallpaper, wallpaperFaceMask));
    }
}
