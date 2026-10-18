package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.write;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.block.LevelZeroWallpaperBlock;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroCoords;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroVerticalSlice;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroCellEvaluation;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroLayout;
import com.gtgpuke.gtgsbackrooms.init.ModBlocks;

@DisplayName("Writer de blocs du Level 0")
class LevelZeroBlockWriterTest {

    private static final long SEED = 998877L;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    private static BlockState[] column(LevelZeroBlockWriter writer, LevelZeroLayout layout, LevelZeroVerticalSlice slice,
                                       int worldX, int worldZ) {
        BlockState[] states = new BlockState[LevelZeroLayerStackLayout.DIMENSION_HEIGHT];
        Arrays.fill(states, Blocks.AIR.getDefaultState());
        writer.writeColumnSample(states, layout, slice, worldX, worldZ);
        return states;
    }

    @Test
    @DisplayName("Une colonne ouverte recoit sol, air, plafond et bedrock inter-layer")
    void walkableColumn() {
        LevelZeroBlockWriter writer = new LevelZeroBlockWriter(LevelZeroBlockPalette.fromRegistry(), false, false);
        LevelZeroVerticalSlice slice = LevelZeroVerticalSlice.of(2);
        LevelZeroLayout layout = LevelZeroLayout.generate(0, 0, SEED, 2);
        BlockState[] states = column(writer, layout, slice, 1, 1);

        assertTrue(states[slice.floorY()].isOf(ModBlocks.LEVEL_ZERO_DAMP_CARPET)
                || states[slice.floorY()].isOf(ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED), "Sol en moquette.");
        for (int y = slice.airMinY(); y <= slice.airMaxY(); y++) {
            assertTrue(states[y].isAir(), "Air en Y=" + y + ".");
        }
        BlockState ceiling = states[slice.ceilingY()];
        assertTrue(ceiling.isOf(ModBlocks.LEVEL_ZERO_CEILING_TILE) || ceiling.isOf(ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT),
                "Plafond en dalle ou neon.");
        for (int y = slice.bedrockMinY(); y <= slice.bedrockMaxY(); y++) {
            assertTrue(states[y].isOf(Blocks.BEDROCK), "Bedrock en Y=" + y + ".");
        }
        assertTrue(states[slice.floorY() - 1].isAir(), "Rien n'est ecrit sous la slice.");
        assertTrue(states[slice.bedrockMaxY() + 1].isAir(), "Rien n'est ecrit au-dessus de la slice.");
    }

