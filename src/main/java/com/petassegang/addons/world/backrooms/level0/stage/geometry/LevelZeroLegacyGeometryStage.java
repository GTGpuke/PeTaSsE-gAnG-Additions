package com.petassegang.addons.world.backrooms.level0.stage.geometry;

import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellConnections;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryFeature;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionWalkability;
import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;

/**
 * Etape legacy de micro-anomalies geometriques rares au-dessus de la grille 3x3.
 *
 * <p>La couche repart d'une base minimale : une seule famille de gaps de
 * couloir est active, et le stage reste branche dans la pipeline afin que
 * chaque future variante puisse etre ajoutee une par une sans modifier
 * l'architecture du layout.
 *
 * <p>TODO Level 0 : continuer a reconstruire des noises geometriques plus
 * intelligents. Chaque nouvelle variante doit etre ajoutee seule, testee
 * visuellement, eviter les grandes pieces, eviter les cellules eclairees,
 * respecter les connexions de couloir et ne jamais creer de blocage ou de forme
 * flottante au milieu du passage.
 */
public final class LevelZeroLegacyGeometryStage {

    private static final int GAP_FREQUENCY = 48;

    private final boolean enabled;

    /**
     * Construit l'etape geometry legacy a partir de la config globale.
     */
    public LevelZeroLegacyGeometryStage() {
        this(ModConfig.ENABLE_LEVEL_ZERO_NOISE_GEOMETRY);
    }

