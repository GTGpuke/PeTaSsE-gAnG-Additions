package com.petassegang.addons;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroChunkCellWindow;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellState;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionGrid;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionLayout;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionLayoutBuilder;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionWalkability;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroSectorWalkabilitySampler;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroSectorData;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroSectorGenerator;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroLegacyRegionLayoutStage;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroLegacyLayoutPipeline;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroLegacyRegionWalkabilityStage;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroRegionContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifie la representation canonique de region pour le Level 0.
 */
@DisplayName("Region layout du Level 0")
class BackroomsLevelZeroRegionLayoutTest {

    private static final int SECTOR_COLS = 1920 / 8;
    private static final int SECTOR_ROWS = 1080 / 8;

    private static LevelZeroSectorGenerator sectorGenerator() {
        return new LevelZeroSectorGenerator(
                SECTOR_COLS,
                SECTOR_ROWS,
                SECTOR_COLS * SECTOR_ROWS,
                0.8D,
                1000,
                0.5D,
                2,
                1,
                32,
                1,
                1,
                32,
                2,
                6,
                1,
                2,
                8,
                1,
                16);
    }

    @Test
    @DisplayName("L'extraction depuis la region canonique reste identique au layout historique")
    void testRegionLayoutExtractionMatchesLegacyLayout() {
        LevelZeroRegionGrid regionGrid = new LevelZeroRegionGrid(
                998877L,
                SECTOR_COLS,
                SECTOR_ROWS,
                7,
                1024,
                new ConcurrentHashMap<Long, LevelZeroSectorData>(),
                sectorGenerator(),
                new LevelZeroLegacyLayoutPipeline(7));

        LevelZeroRegionLayout regionLayout = regionGrid.buildChunkRegionLayout(4, 7);
        LevelZeroLayout legacyLayout = LevelZeroLayout.generate(4, 7, 998877L);

        for (int localX = 0; localX < LevelZeroLayout.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < LevelZeroLayout.CHUNK_SIZE; localZ++) {
                LevelZeroCellState extractedState = regionLayout.extractChunk(4, 7).cellState(localX, localZ);
                assertEquals(legacyLayout.cellState(localX, localZ), extractedState,
                        "L'extraction via region canonique doit rester identique au layout legacy.");
            }
        }
    }

    @Test
    @DisplayName("Le builder canonique de region reste coherent avec la facade regionale")
    void testRegionLayoutBuilderMatchesRegionGrid() {
        LevelZeroLegacyLayoutPipeline pipeline = new LevelZeroLegacyLayoutPipeline(7);
        LevelZeroRegionGrid regionGrid = new LevelZeroRegionGrid(
                998877L,
                SECTOR_COLS,
                SECTOR_ROWS,
                7,
                1024,
                new ConcurrentHashMap<Long, LevelZeroSectorData>(),
                sectorGenerator(),
                pipeline);
        LevelZeroRegionLayoutBuilder builder = new LevelZeroRegionLayoutBuilder(pipeline);
        LevelZeroChunkCellWindow window = new LevelZeroChunkCellWindow(4, 7, 21, 37, 26, 42);
        LevelZeroSectorWalkabilitySampler walkabilitySampler = new LevelZeroSectorWalkabilitySampler(
                998877L,
                SECTOR_COLS,
                SECTOR_ROWS,
                1024,
                new ConcurrentHashMap<Long, LevelZeroSectorData>(),
                sectorGenerator(),
                pipeline);

        LevelZeroRegionLayout fromGrid = regionGrid.buildChunkRegionLayout(4, 7);
        LevelZeroRegionWalkability walkability = new LevelZeroLegacyRegionWalkabilityStage(walkabilitySampler)
                .sample(new LevelZeroRegionContext(window, 998877L));
        LevelZeroRegionLayout fromBuilder = builder.build(998877L, walkability);

        for (int cellX = window.minCellX(); cellX <= window.maxCellX(); cellX++) {
            for (int cellZ = window.minCellZ(); cellZ <= window.maxCellZ(); cellZ++) {
                assertEquals(fromGrid.sampleCell(cellX, cellZ), fromBuilder.sampleCell(cellX, cellZ),
                        "Le builder canonique doit produire la meme region que la facade regionale.");
            }
        }
    }

    @Test
    @DisplayName("La stage regionale legacy reste coherente avec la facade regionale")
    void testLegacyRegionStageMatchesRegionGrid() {
        LevelZeroLegacyLayoutPipeline pipeline = new LevelZeroLegacyLayoutPipeline(7);
        ConcurrentHashMap<Long, LevelZeroSectorData> sectorCache = new ConcurrentHashMap<>();
        LevelZeroRegionGrid regionGrid = new LevelZeroRegionGrid(
                998877L,
                SECTOR_COLS,
                SECTOR_ROWS,
                7,
                1024,
                sectorCache,
                sectorGenerator(),
                pipeline);
        LevelZeroLegacyRegionLayoutStage regionStage = new LevelZeroLegacyRegionLayoutStage(
                new LevelZeroRegionLayoutBuilder(pipeline),
                new LevelZeroLegacyRegionWalkabilityStage(
                        new LevelZeroSectorWalkabilitySampler(
                                998877L,
                                SECTOR_COLS,
                                SECTOR_ROWS,
                                1024,
                                sectorCache,
                                sectorGenerator(),
                                pipeline)));
        LevelZeroChunkCellWindow window = new LevelZeroChunkCellWindow(4, 7, 21, 37, 26, 42);

        LevelZeroRegionLayout fromGrid = regionGrid.buildChunkRegionLayout(4, 7);
        LevelZeroRegionLayout fromStage = regionStage.sample(new LevelZeroRegionContext(window, 998877L));

        for (int cellX = window.minCellX(); cellX <= window.maxCellX(); cellX++) {
            for (int cellZ = window.minCellZ(); cellZ <= window.maxCellZ(); cellZ++) {
                assertEquals(fromGrid.sampleCell(cellX, cellZ), fromStage.sampleCell(cellX, cellZ),
                        "La stage regionale legacy doit produire la meme region que la facade regionale.");
            }
        }
    }

    @Test
    @DisplayName("La stage regionale de walkability reste coherente avec la facade regionale")
    void testLegacyRegionWalkabilityStageMatchesRegionGrid() {
        LevelZeroLegacyLayoutPipeline pipeline = new LevelZeroLegacyLayoutPipeline(7);
        ConcurrentHashMap<Long, LevelZeroSectorData> sectorCache = new ConcurrentHashMap<>();
        LevelZeroRegionGrid regionGrid = new LevelZeroRegionGrid(
                998877L,
                SECTOR_COLS,
                SECTOR_ROWS,
                7,
                1024,
                sectorCache,
                sectorGenerator(),
                pipeline);
        LevelZeroChunkCellWindow window = new LevelZeroChunkCellWindow(4, 7, 21, 37, 26, 42);
        LevelZeroRegionWalkability walkability = new LevelZeroLegacyRegionWalkabilityStage(
                new LevelZeroSectorWalkabilitySampler(
                        998877L,
                        SECTOR_COLS,
                        SECTOR_ROWS,
                        1024,
                        sectorCache,
                        sectorGenerator(),
                        pipeline))
                .sample(new LevelZeroRegionContext(window, 998877L));

        for (int cellX = window.minCellX(); cellX <= window.maxCellX(); cellX++) {
            for (int cellZ = window.minCellZ(); cellZ <= window.maxCellZ(); cellZ++) {
                assertEquals(regionGrid.sampleWalkableCell(cellX, cellZ), walkability.sampleWalkableCell(cellX, cellZ),
                        "La stage regionale de walkability doit rester alignee avec la facade regionale.");
            }
        }
    }
}
