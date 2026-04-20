package com.petassegang.addons.world.backrooms.level0.write;

import java.util.function.BiFunction;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;

import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroVerticalLayout;
import com.petassegang.addons.world.backrooms.level0.debug.LevelZeroWallPropDebugPalette;

/**
 * Etape d'ecriture des petits details muraux du Level 0.
 */
public final class LevelZeroWallPropWriteStage implements LevelZeroWriteStage {

    private final boolean debugFixturesEnabled;
    private final BiFunction<LevelZeroWallFixture, Integer, BlockState> fixtureResolver;

    /**
     * Construit l'etape a partir de la config globale.
     */
    public LevelZeroWallPropWriteStage() {
        this(ModConfig.DEBUG_LEVEL_ZERO_WALL_PROPS);
    }

    /**
     * Construit l'etape avec un toggle explicite.
     *
     * @param debugFixturesEnabled {@code true} pour afficher les fixtures debug
     */
    public LevelZeroWallPropWriteStage(boolean debugFixturesEnabled) {
        this(debugFixturesEnabled, LevelZeroWallPropDebugPalette::fixture);
    }

    /**
     * Construit l'etape avec un toggle explicite et un resolveur injectable.
     *
     * <p>Ce point d'entree sert surtout aux tests unitaires pour injecter des
     * etats factices sans charger tous les blocs debug vanilla.
     *
     * @param debugFixturesEnabled {@code true} pour afficher les fixtures debug
     * @param fixtureResolver resolveur du bloc de fixture a ecrire
     */
    public LevelZeroWallPropWriteStage(boolean debugFixturesEnabled,
                                       BiFunction<LevelZeroWallFixture, Integer, BlockState> fixtureResolver) {
        this.debugFixturesEnabled = debugFixturesEnabled;
        this.fixtureResolver = fixtureResolver;
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
        // TODO Level 0: la plinthe n'est plus posee comme bloc de generation.
        // Elle est maintenant rendue cote client sur le wallpaper pour eviter
        // de remplacer le mur et de casser le hover/ciblage du bloc.
        if (debugFixturesEnabled) {
            writeFixture(chunk, mutablePos, localX, localZ, resolvedColumn);
        }
    }

    @Override
    public void writeColumnSample(BlockState[] states, LevelZeroResolvedColumn resolvedColumn) {
        // TODO Level 0: la plinthe est maintenant rendue cote client sur le mur
        // et ne doit plus ecrire de bloc dans l'echantillon vertical.
        if (debugFixturesEnabled) {
            writeFixture(states, resolvedColumn);
        }
    }

    private void writeFixture(Chunk chunk,
                              BlockPos.Mutable mutablePos,
                              int localX,
                              int localZ,
                              LevelZeroResolvedColumn resolvedColumn) {
        // Les interrupteurs et prises restent pour l'instant des fixtures debug
        // explicites. Ils sont ecrits comme blocs uniquement tant que leurs
        // vrais assets fins et leur rendu definitif ne sont pas figes.
        BlockState fixture = fixtureResolver.apply(
                resolvedColumn.wallProps().fixture(),
                resolvedColumn.wallProps().fixtureFaceMask());
        if (fixture == null || !resolvedColumn.wallProps().hasFixture()) {
            return;
        }
        chunk.setBlockState(
                mutablePos.set(localX, resolvedColumn.wallProps().fixtureY(), localZ),
                fixture,
                false);
    }

    private void writeFixture(BlockState[] states, LevelZeroResolvedColumn resolvedColumn) {
        BlockState fixture = fixtureResolver.apply(
                resolvedColumn.wallProps().fixture(),
                resolvedColumn.wallProps().fixtureFaceMask());
        if (fixture == null || !resolvedColumn.wallProps().hasFixture()) {
            return;
        }
        setColumnState(states, resolvedColumn.wallProps().fixtureY(), fixture);
    }

    private static void setColumnState(BlockState[] states, int y, BlockState state) {
        if (y >= 0 && y < states.length) {
            states[y] = state;
        }
    }
}
