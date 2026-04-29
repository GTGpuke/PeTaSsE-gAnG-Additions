package com.petassegang.addons.backrooms.level.level0.generation.debug;

import java.util.function.Function;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroResolvedColumn;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroWriteStage;
import com.petassegang.addons.backrooms.level.level0.generation.write.structure.LevelZeroStructureProfile;

/**
 * Etape d'ecriture debug des structures rares du Level 0.
 *
 * <p>Cette etape reste purement visuelle et optionnelle. Elle ne doit jamais
 * s'activer dans le rendu normal.
 *
 * <p>A reprendre Level 0 : reprendre plus tard un vrai systeme de structures faites
 * a la main, chargees depuis des definitions dediees, separe du traitement des
 * grandes rooms et sans casser le layout.
 */
public final class LevelZeroStructureWriteStage implements LevelZeroWriteStage {

    private final boolean debugStructuresEnabled;
    private final Function<LevelZeroStructureProfile, BlockState> markerResolver;

    /**
     * Construit l'etape a partir de la config globale.
     */
    public LevelZeroStructureWriteStage() {
        this(ModConfig.DEBUG_LEVEL_ZERO_STRUCTURES);
    }

    /**
     * Construit l'etape avec un toggle explicite.
     *
     * @param debugStructuresEnabled {@code true} pour afficher les structures rares
     */
    public LevelZeroStructureWriteStage(boolean debugStructuresEnabled) {
        this(debugStructuresEnabled, LevelZeroStructureWriteStage::markerFor);
    }

    /**
     * Construit l'etape avec un toggle explicite et un resolveur injectable.
     *
     * <p>Ce point d'entree existe surtout pour les tests unitaires afin
     * d'eviter de depender du registre complet des blocs vanilla.
     *
     * @param debugStructuresEnabled {@code true} pour afficher les structures rares
     * @param markerResolver resolveur du bloc de debug a ecrire
     */
    public LevelZeroStructureWriteStage(boolean debugStructuresEnabled,
                                        Function<LevelZeroStructureProfile, BlockState> markerResolver) {
        this.debugStructuresEnabled = debugStructuresEnabled;
        this.markerResolver = markerResolver;
    }

    @Override
    public void initializeColumnSample(BlockState[] states) {
    }

    @Override
    public void writeChunkColumn(Chunk chunk,
                                 BlockPos.Mutable mutablePos,
                                 int localX,
                                 int localZ,
                                 LevelZeroResolvedColumn resolvedColumn) {
        if (!debugStructuresEnabled) {
            return;
        }

        // Cette etape n'ecrit jamais de vraie structure gameplay. Elle ne fait
        // que visualiser la couche semantique actuelle pour aider a verifier
        // les footprints et points de gameplay potentiels.
        BlockState marker = markerResolver.apply(resolvedColumn.structure());
        if (marker == null) {
            return;
        }

        chunk.setBlockState(
                mutablePos.set(localX, resolvedColumn.verticalSlice().airMinY(), localZ),
                marker,
                false);
    }

    @Override
    public void writeColumnSample(BlockState[] states, LevelZeroResolvedColumn resolvedColumn) {
        if (!debugStructuresEnabled) {
            return;
        }

        // Le chemin "column sample" reste aligne sur l'ecriture debug du chunk
        // pour que heightmap, debug et worldgen lisent la meme visualisation.
        BlockState marker = markerResolver.apply(resolvedColumn.structure());
        if (marker == null) {
            return;
        }

        int y = resolvedColumn.verticalSlice().airMinY();
        if (y >= 0 && y < states.length) {
            states[y] = marker;
        }
    }

    private static BlockState markerFor(LevelZeroStructureProfile structure) {
        if (!structure.hasStructure()) {
            return null;
        }
        if (structure.hasGameplayPoint()) {
            return LevelZeroStructureDebugPalette.gameplayPoint(structure.gameplayPointKind());
        }
        return LevelZeroStructureDebugPalette.structureRole(structure.role());
    }
}
