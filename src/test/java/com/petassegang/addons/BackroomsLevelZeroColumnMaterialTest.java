package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroCoords;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellState;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroLayoutSampler;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroBlockPalette;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroColumnCoordinates;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroColumnMaterial;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroResolvedColumn;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroResolvedColumnResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifie la traduction intermediaire des colonnes du Level 0.
 */
@DisplayName("Materiaux de colonne du Level 0")
class BackroomsLevelZeroColumnMaterialTest {

    @Test
    @DisplayName("Une cellule traversable produit une colonne d'air avec le bon plafond")
    void testWalkableColumnUsesAirInterior() {
        long layoutSeed = 12345L;
        LevelZeroLayout layout = LevelZeroLayout.generate(0, 0, layoutSeed);
        LevelZeroLayoutSampler sampler = new LevelZeroLayoutSampler(layoutSeed);
        LevelZeroResolvedColumnResolver resolver = new LevelZeroResolvedColumnResolver(new LevelZeroBlockPalette());

        int localX = 0;
        int localZ = 0;
        LevelZeroCellState state = layout.cellState(localX, localZ);
        int worldX = LevelZeroCoords.chunkStartX(0) + localX;
        int worldZ = LevelZeroCoords.chunkStartZ(0) + localZ;

        LevelZeroResolvedColumn resolvedColumn = resolver.resolve(
                layout,
                sampler,
                new LevelZeroColumnCoordinates(localX, localZ, worldX, worldZ));
        LevelZeroColumnMaterial material = resolvedColumn.material();

        assertEquals(true, material.walkable(),
                "Une cellule traversable doit produire une colonne declaree traversable.");
        assertEquals(new LevelZeroColumnCoordinates(localX, localZ, worldX, worldZ), resolvedColumn.coordinates(),
                "La colonne resolue doit conserver ses coordonnees de contexte.");
        assertEquals(state, resolvedColumn.cellState(),
                "La colonne resolue doit conserver l'etat semantique source.");
        assertEquals(sampler.isWallpaperExposed(worldX, worldZ), resolvedColumn.exposedWallpaper(),
                "La colonne resolue doit conserver l'information d'exposition du wallpaper.");
        assertEquals(Blocks.AIR.getDefaultState(), material.interior(),
                "L'interieur d'une cellule traversable doit rester de l'air.");
        assertEquals(state.lighted()
                        ? ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT.getDefaultState()
                        : ModBlocks.LEVEL_ZERO_CEILING_TILE.getDefaultState(),
                material.ceiling(),
                "Le plafond d'une cellule traversable doit rester pilote par la lumiere historique.");
    }

    @Test
    @DisplayName("Une cellule mur produit une colonne non traversable coherente")
    void testWallColumnUsesResolvedWallInterior() {
        long layoutSeed = 998877L;
        LevelZeroLayout layout = LevelZeroLayout.generate(4, 7, layoutSeed);
        LevelZeroLayoutSampler sampler = new LevelZeroLayoutSampler(layoutSeed);
        LevelZeroResolvedColumnResolver resolver = new LevelZeroResolvedColumnResolver(new LevelZeroBlockPalette());

        int localX = 1;
        int localZ = 1;
        LevelZeroCellState state = layout.cellState(localX, localZ);
        int worldX = LevelZeroCoords.chunkStartX(4) + localX;
        int worldZ = LevelZeroCoords.chunkStartZ(7) + localZ;

        LevelZeroResolvedColumn resolvedColumn = resolver.resolve(
                layout,
                sampler,
                new LevelZeroColumnCoordinates(localX, localZ, worldX, worldZ));
        LevelZeroColumnMaterial material = resolvedColumn.material();
        BlockState expectedInterior = new LevelZeroBlockPalette().wall(
                sampler.isWallpaperExposed(worldX, worldZ),
                sampler.isWallpaperExposed(worldX, worldZ) ? sampler.sampleWallpaperFaceMask(worldX, worldZ) : 0);

        assertEquals(false, state.walkable(),
                "Le point choisi doit rester un mur pour verrouiller ce test.");
        assertEquals(new LevelZeroColumnCoordinates(localX, localZ, worldX, worldZ), resolvedColumn.coordinates(),
                "La colonne resolue doit conserver ses coordonnees de contexte.");
        assertEquals(state, resolvedColumn.cellState(),
                "La colonne resolue doit conserver l'etat semantique source.");
        assertEquals(sampler.isWallpaperExposed(worldX, worldZ), resolvedColumn.exposedWallpaper(),
                "La colonne resolue doit conserver la visibilite du wallpaper.");
        assertEquals(resolvedColumn.exposedWallpaper() ? sampler.sampleWallpaperFaceMask(worldX, worldZ) : 0,
                resolvedColumn.faceMask(),
                "Le face mask resolu doit rester coherent avec le sampler historique.");
        assertEquals(false, material.walkable(),
                "Une cellule mur doit produire une colonne non traversable.");
        assertEquals(expectedInterior, material.interior(),
                "L'interieur d'un mur doit rester aligne avec la logique historique de wallpaper.");
        assertEquals(ModBlocks.LEVEL_ZERO_CEILING_TILE.getDefaultState(), material.ceiling(),
                "Le plafond d'un mur doit rester une dalle standard.");
    }
}
