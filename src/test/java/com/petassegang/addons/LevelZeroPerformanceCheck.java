package com.petassegang.addons;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.petassegang.addons.backrooms.BackroomsConstants;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroLayout;
import com.petassegang.addons.backrooms.level.level0.biome.LevelZeroSurfaceBiome;
import com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroCoords;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellState;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellTopology;
import com.petassegang.addons.backrooms.level.level0.generation.write.structure.LevelZeroStructureProfile;
import com.petassegang.addons.backrooms.level.level0.generation.write.structure.LevelZeroStructureResolver;

/**
 * Benchmark local et deterministe du pipeline de generation du Level 0.
 *
 * <p>Cette classe n'est pas un test unitaire. Elle sert a comparer rapidement
 * le cout relatif de la generation d'une meme zone entre plusieurs revisions
 * du code, afin de detecter les regressions avant d'empiler plusieurs couches
 * de generation Backrooms.
 */
public final class LevelZeroPerformanceCheck {

    private static final long[] SAMPLE_SEEDS = {
            12345L,
            998877L,
            13579L,
            24680L,
            424242L
    };
    private static final int CHUNK_RADIUS = 6;
    private static final int WARMUP_PASSES = 3;
    private static final int MEASURED_PASSES = 5;
    private static final int NORTH_MASK = 1;
    private static final int SOUTH_MASK = 1 << 1;
    private static final int WEST_MASK = 1 << 2;
    private static final int EAST_MASK = 1 << 3;
    private static final int FULL_MASK = NORTH_MASK | SOUTH_MASK | WEST_MASK | EAST_MASK;

