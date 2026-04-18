package com.petassegang.addons.world.backrooms.level0.write;

import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import com.petassegang.addons.world.backrooms.level0.LevelZeroLayout;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroCoords;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroLayoutSampler;

/**
 * Ecrit la traduction bloc par bloc du layout du Level 0.
 */
public final class LevelZeroBlockWriter {

    private final LevelZeroResolvedColumnResolver resolvedColumnResolver;
    private final LevelZeroWriteStage columnWriteStage;

    /**
     * Construit un writer utilisant la palette fournie.
     *
     * @param blockPalette palette de blocs du Level 0
     */
    public LevelZeroBlockWriter(LevelZeroBlockPalette blockPalette) {
        this.resolvedColumnResolver = new LevelZeroResolvedColumnResolver(blockPalette);
        this.columnWriteStage = new LevelZeroCompositeWriteStage(List.of(
                new LevelZeroFoundationWriteStage(blockPalette),
                new LevelZeroFloorWriteStage(),
                new LevelZeroInteriorWriteStage(),
                new LevelZeroCeilingWriteStage()));
    }

    /**
     * Ecrit toutes les colonnes d'un chunk a partir d'un layout deja calcule.
     *
     * @param chunk chunk de sortie
     * @param layout layout local du chunk
     * @param layoutSeed seed globale du layout
     */
    public void writeChunk(Chunk chunk, LevelZeroLayout layout, long layoutSeed) {
        LevelZeroLayoutSampler sampler = new LevelZeroLayoutSampler(layoutSeed);
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        int worldMinX = LevelZeroCoords.chunkStartX(chunk.getPos().x);
        int worldMinZ = LevelZeroCoords.chunkStartZ(chunk.getPos().z);

        for (int localX = 0; localX < LevelZeroLayout.CHUNK_SIZE; localX++) {
            int worldX = worldMinX + localX;
            for (int localZ = 0; localZ < LevelZeroLayout.CHUNK_SIZE; localZ++) {
                int worldZ = worldMinZ + localZ;
                writeChunkColumn(chunk, layout, sampler, mutablePos, localX, localZ, worldX, worldZ);
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
        columnWriteStage.initializeColumnSample(states);
        LevelZeroLayoutSampler sampler = new LevelZeroLayoutSampler(layoutSeed);
        int localX = LevelZeroCoords.worldToLocalX(worldX);
        int localZ = LevelZeroCoords.worldToLocalZ(worldZ);
        LevelZeroResolvedColumn resolvedColumn = resolvedColumn(layout, sampler, localX, localZ, worldX, worldZ);
        columnWriteStage.writeColumnSample(states, resolvedColumn);
    }

    private void writeChunkColumn(Chunk chunk,
                                  LevelZeroLayout layout,
                                  LevelZeroLayoutSampler sampler,
                                  BlockPos.Mutable mutablePos,
                                  int localX,
                                  int localZ,
                                  int worldX,
                                  int worldZ) {
        LevelZeroResolvedColumn resolvedColumn = resolvedColumn(layout, sampler, localX, localZ, worldX, worldZ);
        columnWriteStage.writeChunkColumn(chunk, mutablePos, localX, localZ, resolvedColumn);
    }

    private LevelZeroResolvedColumn resolvedColumn(LevelZeroLayout layout,
                                                   LevelZeroLayoutSampler sampler,
                                                   int localX,
                                                   int localZ,
                                                   int worldX,
                                                   int worldZ) {
        return resolvedColumnResolver.resolve(layout,
                sampler,
                new LevelZeroColumnCoordinates(localX, localZ, worldX, worldZ));
    }
}
