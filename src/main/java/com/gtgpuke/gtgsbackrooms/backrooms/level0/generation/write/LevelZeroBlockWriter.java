package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.write;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroCoords;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroVerticalSlice;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroCellEvaluation;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroLayout;
import com.gtgpuke.gtgsbackrooms.core.ModConfig;
import com.gtgpuke.gtgsbackrooms.core.perf.ModPerformanceMonitor;

/**
 * Traduit un layout de chunk en blocs pour une slice verticale.
 *
 * <p>Pour chaque colonne : bedrock inter-layer, sol, puis soit de l'air et un
 * plafond (eventuellement un neon au centre de la cellule), soit un mur et un
 * plafond. Les murs enfouis sont en bedrock ; les murs visibles en papier
 * peint adaptatif. Le meme code sert au chunk et a l'echantillon de colonne
 * demande par le moteur, via {@link BlockSink}.
 */
public final class LevelZeroBlockWriter {

    /** Destination des blocs d'une colonne. */
    @FunctionalInterface
    public interface BlockSink {
        void set(int localX, int y, int localZ, BlockState state);
    }

    private static final int CELL_CENTER = LevelZeroCoords.CELL_SCALE / 2;

    private final LevelZeroBlockPalette palette;
    private final boolean debugGeometry;
    private final boolean debugWallProps;

    /**
     * Construit un writer avec les toggles de debug de la configuration.
     *
     * @param palette palette de blocs
     */
    public LevelZeroBlockWriter(LevelZeroBlockPalette palette) {
        this(palette, ModConfig.DEBUG_LEVEL_ZERO_MICRO_GEOMETRY, ModConfig.DEBUG_LEVEL_ZERO_WALL_PROPS);
    }

    /**
     * Construit un writer avec des toggles de debug explicites.
     *
     * @param palette palette de blocs
     * @param debugGeometry {@code true} pour colorer les cellules a micro-geometrie
     * @param debugWallProps {@code true} pour poser les interrupteurs et prises en blocs
     */
    public LevelZeroBlockWriter(LevelZeroBlockPalette palette, boolean debugGeometry, boolean debugWallProps) {
        this.palette = palette;
        this.debugGeometry = debugGeometry;
        this.debugWallProps = debugWallProps;
    }

    /** Palette utilisee par ce writer. */
    public LevelZeroBlockPalette palette() {
        return palette;
    }

    /**
     * Ecrit toutes les colonnes d'un chunk pour une slice.
     *
     * @param chunk chunk de sortie
     * @param layout layout du chunk pour ce layer
     * @param slice slice verticale du layer
     */
    @SuppressWarnings("try")
    public void writeChunk(Chunk chunk, LevelZeroLayout layout, LevelZeroVerticalSlice slice) {
        try (ModPerformanceMonitor.Scope ignored = ModPerformanceMonitor.scope("level0.write.chunk")) {
            BlockPos.Mutable pos = new BlockPos.Mutable();
            BlockSink sink = (x, y, z, state) -> chunk.setBlockState(pos.set(x, y, z), state, false);
            LevelZeroWallSampler sampler = new LevelZeroWallSampler(layout);
            for (int localX = 0; localX < LevelZeroCoords.CHUNK_SIZE; localX++) {
                for (int localZ = 0; localZ < LevelZeroCoords.CHUNK_SIZE; localZ++) {
                    writeColumn(sink, sampler, layout, slice, localX, localZ);
                }
            }
        }
    }

    /**
     * Ecrit une colonne dans un echantillon vertical (indice = Y absolu).
     *
     * @param states echantillon a completer, deja initialise a l'air
     * @param layout layout du chunk pour ce layer
     * @param slice slice verticale du layer
     * @param worldX coordonnee monde X de la colonne
     * @param worldZ coordonnee monde Z de la colonne
     */
    public void writeColumnSample(BlockState[] states, LevelZeroLayout layout, LevelZeroVerticalSlice slice,
                                  int worldX, int worldZ) {
        BlockSink sink = (x, y, z, state) -> {
            if (y >= 0 && y < states.length) {
                states[y] = state;
            }
        };
        writeColumn(sink, new LevelZeroWallSampler(layout), layout, slice,
                LevelZeroCoords.worldToLocal(worldX), LevelZeroCoords.worldToLocal(worldZ));
    }

    private void writeColumn(BlockSink sink, LevelZeroWallSampler sampler, LevelZeroLayout layout,
                             LevelZeroVerticalSlice slice, int localX, int localZ) {
        int worldX = LevelZeroCoords.chunkStart(layout.chunkX()) + localX;
        int worldZ = LevelZeroCoords.chunkStart(layout.chunkZ()) + localZ;
        LevelZeroCellEvaluation cell = layout.cellAt(localX, localZ);
        int subCellX = LevelZeroCoords.subCell(worldX);
        int subCellZ = LevelZeroCoords.subCell(worldZ);

        for (int y = slice.bedrockMinY(); y <= slice.bedrockMaxY(); y++) {
            sink.set(localX, y, localZ, palette.bedrock());
        }
        BlockState floor = debugGeometry && cell.hasGeometryFeature()
                ? LevelZeroDebugPalette.gapFloor()
                : palette.floor(cell.surfaceBiome());
        sink.set(localX, slice.floorY(), localZ, floor);

        if (cell.isLocallyWalkable(subCellX, subCellZ)) {
            boolean lit = cell.lighted() && subCellX == CELL_CENTER && subCellZ == CELL_CENTER;
            sink.set(localX, slice.ceilingY(), localZ, lit ? palette.light() : palette.ceilingTile());
            return;
        }

        int exposedFaceMask = sampler.exposedFaceMask(worldX, worldZ);
        BlockState wall;
        if (debugGeometry && cell.walkable()) {
            // Cellule ouverte dont ce bloc est ferme par la micro-geometrie.
            wall = LevelZeroDebugPalette.gapWall();
        } else if (exposedFaceMask == 0) {
            wall = palette.bedrock();
        } else {
            wall = palette.wallpaper(sampler.wallpaperFaceMask(worldX, worldZ));
        }
        for (int y = slice.airMinY(); y <= slice.airMaxY(); y++) {
            sink.set(localX, y, localZ, wall);
        }
        sink.set(localX, slice.ceilingY(), localZ, palette.ceilingTile());

        if (debugWallProps && !cell.walkable() && exposedFaceMask != 0) {
            LevelZeroWallProps props = LevelZeroWallProps.resolve(worldX, worldZ, exposedFaceMask, slice);
            // Un element mural (interrupteur, prise) a toujours son bloc de debug ; sans element, rien a poser.
            if (props.hasFixture()) {
                sink.set(localX, props.fixtureY(), localZ, LevelZeroDebugPalette.fixture(props.fixture(), props.fixtureFaceMask()));
            }
        }
    }
}