    @Test
    @DisplayName("Le neon n'est pose qu'au centre d'une cellule eclairee")
    void lightAtCellCenterOnly() {
        LevelZeroBlockWriter writer = new LevelZeroBlockWriter(LevelZeroBlockPalette.fromRegistry(), false, false);
        LevelZeroVerticalSlice slice = LevelZeroVerticalSlice.of(0);
        int lights = 0;
        for (int chunkX = -2; chunkX <= 2; chunkX++) {
            LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, 0, SEED, 0);
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int worldX = LevelZeroCoords.chunkStart(chunkX) + localX;
                    BlockState ceiling = column(writer, layout, slice, worldX, localZ)[slice.ceilingY()];
                    boolean isLight = ceiling.isOf(ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT);
                    assertEquals(layout.hasLight(localX, localZ), isLight, "Neon exactement la ou le layout le marque.");
                    lights += isLight ? 1 : 0;
                }
            }
        }
        assertTrue(lights > 0, "La zone de test contient des neons.");
    }

    @Test
    @DisplayName("Un mur visible est en papier peint adaptatif, un mur enfoui en bedrock")
    void wallColumns() {
        LevelZeroBlockWriter writer = new LevelZeroBlockWriter(LevelZeroBlockPalette.fromRegistry(), false, false);
        LevelZeroVerticalSlice slice = LevelZeroVerticalSlice.of(1);
        boolean exposedSeen = false;
        boolean buriedSeen = false;
        for (int chunkX = -3; chunkX <= 3; chunkX++) {
            LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, 2, SEED, 1);
            LevelZeroWallSampler sampler = new LevelZeroWallSampler(layout);
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    if (layout.isLocallyWalkable(localX, localZ)) {
                        continue;
                    }
                    int worldX = LevelZeroCoords.chunkStart(chunkX) + localX;
                    int worldZ = LevelZeroCoords.chunkStart(2) + localZ;
                    BlockState[] states = column(writer, layout, slice, worldX, worldZ);
                    BlockState wall = states[slice.airMinY()];
                    for (int y = slice.airMinY(); y <= slice.airMaxY(); y++) {
                        assertEquals(wall, states[y], "Le mur est uniforme sur toute sa hauteur.");
                    }
                    assertTrue(states[slice.ceilingY()].isOf(ModBlocks.LEVEL_ZERO_CEILING_TILE), "Plafond au-dessus d'un mur.");
                    int exposed = sampler.exposedFaceMask(worldX, worldZ);
                    if (exposed == 0) {
                        assertTrue(wall.isOf(Blocks.BEDROCK), "Mur enfoui en bedrock.");
                        buriedSeen = true;
                    } else {
                        assertTrue(wall.isOf(ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE), "Mur visible en papier peint.");
                        assertEquals(sampler.wallpaperFaceMask(worldX, worldZ), wall.get(LevelZeroWallpaperBlock.FACE_MASK),
                                "Le masque de faces vient du sampler.");
                        exposedSeen = true;
                    }
                }
            }
        }
        assertTrue(exposedSeen && buriedSeen, "La zone de test contient des murs visibles et enfouis.");
    }

    @Test
    @DisplayName("Le debug de micro-geometrie colore les cellules a gap et leurs blocs fermes")
    void debugGeometryPalette() {
        LevelZeroBlockWriter writer = new LevelZeroBlockWriter(LevelZeroBlockPalette.fromRegistry(), true, false);
        LevelZeroVerticalSlice slice = LevelZeroVerticalSlice.of(0);
        boolean gapFound = false;
        for (int chunkX = -6; chunkX <= 6 && !gapFound; chunkX++) {
            for (int chunkZ = -6; chunkZ <= 6 && !gapFound; chunkZ++) {
                LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, chunkZ, SEED, 0);
                for (int localX = 0; localX < 16 && !gapFound; localX++) {
                    for (int localZ = 0; localZ < 16 && !gapFound; localZ++) {
                        LevelZeroCellEvaluation cell = layout.cellAt(localX, localZ);
                        if (!cell.hasGeometryFeature()) {
                            continue;
                        }
                        gapFound = true;
                        int worldX = LevelZeroCoords.chunkStart(chunkX) + localX;
                        int worldZ = LevelZeroCoords.chunkStart(chunkZ) + localZ;
                        BlockState[] states = column(writer, layout, slice, worldX, worldZ);
                        assertTrue(states[slice.floorY()].isOf(Blocks.WHITE_CONCRETE), "Sol de debug sous un gap.");
                        if (!layout.isLocallyWalkable(localX, localZ)) {
                            assertTrue(states[slice.airMinY()].isOf(Blocks.GRAY_CONCRETE), "Bloc ferme par le gap en beton gris.");
                        }
                    }
                }
            }
        }
        assertTrue(gapFound, "La zone de test contient au moins un gap.");
    }

    @Test
    @DisplayName("Le debug des details muraux pose les elements muraux en blocs voyants")
    void debugWallProps() {
        LevelZeroBlockWriter writer = new LevelZeroBlockWriter(LevelZeroBlockPalette.fromRegistry(), false, true);
        LevelZeroVerticalSlice slice = LevelZeroVerticalSlice.of(0);
        boolean fixtureFound = false;
        for (int chunkX = -6; chunkX <= 6 && !fixtureFound; chunkX++) {
            LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, 1, SEED, 0);
            LevelZeroWallSampler sampler = new LevelZeroWallSampler(layout);
            for (int localX = 0; localX < 16 && !fixtureFound; localX++) {
                for (int localZ = 0; localZ < 16 && !fixtureFound; localZ++) {
                    int worldX = LevelZeroCoords.chunkStart(chunkX) + localX;
                    int worldZ = LevelZeroCoords.chunkStart(1) + localZ;
                    if (layout.isWalkable(localX, localZ)) {
                        continue;
                    }
                    int exposed = sampler.exposedFaceMask(worldX, worldZ);
                    LevelZeroWallProps props = LevelZeroWallProps.resolve(worldX, worldZ, exposed, slice);
                    if (!props.hasFixture()) {
                        continue;
                    }
                    fixtureFound = true;
                    BlockState[] states = column(writer, layout, slice, worldX, worldZ);
                    assertEquals(LevelZeroDebugPalette.fixture(props.fixture(), props.fixtureFaceMask()), states[props.fixtureY()],
                            "L'element mural est pose a sa hauteur.");
                    assertFalse(states[props.fixtureY()].isOf(ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE), "Le bloc de debug remplace le mur.");
                }
            }
        }
        assertTrue(fixtureFound, "La zone de test contient au moins un element mural.");
    }

    @Test
    @DisplayName("En debug de micro-geometrie, le bloc ferme par un gap est un mur de debug ; un mur enfoui reste en bedrock")
    void debugGapWallAndBuriedWall() {
        LevelZeroBlockWriter writer = new LevelZeroBlockWriter(LevelZeroBlockPalette.fromRegistry(), true, false);
        LevelZeroVerticalSlice slice = LevelZeroVerticalSlice.of(0);
        boolean floorChecked = false;
        boolean wallChecked = false;
        for (int chunkX = -8; chunkX <= 8 && !(floorChecked && wallChecked); chunkX++) {
            for (int chunkZ = -8; chunkZ <= 8 && !(floorChecked && wallChecked); chunkZ++) {
                LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, chunkZ, SEED, 0);
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        LevelZeroCellEvaluation cell = layout.cellAt(localX, localZ);
                        if (!cell.hasGeometryFeature() || !cell.walkable()) {
                            continue;
                        }
                        int worldX = LevelZeroCoords.chunkStart(chunkX) + localX;
                        int worldZ = LevelZeroCoords.chunkStart(chunkZ) + localZ;
                        BlockState[] states = column(writer, layout, slice, worldX, worldZ);
                        assertEquals(LevelZeroDebugPalette.gapFloor(), states[slice.floorY()], "Sol de debug sous une cellule a gap.");
                        floorChecked = true;
                        if (!cell.isLocallyWalkable(LevelZeroCoords.subCell(worldX), LevelZeroCoords.subCell(worldZ))) {
                            assertEquals(LevelZeroDebugPalette.gapWall(), states[slice.airMinY()], "Mur de debug la ou le gap ferme la cellule.");
                            wallChecked = true;
                        }
                    }
                }
            }
        }
        assertTrue(floorChecked, "Une cellule a gap existe dans l'echantillon.");
        assertTrue(wallChecked, "Un sous-bloc ferme par un gap existe dans l'echantillon.");
        assertFalse(LevelZeroDebugPalette.gapFloor().isAir(), "La palette de debug est un vrai bloc.");

        boolean buriedChecked = false;
        for (int chunkX = -8; chunkX <= 8 && !buriedChecked; chunkX++) {
            for (int chunkZ = -8; chunkZ <= 8 && !buriedChecked; chunkZ++) {
                LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, chunkZ, SEED, 0);
                for (int localX = 0; localX < 16 && !buriedChecked; localX++) {
                    for (int localZ = 0; localZ < 16 && !buriedChecked; localZ++) {
                        if (layout.cellAt(localX, localZ).walkable()) {
                            continue;
                        }
                        int worldX = LevelZeroCoords.chunkStart(chunkX) + localX;
                        int worldZ = LevelZeroCoords.chunkStart(chunkZ) + localZ;
                        BlockState[] states = column(writer, layout, slice, worldX, worldZ);
                        buriedChecked = states[slice.airMinY()].isOf(Blocks.BEDROCK);
                    }
                }
            }
        }
        assertTrue(buriedChecked, "Un mur sans face exposee est rempli de bedrock.");
    }

    @Test
    @DisplayName("Un echantillon plus court que la pile ignore les blocs au-dela de sa hauteur")
    void shortSampleIgnoresBlocksAboveIt() {
        LevelZeroBlockWriter writer = new LevelZeroBlockWriter(LevelZeroBlockPalette.fromRegistry(), false, false);
        LevelZeroVerticalSlice slice = LevelZeroVerticalSlice.of(0);
        BlockState[] states = new BlockState[slice.floorY() + 1];
        Arrays.fill(states, Blocks.AIR.getDefaultState());
        writer.writeColumnSample(states, LevelZeroLayout.generate(0, 0, SEED, 0), slice, 1, 1);
        assertFalse(states[slice.floorY()].isAir(), "Le sol, dans l'echantillon, est ecrit.");
    }

    @Test
    @DisplayName("Un echantillon ne recoit rien sous Y = 0 : une tranche plus basse que lui n'y ecrit aucun bloc")
    void sampleIgnoresBlocksBelowItsBottom() {
        LevelZeroBlockWriter writer = new LevelZeroBlockWriter(LevelZeroBlockPalette.fromRegistry(), false, false);
        LevelZeroVerticalSlice below = new LevelZeroVerticalSlice(0, -10, -9, -6, -5, -4, -1);
        BlockState[] states = column(writer, LevelZeroLayout.generate(0, 0, SEED, 0), below, 1, 1);
        for (BlockState state : states) {
            assertTrue(state.isAir(), "Aucun bloc d'une tranche negative n'entre dans l'echantillon.");
        }
    }

    @Test
    @DisplayName("En debug des details muraux, un mur expose sans element et un sous-bloc ferme par un gap restent des murs")
    void debugWallPropsLeavesPlainWallsAlone() {
        LevelZeroBlockWriter writer = new LevelZeroBlockWriter(LevelZeroBlockPalette.fromRegistry(), false, true);
        LevelZeroVerticalSlice slice = LevelZeroVerticalSlice.of(0);
        boolean plainChecked = false;
        boolean gapChecked = false;
        // Sur cette seed, le premier sous-bloc ferme sous un element mural est dans le chunk (-9, -10).
        for (int chunkX = -10; chunkX <= 10 && !(plainChecked && gapChecked); chunkX++) {
            for (int chunkZ = -10; chunkZ <= 10 && !(plainChecked && gapChecked); chunkZ++) {
                LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, chunkZ, SEED, 0);
                LevelZeroWallSampler sampler = new LevelZeroWallSampler(layout);
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int worldX = LevelZeroCoords.chunkStart(chunkX) + localX;
                        int worldZ = LevelZeroCoords.chunkStart(chunkZ) + localZ;
                        LevelZeroCellEvaluation cell = layout.cellAt(localX, localZ);
                        if (cell.isLocallyWalkable(LevelZeroCoords.subCell(worldX), LevelZeroCoords.subCell(worldZ))) {
                            continue;
                        }
                        int exposed = sampler.exposedFaceMask(worldX, worldZ);
                        boolean plain = !plainChecked && !cell.walkable() && exposed != 0
                                && !LevelZeroWallProps.resolve(worldX, worldZ, exposed, slice).hasFixture();
                        // Un sous-bloc ferme dont la colonne aurait un element mural : seul le test de la cellule l'en prive.
                        boolean gap = !gapChecked && cell.walkable() && exposed != 0
                                && LevelZeroWallProps.resolve(worldX, worldZ, exposed, slice).hasFixture();
                        if (!plain && !gap) {
                            continue;
                        }
                        BlockState[] states = column(writer, layout, slice, worldX, worldZ);
                        for (int y = slice.airMinY(); y <= slice.airMaxY(); y++) {
                            assertTrue(states[y].isOf(ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE), "Le mur reste du papier peint en Y=" + y + ".");
                        }
                        plainChecked |= plain;
                        gapChecked |= gap;
                    }
                }
            }
        }
        assertTrue(plainChecked, "Un mur expose sans element mural existe dans l'echantillon.");
        assertTrue(gapChecked, "Un sous-bloc ferme par un gap, dans une cellule ouverte, existe dans l'echantillon.");
    }

    @Test
    @DisplayName("En debug des details muraux, une colonne ouverte ou un mur enfoui ne recoivent aucun element")
    void debugWallPropsSkipsOpenColumnsAndBuriedWalls() {
        LevelZeroBlockWriter writer = new LevelZeroBlockWriter(LevelZeroBlockPalette.fromRegistry(), false, true);
        LevelZeroVerticalSlice slice = LevelZeroVerticalSlice.of(0);
        LevelZeroLayout layout = LevelZeroLayout.generate(0, 0, SEED, 0);
        BlockState[] open = column(writer, layout, slice, 1, 1);
        for (int y = slice.airMinY(); y <= slice.airMaxY(); y++) {
            assertTrue(open[y].isAir(), "Une colonne ouverte reste de l'air en Y=" + y + ".");
        }
        boolean buriedChecked = false;
        for (int chunkX = -8; chunkX <= 8 && !buriedChecked; chunkX++) {
            for (int chunkZ = -8; chunkZ <= 8 && !buriedChecked; chunkZ++) {
                LevelZeroLayout candidate = LevelZeroLayout.generate(chunkX, chunkZ, SEED, 0);
                for (int localX = 0; localX < 16 && !buriedChecked; localX++) {
                    for (int localZ = 0; localZ < 16 && !buriedChecked; localZ++) {
                        if (candidate.cellAt(localX, localZ).walkable()) {
                            continue;
                        }
                        BlockState[] states = column(writer, candidate, slice,
                                LevelZeroCoords.chunkStart(chunkX) + localX, LevelZeroCoords.chunkStart(chunkZ) + localZ);
                        buriedChecked = states[slice.airMinY()].isOf(Blocks.BEDROCK);
                    }
                }
            }
        }
        assertTrue(buriedChecked, "Un mur sans face exposee existe et reste en bedrock, sans element de debug.");
    }
}
