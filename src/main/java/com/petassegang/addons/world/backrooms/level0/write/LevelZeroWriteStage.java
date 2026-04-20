package com.petassegang.addons.world.backrooms.level0.write;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

/**
 * Contrat commun des stages d'ecriture du Level 0.
 *
 * <p>Chaque stage recoit une colonne deja resolue et se contente d'ecrire sa
 * couche : fondation, sol, interieur, plafond, lumiere, etc. Les decisions de
 * layout doivent avoir ete prises avant d'arriver ici.
 */
public interface LevelZeroWriteStage {

    /**
     * Initialise un echantillon vertical avant l'ecriture d'une colonne.
     *
     * @param states etats de colonne a initialiser
     */
    void initializeColumnSample(BlockState[] states);

    /**
     * Ecrit une colonne resolue dans un chunk.
     *
     * @param chunk chunk cible
     * @param mutablePos position mutable reutilisable
     * @param localX coordonnee locale X
     * @param localZ coordonnee locale Z
     * @param resolvedColumn colonne completement resolue
     */
    void writeChunkColumn(Chunk chunk,
                          BlockPos.Mutable mutablePos,
                          int localX,
                          int localZ,
                          LevelZeroResolvedColumn resolvedColumn);

    /**
     * Ecrit un echantillon vertical de colonne.
     *
     * @param states etats de colonne a remplir
     * @param resolvedColumn colonne completement resolue
     */
    void writeColumnSample(BlockState[] states, LevelZeroResolvedColumn resolvedColumn);
}
