package com.petassegang.addons.world.backrooms.level0.write;

import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import com.petassegang.addons.debug.performance.ModPerformanceMonitor;
import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroCoords;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroVerticalSlice;
import com.petassegang.addons.world.backrooms.level0.debug.LevelZeroStructureWriteStage;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroLayoutSampler;
import com.petassegang.addons.world.backrooms.level0.write.profiling.LevelZeroProfiledWriteStage;

/**
 * Traducteur final entre layout logique et blocs poses.
 *
 * <p>Le writer ne decide pas la topologie du niveau. Il prend un layout deja
 * calcule, resout chaque colonne en contrat d'ecriture, puis applique les
 * stages dans un ordre stable :
 *
 * <ol>
 *   <li>masse structurelle ;</li>
 *   <li>sol ;</li>
 *   <li>volume interieur ;</li>
 *   <li>plafond ;</li>
 *   <li>details muraux ;</li>
 *   <li>debug optionnel ;</li>
 *   <li>lumiere.</li>
 * </ol>
 *
 * <p>C'est donc le point d'entree a lire pour comprendre comment une cellule
 * logique finit en colonnes et en blocs Minecraft.
 */
public final class LevelZeroBlockWriter {

    private static final LevelZeroVerticalSlice LEGACY_VERTICAL_SLICE =
            LevelZeroVerticalSlice.legacySingleLayer();

    private final LevelZeroResolvedColumnResolver resolvedColumnResolver;
    private final LevelZeroWriteStage columnWriteStage;

    /**
     * Construit un writer utilisant la palette fournie.
     *
     * @param blockPalette palette de blocs du Level 0
     */
    public LevelZeroBlockWriter(LevelZeroBlockPalette blockPalette) {
        this.resolvedColumnResolver = new LevelZeroResolvedColumnResolver(blockPalette);
        // L'ordre des stages est important : on construit d'abord la masse
        // structurelle de la colonne, puis les details muraux, puis le neon
        // qui peut remplacer la dalle de plafond resolue si la cellule est
        // marquee comme lighted.
        this.columnWriteStage = new LevelZeroCompositeWriteStage(List.of(
                profiledStage("level0.write_stage.foundation", new LevelZeroFoundationWriteStage(blockPalette)),
                profiledStage("level0.write_stage.floor", new LevelZeroFloorWriteStage()),
                profiledStage("level0.write_stage.interior", new LevelZeroInteriorWriteStage()),
                profiledStage("level0.write_stage.ceiling", new LevelZeroCeilingWriteStage()),
                // A reprendre Level 0 : reprendre plus tard les overlays surfaciques.
                profiledStage("level0.write_stage.wall_props", new LevelZeroWallPropWriteStage()),
                profiledStage("level0.write_stage.structures_debug", new LevelZeroStructureWriteStage()),
                profiledStage("level0.write_stage.light", new LevelZeroLightWriteStage(blockPalette))));
    }

    /**
     * Ecrit toutes les colonnes d'un chunk a partir d'un layout deja calcule.
     *
     * @param chunk chunk de sortie
     * @param layout layout local du chunk
     * @param layoutSeed seed globale du layout
     */
    public void writeChunk(Chunk chunk, LevelZeroLayout layout, long layoutSeed) {
        writeChunk(chunk, layout, layoutSeed, LEGACY_VERTICAL_SLICE);
    }

    /**
     * Ecrit toutes les colonnes d'un chunk a partir d'un layout deja calcule dans une
     * tranche verticale explicite.
     *
     * @param chunk chunk de sortie
     * @param layout layout local du chunk
     * @param layoutSeed seed globale du layout
     * @param verticalSlice tranche verticale cible
     */
    @SuppressWarnings("try")
    public void writeChunk(Chunk chunk,
                           LevelZeroLayout layout,
                           long layoutSeed,
                           LevelZeroVerticalSlice verticalSlice) {
        try (ModPerformanceMonitor.Scope ignored = ModPerformanceMonitor.scope("level0.block_writer.write_chunk")) {
            // Le sampler reconstitue les lectures globales du layout a partir de la
            // seed et du layer courant. Il permet au writer de rester purement
            // deterministe sans dependre d'un etat cache cote chunk.
            LevelZeroLayoutSampler sampler = new LevelZeroLayoutSampler(layoutSeed, verticalSlice.layerIndex());
            BlockPos.Mutable mutablePos = new BlockPos.Mutable();
            int worldMinX = LevelZeroCoords.chunkStartX(chunk.getPos().x);
            int worldMinZ = LevelZeroCoords.chunkStartZ(chunk.getPos().z);

            for (int localX = 0; localX < LevelZeroLayout.CHUNK_SIZE; localX++) {
                int worldX = worldMinX + localX;
                for (int localZ = 0; localZ < LevelZeroLayout.CHUNK_SIZE; localZ++) {
                    int worldZ = worldMinZ + localZ;
                    writeChunkColumn(chunk,
                            layout,
                            sampler,
                            mutablePos,
                            localX,
                            localZ,
                            worldX,
                            worldZ,
                            verticalSlice);
                }
            }
        }
    }