    private LevelZeroPerformanceCheck() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Lance le benchmark local du Level 0.
     *
     * @param args arguments optionnels, non utilises
     */
    public static void main(String[] args) {
        for (int pass = 0; pass < WARMUP_PASSES; pass++) {
            runScenario(false);
        }

        ScenarioStats total = new ScenarioStats();
        long bestDurationNs = Long.MAX_VALUE;
        long worstDurationNs = Long.MIN_VALUE;

        for (int pass = 0; pass < MEASURED_PASSES; pass++) {
            ScenarioStats current = runScenario(false);
            total.add(current);
            bestDurationNs = Math.min(bestDurationNs, current.durationNs);
            worstDurationNs = Math.max(worstDurationNs, current.durationNs);
        }

        double averageDurationMs = nanosToMillis(total.durationNs / (double) MEASURED_PASSES);
        double averageChunkDurationMs = averageDurationMs / total.generatedChunks * MEASURED_PASSES;
        double bestDurationMs = nanosToMillis(bestDurationNs);
        double worstDurationMs = nanosToMillis(worstDurationNs);
        double mixedColumnRatio = total.mixedColumns * 100.0D / total.wallColumns;
        double corridorFeatureRatio = percent(total.corridorFeatureCells, total.corridorCells);
        double deadEndFeatureRatio = percent(total.deadEndFeatureCells, total.deadEndCells);

        System.out.println("=== Benchmark Level 0 ===");
        System.out.printf(Locale.ROOT, "Passes mesurees : %d%n", MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Chunks analyses : %d%n", total.generatedChunks / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Temps moyen total : %.3f ms%n", averageDurationMs);
        System.out.printf(Locale.ROOT, "Temps moyen par chunk : %.6f ms%n", averageChunkDurationMs);
        System.out.printf(Locale.ROOT, "Meilleur passage : %.3f ms%n", bestDurationMs);
        System.out.printf(Locale.ROOT, "Pire passage : %.3f ms%n", worstDurationMs);
        System.out.printf(Locale.ROOT, "Colonnes de murs : %d%n", total.wallColumns / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Colonnes exposees : %d%n", total.exposedColumns / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Colonnes mixtes : %d (%.2f%% des murs)%n",
                total.mixedColumns / MEASURED_PASSES,
                mixedColumnRatio);
        System.out.printf(Locale.ROOT, "Sondes de faces : %d%n", total.faceSamples / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Cellules logiques : %d%n", total.logicalCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Corridors : %d%n", total.corridorCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Angles : %d%n", total.angleCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "T-junctions : %d%n", total.tJunctionCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Crossroads : %d%n", total.crossroadCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Jonctions legacy : %d%n", total.junctionCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Dead ends : %d%n", total.deadEndCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Grandes pieces : %d%n", total.largeRoomCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Rooms rect legacy : %d%n", total.rectRoomCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Rooms piliers legacy : %d%n", total.pillarRoomCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Rooms custom legacy : %d%n", total.customRoomCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Rooms rect walkable : %d%n", total.rectRoomWalkableCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Rooms piliers walkable : %d%n", total.pillarRoomWalkableCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Rooms custom walkable : %d%n", total.customRoomWalkableCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Features sur corridors : %d (%.2f%%)%n",
                total.corridorFeatureCells / MEASURED_PASSES,
                corridorFeatureRatio);
        System.out.printf(Locale.ROOT, "Features sur dead ends : %d (%.2f%%)%n",
                total.deadEndFeatureCells / MEASURED_PASSES,
                deadEndFeatureRatio);
        System.out.printf(Locale.ROOT, "Offsets : %d%n", total.offsetWalls / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Recess : %d%n", total.recesses / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Alcoves : %d%n", total.alcoves / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Half-walls : %d%n", total.halfWalls / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Pinch 1-wide : %d%n", total.pinches / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Structures rares : %d%n", total.rareStructures / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Structure cells : %d%n", total.structureCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Storage clusters : %d%n", total.storageClusters / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Office remains : %d%n", total.officeRemains / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Pillar rings : %d%n", total.pillarRings / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Structure anchors : %d%n", total.structureAnchorCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Structure edges : %d%n", total.structureEdgeCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Structure centers : %d%n", total.structureCenterCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Structure interiors : %d%n", total.structureInteriorCells / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Gameplay points structures : %d%n", total.structureGameplayPoints / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Gameplay entry : %d%n", total.structureGameplayEntries / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Gameplay focal : %d%n", total.structureGameplayFocalPoints / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Gameplay loot : %d%n", total.structureGameplayLootHints / MEASURED_PASSES);
        System.out.printf(Locale.ROOT, "Gameplay utility : %d%n", total.structureGameplayUtilityHints / MEASURED_PASSES);

        String budgetProperty = System.getProperty("levelZeroPerfBudgetMsPerChunk");
        if (budgetProperty == null || budgetProperty.isBlank()) {
            return;
        }

        double budgetMsPerChunk = Double.parseDouble(budgetProperty);
        if (averageChunkDurationMs > budgetMsPerChunk) {
            throw new IllegalStateException(String.format(
                    Locale.ROOT,
                    "Le benchmark du Level 0 depasse le budget autorise : %.6f ms/chunk > %.6f ms/chunk.",
                    averageChunkDurationMs,
                    budgetMsPerChunk));
        }
    }

    private static ScenarioStats runScenario(boolean ignored) {
        ScenarioStats stats = new ScenarioStats();
        long startNs = System.nanoTime();

        for (long layoutSeed : SAMPLE_SEEDS) {
            LayoutSampler sampler = new LayoutSampler(layoutSeed);
            for (int chunkX = -CHUNK_RADIUS; chunkX <= CHUNK_RADIUS; chunkX++) {
                for (int chunkZ = -CHUNK_RADIUS; chunkZ <= CHUNK_RADIUS; chunkZ++) {
                    LevelZeroLayout layout = LevelZeroLayout.generate(chunkX, chunkZ, layoutSeed);
                    scanChunk(layout, sampler, chunkX, chunkZ, stats);
                    stats.generatedChunks++;
                }
            }
        }

        stats.durationNs = System.nanoTime() - startNs;
        return stats;
    }

    private static void scanChunk(LevelZeroLayout layout,
                                  LayoutSampler sampler,
                                  int chunkX,
                                  int chunkZ,
                                  ScenarioStats stats) {
        int worldMinX = chunkX * LevelZeroLayout.CHUNK_SIZE;
        int worldMinZ = chunkZ * LevelZeroLayout.CHUNK_SIZE;
        Set<Long> visitedCells = new HashSet<>();

        for (int localX = 0; localX < LevelZeroLayout.CHUNK_SIZE; localX++) {
            int worldX = worldMinX + localX;
            for (int localZ = 0; localZ < LevelZeroLayout.CHUNK_SIZE; localZ++) {
                int worldZ = worldMinZ + localZ;
                recordLogicalCell(layout, localX, localZ, worldX, worldZ, visitedCells, stats);

                if (layout.isWalkable(localX, localZ)) {
                    continue;
                }

                stats.wallColumns++;
                if (!sampler.isWallpaperExposed(worldX, worldZ)) {
                    continue;
                }

                stats.exposedColumns++;
                int faceMask = sampler.sampleWallpaperFaceMask(worldX, worldZ, stats);
                if (faceMask != 0 && faceMask != FULL_MASK) {
                    stats.mixedColumns++;
                }
            }
        }
    }

    private static void recordLogicalCell(LevelZeroLayout layout,
                                          int localX,
                                          int localZ,
                                          int worldX,
                                          int worldZ,
                                          Set<Long> visitedCells,
                                          ScenarioStats stats) {
        long cellKey = packCell(LevelZeroCoords.worldToCellX(worldX), LevelZeroCoords.worldToCellZ(worldZ));
        if (!visitedCells.add(cellKey)) {
            return;
        }

        LevelZeroCellState state = layout.cellState(localX, localZ);
        stats.logicalCells++;
        switch (state.topology()) {
            case CORRIDOR -> stats.corridorCells++;
            case ANGLE -> stats.angleCells++;
            case T_JUNCTION -> stats.tJunctionCells++;
            case CROSSROAD -> stats.crossroadCells++;
            case JUNCTION -> stats.junctionCells++;
            case DEAD_END -> stats.deadEndCells++;
            case ROOM_LARGE -> stats.largeRoomCells++;
            default -> {
            }
        }
        switch (state.roomKind()) {
            case RECT_ROOM -> {
                stats.rectRoomCells++;
                if (state.walkable()) {
                    stats.rectRoomWalkableCells++;
                }
            }
            case PILLAR_ROOM -> {
                stats.pillarRoomCells++;
                if (state.walkable()) {
                    stats.pillarRoomWalkableCells++;
                }
            }
            case CUSTOM_ROOM -> {
                stats.customRoomCells++;
                if (state.walkable()) {
                    stats.customRoomWalkableCells++;
                }
            }
            default -> {
            }
        }

        boolean hasFeature = state.geometryMask() != 0;

        if (hasFeature) {
            if (state.topology() == LevelZeroCellTopology.CORRIDOR) {
                stats.corridorFeatureCells++;
            } else if (state.topology() == LevelZeroCellTopology.DEAD_END) {
                stats.deadEndFeatureCells++;
            }
        }

        LevelZeroStructureProfile structure = StructureSampler.RESOLVER.resolve(
                state.roomKind(),
                state.walkable(),
                worldX,
                worldZ);
        if (!structure.hasStructure()) {
            return;
        }

        stats.structureCells++;
        switch (structure.kind()) {
            case STORAGE_CLUSTER -> stats.storageClusters++;
            case OFFICE_REMAINS -> stats.officeRemains++;
            case PILLAR_RING -> stats.pillarRings++;
            default -> {
            }
        }
        switch (structure.role()) {
            case ANCHOR -> stats.structureAnchorCells++;
            case EDGE -> stats.structureEdgeCells++;
            case CENTER -> stats.structureCenterCells++;
            case INTERIOR -> stats.structureInteriorCells++;
            default -> {
            }
        }
        switch (structure.gameplayPointKind()) {
            case ENTRY -> {
                stats.structureGameplayPoints++;
                stats.structureGameplayEntries++;
            }
            case FOCAL_POINT -> {
                stats.structureGameplayPoints++;
                stats.structureGameplayFocalPoints++;
            }
            case LOOT_HINT -> {
                stats.structureGameplayPoints++;
                stats.structureGameplayLootHints++;
            }
            case UTILITY_HINT -> {
                stats.structureGameplayPoints++;
                stats.structureGameplayUtilityHints++;
            }
            default -> {
            }
        }

        long structureAnchorKey = packCell(structure.anchorCellX(), structure.anchorCellZ())
                ^ (((long) structure.kind().ordinal()) << 48);
        if (stats.seenStructureAnchors.add(structureAnchorKey)) {
            stats.rareStructures++;
        }
    }

    private static double nanosToMillis(double nanos) {
        return nanos / 1_000_000.0D;
    }

    private static double percent(int value, int total) {
        if (total == 0) {
            return 0.0D;
        }
        return value * 100.0D / total;
    }

    private static long packCell(int cellX, int cellZ) {
        return (((long) cellX) << 32) ^ (cellZ & 0xffffffffL);
    }

    private static final class ScenarioStats {

        private long durationNs;
        private int generatedChunks;
        private int wallColumns;
        private int exposedColumns;
        private int mixedColumns;
        private int faceSamples;
        private int logicalCells;
        private int corridorCells;
        private int angleCells;
        private int tJunctionCells;
        private int crossroadCells;
        private int junctionCells;
        private int deadEndCells;
        private int largeRoomCells;
        private int rectRoomCells;
        private int pillarRoomCells;
        private int customRoomCells;
        private int rectRoomWalkableCells;
        private int pillarRoomWalkableCells;
        private int customRoomWalkableCells;
        private int corridorFeatureCells;
        private int deadEndFeatureCells;
        private int offsetWalls;
        private int recesses;
        private int alcoves;
        private int halfWalls;
        private int pinches;
        private int rareStructures;
        private int structureCells;
        private int storageClusters;
        private int officeRemains;
        private int pillarRings;
        private int structureAnchorCells;
        private int structureEdgeCells;
        private int structureCenterCells;
        private int structureInteriorCells;
        private int structureGameplayPoints;
        private int structureGameplayEntries;
        private int structureGameplayFocalPoints;
        private int structureGameplayLootHints;
        private int structureGameplayUtilityHints;
        private final Set<Long> seenStructureAnchors = new HashSet<>();

        private void add(ScenarioStats other) {
            durationNs += other.durationNs;
            generatedChunks += other.generatedChunks;
            wallColumns += other.wallColumns;
            exposedColumns += other.exposedColumns;
            mixedColumns += other.mixedColumns;
            faceSamples += other.faceSamples;
            logicalCells += other.logicalCells;
            corridorCells += other.corridorCells;
            angleCells += other.angleCells;
            tJunctionCells += other.tJunctionCells;
            crossroadCells += other.crossroadCells;
            junctionCells += other.junctionCells;
            deadEndCells += other.deadEndCells;
            largeRoomCells += other.largeRoomCells;
            rectRoomCells += other.rectRoomCells;
            pillarRoomCells += other.pillarRoomCells;
            customRoomCells += other.customRoomCells;
            rectRoomWalkableCells += other.rectRoomWalkableCells;
            pillarRoomWalkableCells += other.pillarRoomWalkableCells;
            customRoomWalkableCells += other.customRoomWalkableCells;
            corridorFeatureCells += other.corridorFeatureCells;
            deadEndFeatureCells += other.deadEndFeatureCells;
            offsetWalls += other.offsetWalls;
            recesses += other.recesses;
            alcoves += other.alcoves;
            halfWalls += other.halfWalls;
            pinches += other.pinches;
            rareStructures += other.rareStructures;
            structureCells += other.structureCells;
            storageClusters += other.storageClusters;
            officeRemains += other.officeRemains;
            pillarRings += other.pillarRings;
            structureAnchorCells += other.structureAnchorCells;
            structureEdgeCells += other.structureEdgeCells;
            structureCenterCells += other.structureCenterCells;
            structureInteriorCells += other.structureInteriorCells;
            structureGameplayPoints += other.structureGameplayPoints;
            structureGameplayEntries += other.structureGameplayEntries;
            structureGameplayFocalPoints += other.structureGameplayFocalPoints;
            structureGameplayLootHints += other.structureGameplayLootHints;
            structureGameplayUtilityHints += other.structureGameplayUtilityHints;
        }
    }

    private static final class StructureSampler {

        private static final LevelZeroStructureResolver RESOLVER = new LevelZeroStructureResolver();

        private StructureSampler() {
        }
    }

    private static final class LayoutSampler {

        private final long layoutSeed;

        private LayoutSampler(long layoutSeed) {
            this.layoutSeed = layoutSeed;
        }

        private boolean isWallpaperExposed(int worldX, int worldZ) {
            return isWalkableAt(worldX + 1, worldZ)
                    || isWalkableAt(worldX - 1, worldZ)
                    || isWalkableAt(worldX, worldZ + 1)
                    || isWalkableAt(worldX, worldZ - 1);
        }

        private int sampleWallpaperFaceMask(int worldX, int worldZ, ScenarioStats stats) {
            int faceMask = 0;
            faceMask |= sampleFace(worldX, worldZ, 0, -1, NORTH_MASK, stats);
            faceMask |= sampleFace(worldX, worldZ, 0, 1, SOUTH_MASK, stats);
            faceMask |= sampleFace(worldX, worldZ, -1, 0, WEST_MASK, stats);
            faceMask |= sampleFace(worldX, worldZ, 1, 0, EAST_MASK, stats);
            return faceMask;
        }

        private int sampleFace(int worldX,
                               int worldZ,
                               int stepX,
                               int stepZ,
                               int maskBit,
                               ScenarioStats stats) {
            for (int distance = 1; distance <= BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 4; distance++) {
                int sampleX = worldX + stepX * distance;
                int sampleZ = worldZ + stepZ * distance;
                stats.faceSamples++;
                if (!isWalkableAt(sampleX, sampleZ)) {
                    continue;
                }
                return LevelZeroSurfaceBiome.sampleAtWorld(sampleX, sampleZ) == LevelZeroSurfaceBiome.RED ? maskBit : 0;
            }

            int fallbackX = worldX + stepX * BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 2;
            int fallbackZ = worldZ + stepZ * BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 2;
            stats.faceSamples++;
            return LevelZeroSurfaceBiome.sampleAtWorld(fallbackX, fallbackZ) == LevelZeroSurfaceBiome.RED ? maskBit : 0;
        }

        private boolean isWalkableAt(int worldX, int worldZ) {
            return LevelZeroLayout.isWalkableAtWorld(worldX, worldZ, layoutSeed);
        }
    }
}