    /**
     * Construit l'etape geometry legacy avec un toggle explicite.
     *
     * @param enabled {@code true} pour autoriser les futures variantes
     */
    public LevelZeroLegacyGeometryStage(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Echantillonne les features geometriques fines d'une cellule.
     *
     * @param context contexte canonique de cellule
     * @param topology topologie fine deja derivee
     * @return masque de features geometriques
     */
    public int sample(LevelZeroCellContext context, LevelZeroCellTopology topology) {
        return sample(context, topology, LevelZeroCellConnections.none());
    }

    /**
     * Echantillonne les features geometriques fines d'une cellule avec son
     * contexte de connexions.
     *
     * <p>La seule variante active est le gap de couloir : un passage droit
     * resserre a une ligne de 1 bloc de large sur 3 blocs de long. Il est
     * limite aux couloirs droits pour ne pas polluer les angles, jonctions,
     * grandes pieces ou cul-de-sac.
     *
     * @param context contexte canonique de cellule
     * @param topology topologie fine deja derivee
     * @param connectionMask sorties cardinales de la cellule
     * @return masque vide tant que les variantes ne sont pas reconstruites
     */
    public int sample(LevelZeroCellContext context, LevelZeroCellTopology topology, int connectionMask) {
        return sampleWithoutRegionalConstraints(context, topology, connectionMask);
    }

    /**
     * Echantillonne les features geometriques avec les contraintes de region.
     *
     * <p>Cette variante est celle utilisee par la pipeline complete. Elle
     * verifie que les deux cotes du passage touchent des murs et arbitre les
     * cellules voisines pour empecher deux gaps colles.
     *
     * @param context contexte canonique de cellule
     * @param topology topologie fine deja derivee
     * @param connectionMask sorties cardinales de la cellule
     * @param regionWalkability carte regionale de walkability
     * @return masque de feature valide pour la vraie region
     */
    public int sample(LevelZeroCellContext context,
                      LevelZeroCellTopology topology,
                      int connectionMask,
                      LevelZeroRegionWalkability regionWalkability) {
        int mask = sampleWithoutRegionalConstraints(context, topology, connectionMask);
        if (mask == LevelZeroGeometryMask.none()) {
            return mask;
        }
        if (!hasWallOnBothSides(context, connectionMask, regionWalkability)) {
            return LevelZeroGeometryMask.none();
        }
        if (hasWinningGapNeighbor(context, connectionMask, regionWalkability)) {
            return LevelZeroGeometryMask.none();
        }
        return mask;
    }

    private int sampleWithoutRegionalConstraints(LevelZeroCellContext context,
                                                 LevelZeroCellTopology topology,
                                                 int connectionMask) {
        if (!enabled) {
            return LevelZeroGeometryMask.none();
        }
        if (!isStraightCorridor(topology, connectionMask)) {
            return LevelZeroGeometryMask.none();
        }

        long hash = gapScore(context);
        int roll = Math.floorMod(hash, GAP_FREQUENCY);
        if (roll != 0) {
            return LevelZeroGeometryMask.none();
        }

        int lane = Math.floorMod(Long.rotateLeft(hash, 23), 3);
        LevelZeroGeometryFeature feature = lane == 0
                ? LevelZeroGeometryFeature.GAP_LEFT
                : lane == 1
                        ? LevelZeroGeometryFeature.GAP_MIDDLE
                        : LevelZeroGeometryFeature.GAP_RIGHT;
        return LevelZeroGeometryMask.with(LevelZeroGeometryMask.none(), feature);
    }

    private boolean isStraightCorridor(LevelZeroCellTopology topology, int connectionMask) {
        if (topology != LevelZeroCellTopology.CORRIDOR) {
            return false;
        }
        return connectionMask == (LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH)
                || connectionMask == (LevelZeroCellConnections.EAST | LevelZeroCellConnections.WEST);
    }

    private boolean hasWallOnBothSides(LevelZeroCellContext context,
                                       int connectionMask,
                                       LevelZeroRegionWalkability regionWalkability) {
        if (connectionMask == (LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH)) {
            return isWall(regionWalkability, context.cellX() - 1, context.cellZ())
                    && isWall(regionWalkability, context.cellX() + 1, context.cellZ());
        }
        if (connectionMask == (LevelZeroCellConnections.EAST | LevelZeroCellConnections.WEST)) {
            return isWall(regionWalkability, context.cellX(), context.cellZ() - 1)
                    && isWall(regionWalkability, context.cellX(), context.cellZ() + 1);
        }
        return false;
    }

    private boolean hasWinningGapNeighbor(LevelZeroCellContext context,
                                          int connectionMask,
                                          LevelZeroRegionWalkability regionWalkability) {
        if (connectionMask == (LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH)) {
            return neighborGapWins(context, connectionMask, regionWalkability, 0, -1)
                    || neighborGapWins(context, connectionMask, regionWalkability, 0, 1);
        }
        if (connectionMask == (LevelZeroCellConnections.EAST | LevelZeroCellConnections.WEST)) {
            return neighborGapWins(context, connectionMask, regionWalkability, -1, 0)
                    || neighborGapWins(context, connectionMask, regionWalkability, 1, 0);
        }
        return false;
    }

    private boolean neighborGapWins(LevelZeroCellContext context,
                                    int connectionMask,
                                    LevelZeroRegionWalkability regionWalkability,
                                    int offsetX,
                                    int offsetZ) {
        int neighborX = context.cellX() + offsetX;
        int neighborZ = context.cellZ() + offsetZ;
        if (!isInside(regionWalkability, neighborX, neighborZ)
                || !regionWalkability.sampleWalkableCell(neighborX, neighborZ)) {
            return false;
        }

        LevelZeroCellContext neighbor = new LevelZeroCellContext(
                neighborX,
                neighborZ,
                context.layoutSeed(),
                context.layerIndex());
        if (!hasWallOnBothSides(neighbor, connectionMask, regionWalkability)) {
            return false;
        }
        if (!hasPassageOnBothEnds(neighbor, connectionMask, regionWalkability)) {
            return false;
        }
        if (sampleWithoutRegionalConstraints(neighbor, LevelZeroCellTopology.CORRIDOR, connectionMask)
                == LevelZeroGeometryMask.none()) {
            return false;
        }

        long selfScore = gapScore(context);
        long neighborScore = gapScore(neighbor);
        if (neighborScore != selfScore) {
            return neighborScore > selfScore;
        }
        if (neighbor.cellX() != context.cellX()) {
            return neighbor.cellX() > context.cellX();
        }
        return neighbor.cellZ() > context.cellZ();
    }

    private boolean hasPassageOnBothEnds(LevelZeroCellContext context,
                                         int connectionMask,
                                         LevelZeroRegionWalkability regionWalkability) {
        if (connectionMask == (LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH)) {
            return isWalkable(regionWalkability, context.cellX(), context.cellZ() - 1)
                    && isWalkable(regionWalkability, context.cellX(), context.cellZ() + 1);
        }
        if (connectionMask == (LevelZeroCellConnections.EAST | LevelZeroCellConnections.WEST)) {
            return isWalkable(regionWalkability, context.cellX() - 1, context.cellZ())
                    && isWalkable(regionWalkability, context.cellX() + 1, context.cellZ());
        }
        return false;
    }

    private long gapScore(LevelZeroCellContext context) {
        return StageRandom.mixLegacy(
                context.layoutSeed(),
                StageRandom.Stage.NOISE_GEOMETRY,
                context.cellX(),
                context.cellZ());
    }

    private boolean isWall(LevelZeroRegionWalkability regionWalkability, int cellX, int cellZ) {
        return !isInside(regionWalkability, cellX, cellZ)
                || !regionWalkability.sampleWalkableCell(cellX, cellZ);
    }

    private boolean isWalkable(LevelZeroRegionWalkability regionWalkability, int cellX, int cellZ) {
        return isInside(regionWalkability, cellX, cellZ)
                && regionWalkability.sampleWalkableCell(cellX, cellZ);
    }

    private boolean isInside(LevelZeroRegionWalkability regionWalkability, int cellX, int cellZ) {
        return cellX >= regionWalkability.minCellX()
                && cellX <= regionWalkability.maxCellX()
                && cellZ >= regionWalkability.minCellZ()
                && cellZ <= regionWalkability.maxCellZ();
    }
}
