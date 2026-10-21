package com.gtgpuke.gtgsbackrooms.backrooms.level0.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.biome.LevelZeroSurfaceBiome;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroCoords;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;
import com.gtgpuke.gtgsbackrooms.core.registry.ModBlocks;

@DisplayName("Mur adaptatif du Level 0")
class LevelZeroWallpaperBlockTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    private static BlockView view(Map<BlockPos, BlockState> blocks) {
        return new BlockView() {
            @Override
            public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
                return null;
            }

            @Override
            public BlockState getBlockState(BlockPos pos) {
                return blocks.getOrDefault(pos, Blocks.AIR.getDefaultState());
            }

            @Override
            public FluidState getFluidState(BlockPos pos) {
                return Fluids.EMPTY.getDefaultState();
            }

            @Override
            public int getHeight() {
                return LevelZeroLayerStackLayout.DIMENSION_HEIGHT;
            }

            @Override
            public int getBottomY() {
                return LevelZeroLayerStackLayout.MIN_Y;
            }
        };
    }

    @Test
    @DisplayName("Le bloc expose la propriete face_mask sur 16 variantes")
    void stateProperty() {
        BlockState state = ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE.getDefaultState();
        assertEquals(0, state.get(LevelZeroWallpaperBlock.FACE_MASK), "Masque nul par defaut.");
        assertEquals(16, ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE.getStateManager().getStates().size(), "Seize variantes.");
        assertEquals(LevelZeroWallpaperBlock.NORTH_MASK, LevelZeroWallpaperBlock.maskFor(Direction.NORTH), "Nord.");
        assertEquals(LevelZeroWallpaperBlock.EAST_MASK, LevelZeroWallpaperBlock.maskFor(Direction.EAST), "Est.");
        assertEquals(0, LevelZeroWallpaperBlock.maskFor(Direction.UP), "Pas de bit vertical.");
    }

    @Test
    @DisplayName("Au layer 0, un mur entoure d'air reste en papier peint jauni")
    void layerZeroIsAlwaysBase() {
        int mask = LevelZeroWallpaperBlock.sampleFaceMask(view(new HashMap<>()), new BlockPos(500, 2, -300));
        assertEquals(0, mask, "Le layer 0 n'a jamais de biome rouge.");
    }

    @Test
    @DisplayName("Dans une region rouge du layer 1, toutes les faces exposees passent en blanc")
    void redRegionTurnsFacesWhite() {
        int regionX = -64;
        int regionZ = 0;
        for (; regionX <= 64; regionX++) {
            if (LevelZeroSurfaceBiome.sampleAtRegion(regionX, regionZ, 1) == LevelZeroSurfaceBiome.RED) {
                break;
            }
        }
        assertTrue(regionX <= 64, "Une region rouge existe dans l'echantillon.");
        int worldX = LevelZeroCoords.cellToWorldMin(regionX * LevelZeroSurfaceBiome.REGION_SIZE_CELLS + 24);
        int worldZ = LevelZeroCoords.cellToWorldMin(regionZ * LevelZeroSurfaceBiome.REGION_SIZE_CELLS + 24);
        BlockPos pos = new BlockPos(worldX, LevelZeroLayerStackLayout.airMinY(1), worldZ);
        assertEquals(LevelZeroWallpaperBlock.FULL_MASK, LevelZeroWallpaperBlock.sampleFaceMask(view(new HashMap<>()), pos),
                "Toutes les faces voient le biome rouge.");

        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (int distance = 1; distance <= LevelZeroWallpaperBlock.MAX_SURFACE_PROBE_DISTANCE; distance++) {
            blocks.put(pos.north(distance), Blocks.STONE.getDefaultState());
        }
        assertEquals(LevelZeroWallpaperBlock.FULL_MASK, LevelZeroWallpaperBlock.sampleFaceMask(view(blocks), pos),
                "Sans air au nord, le biome de repli reste celui de la region.");
    }

    @Test
    @DisplayName("Hors de la pile verticale, le mur est evalue comme au layer 0")
    void outsideStackIsLayerZero() {
        int mask = LevelZeroWallpaperBlock.sampleFaceMask(view(new HashMap<>()), new BlockPos(500, LevelZeroLayerStackLayout.MIN_Y - 10, -300));
        assertEquals(0, mask, "Sous la pile, pas de biome rouge.");
    }
}