    /**
     * Ecrit un echantillon vertical de colonne pour le moteur de worldgen.
     *
     * @param states etats de la colonne a remplir
     * @param layout layout local du chunk
     * @param layoutSeed seed globale du layout
     * @param worldX coordonnee monde X
     * @param worldZ coordonnee monde Z
     */
    public void writeColumnSample(BlockState[] states,
                                  LevelZeroLayout layout,
                                  long layoutSeed,
                                  int worldX,
                                  int worldZ) {
        writeColumnSample(states, layout, layoutSeed, worldX, worldZ, LEGACY_VERTICAL_SLICE);
    }

    /**
     * Ecrit un echantillon vertical de colonne dans une tranche verticale explicite.
     *
     * @param states etats de la colonne a remplir
     * @param layout layout local du chunk
     * @param layoutSeed seed globale du layout
     * @param worldX coordonnee monde X
     * @param worldZ coordonnee monde Z
     * @param verticalSlice tranche verticale cible
     */
    public void writeColumnSample(BlockState[] states,
                                  LevelZeroLayout layout,
                                  long layoutSeed,
                                  int worldX,
                                  int worldZ,
                                  LevelZeroVerticalSlice verticalSlice) {
        writeColumnSample(states, layout, layoutSeed, worldX, worldZ, verticalSlice, true);
    }

    /**
     * Ecrit ou complete un echantillon vertical de colonne dans une tranche verticale explicite.
     *
     * @param states etats de la colonne a remplir
     * @param layout layout local du chunk
     * @param layoutSeed seed globale du layout
     * @param worldX coordonnee monde X
     * @param worldZ coordonnee monde Z
     * @param verticalSlice tranche verticale cible
     * @param initialize true pour vider l'echantillon avant ecriture
     */
    @SuppressWarnings("try")
    public void writeColumnSample(BlockState[] states,
                                  LevelZeroLayout layout,
                                  long layoutSeed,
                                  int worldX,
                                  int worldZ,
                                  LevelZeroVerticalSlice verticalSlice,
                                  boolean initialize) {
        try (ModPerformanceMonitor.Scope ignored =
                     ModPerformanceMonitor.scope("level0.block_writer.write_column_sample")) {
            if (initialize) {
                columnWriteStage.initializeColumnSample(states);
            }
            // Le chemin "column sample" doit rester aligne sur l'ecriture de chunk
            // normale : meme resolveur de colonne, meme sampler, meme verticalSlice.
            LevelZeroLayoutSampler sampler = new LevelZeroLayoutSampler(layoutSeed, verticalSlice.layerIndex());
            int localX = LevelZeroCoords.worldToLocalX(worldX);
            int localZ = LevelZeroCoords.worldToLocalZ(worldZ);
            LevelZeroResolvedColumn resolvedColumn = resolvedColumn(
                    layout,
                    sampler,
                    localX,
                    localZ,
                    worldX,
                    worldZ,
                    verticalSlice);
            columnWriteStage.writeColumnSample(states, resolvedColumn);
        }
    }

    private void writeChunkColumn(Chunk chunk,
                                  LevelZeroLayout layout,
                                  LevelZeroLayoutSampler sampler,
                                  BlockPos.Mutable mutablePos,
                                  int localX,
                                  int localZ,
                                  int worldX,
                                  int worldZ,
                                  LevelZeroVerticalSlice verticalSlice) {
        LevelZeroResolvedColumn resolvedColumn = resolvedColumn(
                layout,
                sampler,
                localX,
                localZ,
                worldX,
                worldZ,
                verticalSlice);
        columnWriteStage.writeChunkColumn(chunk, mutablePos, localX, localZ, resolvedColumn);
    }

    private LevelZeroResolvedColumn resolvedColumn(LevelZeroLayout layout,
                                                   LevelZeroLayoutSampler sampler,
                                                   int localX,
                                                   int localZ,
                                                   int worldX,
                                                   int worldZ,
                                                   LevelZeroVerticalSlice verticalSlice) {
        return resolvedColumnResolver.resolve(layout,
                sampler,
                new LevelZeroColumnCoordinates(localX, localZ, worldX, worldZ),
                verticalSlice);
    }

    private static LevelZeroWriteStage profiledStage(String sectionName, LevelZeroWriteStage delegate) {
        return new LevelZeroProfiledWriteStage(sectionName, delegate);
    }
}
