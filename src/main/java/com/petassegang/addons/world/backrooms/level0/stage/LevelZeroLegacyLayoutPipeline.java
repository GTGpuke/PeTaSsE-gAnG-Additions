package com.petassegang.addons.world.backrooms.level0.stage;

import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionWalkability;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.sector.LevelZeroSectorRoomKind;
import com.petassegang.addons.world.backrooms.level0.stage.biome.LevelZeroSurfaceBiomeStage;
import com.petassegang.addons.world.backrooms.level0.stage.geometry.LevelZeroLegacyGeometryStage;
import com.petassegang.addons.world.backrooms.level0.stage.geometry.LevelZeroLegacyMicroPatternStage;
import com.petassegang.addons.world.backrooms.level0.stage.light.LevelZeroLightStage;
import com.petassegang.addons.world.backrooms.level0.stage.region.LevelZeroSectorCacheKeyStage;
import com.petassegang.addons.world.backrooms.level0.stage.topology.LevelZeroLargeRoomStage;
import com.petassegang.addons.world.backrooms.level0.stage.topology.LevelZeroLegacyTopologyStage;

/**
 * Pipeline explicite du coeur historique de generation du Level 0.
 *
 * <p>Ici, {@code Legacy} ne veut pas dire {@code obsolete}. Le terme designe
 * la grammaire stable et validee du Level 0 telle qu'elle existe aujourd'hui :
 * walkability, salles, topologie, motifs, lumiere et micro-geometrie derives
 * a partir de la base historique du projet.
 *
 * <p>Cette pipeline est donc a considerer comme le coeur de generation qui ne
 * doit pas bouger silencieusement. Les evolutions autour d'elle doivent etre
 * explicites, mesurees et compatibles avec ce socle.
 */
public final class LevelZeroLegacyLayoutPipeline {

    private final LevelZeroLightStage lightStage;
    private final LevelZeroSurfaceBiomeStage surfaceBiomeStage;
    private final LevelZeroLargeRoomStage largeRoomStage;
    private final LevelZeroSectorCacheKeyStage sectorCacheKeyStage;
    private final LevelZeroLegacyTopologyStage topologyStage;
    private final LevelZeroLegacyGeometryStage geometryStage;
    private final LevelZeroLegacyMicroPatternStage microPatternStage;

    /**
     * Construit la pipeline legacy a partir du modulo historique des neons.
     *
     * @param lightInterval modulo historique des neons
     */
    public LevelZeroLegacyLayoutPipeline(int lightInterval) {
        this(lightInterval, true);
    }

    /**
     * Construit la pipeline legacy avec controle explicite de la
     * micro-geometrie.
     *
     * @param lightInterval modulo historique des neons
     * @param noiseGeometryEnabled {@code true} pour activer la micro-geometrie
     */
    public LevelZeroLegacyLayoutPipeline(int lightInterval, boolean noiseGeometryEnabled) {
        this.lightStage = new LevelZeroLightStage(lightInterval);
        this.surfaceBiomeStage = new LevelZeroSurfaceBiomeStage();
        this.largeRoomStage = new LevelZeroLargeRoomStage();
        this.sectorCacheKeyStage = new LevelZeroSectorCacheKeyStage();
        this.topologyStage = new LevelZeroLegacyTopologyStage();
        this.geometryStage = new LevelZeroLegacyGeometryStage(noiseGeometryEnabled);
        this.microPatternStage = new LevelZeroLegacyMicroPatternStage();
    }

    /**
     * Echantillonne la presence historique d'un neon.
     *
     * @param context contexte canonique de cellule
     * @return {@code true} si la cellule porte un neon
     */
    public boolean sampleLight(LevelZeroCellContext context) {
        return sampleLight(
                context,
                sampleSurfaceBiome(context),
                sampleLargeRoom(context),
                LevelZeroSectorRoomKind.NONE);
    }

    /**
     * Echantillonne la presence d'un neon a partir du biome et du statut
     * de grande piece de la cellule.
     *
     * @param context contexte canonique de cellule
     * @param surfaceBiome biome cosmetique de surface
     * @param largeRoom {@code true} si la cellule appartient a une grande piece
     * @return {@code true} si la cellule porte un neon
     */
    public boolean sampleLight(LevelZeroCellContext context,
                               LevelZeroSurfaceBiome surfaceBiome,
                               boolean largeRoom) {
        return sampleLight(context, surfaceBiome, largeRoom, LevelZeroSectorRoomKind.NONE);
    }

    /**
     * Echantillonne la presence d'un neon a partir du biome, du statut
     * de grande piece et du type de salle legacy.
     *
     * @param context contexte canonique de cellule
     * @param surfaceBiome biome cosmetique de surface
     * @param largeRoom {@code true} si la cellule appartient a une grande piece
     * @param roomKind type de salle legacy
     * @return {@code true} si la cellule porte un neon
     */
    public boolean sampleLight(LevelZeroCellContext context,
                               LevelZeroSurfaceBiome surfaceBiome,
                               boolean largeRoom,
                               LevelZeroSectorRoomKind roomKind) {
        return lightStage.sample(context, surfaceBiome, largeRoom, roomKind);
    }

    /**
     * Echantillonne la presence d'un neon avec arbitrage complet de proximite
     * entre voisins a partir de la vraie walkability regionale.
     *
     * @param context contexte canonique de cellule
     * @param surfaceBiome biome cosmetique de surface
     * @param largeRoom {@code true} si la cellule suit la logique de grande piece
     * @param roomKind type de salle legacy
     * @param regionWalkability carte regionale de walkability
     * @return {@code true} si la cellule porte effectivement un neon
     */
    public boolean sampleLight(LevelZeroCellContext context,
                               LevelZeroSurfaceBiome surfaceBiome,
                               boolean largeRoom,
                               LevelZeroSectorRoomKind roomKind,
                               LevelZeroRegionWalkability regionWalkability) {
        if (!lightStage.sampleCandidate(context, surfaceBiome, largeRoom, roomKind)) {
            return false;
        }
        return !hasWinningLightNeighbor(context, regionWalkability);
    }

    /**
     * Echantillonne le biome cosmetique de surface historique.
     *
     * @param context contexte canonique de cellule
     * @return biome cosmetique de surface
     */
    public LevelZeroSurfaceBiome sampleSurfaceBiome(LevelZeroCellContext context) {
        return surfaceBiomeStage.sample(context);
    }

    /**
     * Echantillonne le marquage historique des grandes pieces.
     *
     * @param context contexte canonique de cellule
     * @return {@code true} si la cellule appartient a une grande piece
     */
    public boolean sampleLargeRoom(LevelZeroCellContext context) {
        return largeRoomStage.sample(context);
    }

    /**
     * Evalue toutes les etapes legacy d'une cellule deja connue comme
     * traversable ou non.
     *
     * @param context contexte canonique de cellule
     * @param regionWalkability carte canonique de traversabilite regionale
     * @return evaluation agregee de la cellule
     */
    public LevelZeroCellEvaluation evaluateCell(LevelZeroCellContext context,
                                                LevelZeroRegionWalkability regionWalkability) {
        // Cette methode est le coeur de la generation logique du Level 0 :
        // on part d'une cellule walkable ou non, puis on derive successivement
        // sa salle legacy, son biome cosmetique, sa topologie, sa
        // micro-geometrie, son motif 3x3 et enfin son eventuelle lumiere.
        boolean walkable = regionWalkability.sampleWalkableCell(context.cellX(), context.cellZ());
        LevelZeroSectorRoomKind roomKind = regionWalkability.sampleRoomKindCell(context.cellX(), context.cellZ());
        boolean largeRoom = sampleLargeRoom(context);
        LevelZeroSurfaceBiome surfaceBiome = sampleSurfaceBiome(context);
        LevelZeroCellTopology topology = topologyStage.sample(context, regionWalkability, largeRoom);
        int connectionMask = walkable && !largeRoom
                ? topologyStage.sampleConnectionMask(context, regionWalkability)
                : com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellConnections.none();
        int geometryMask = geometryStage.sample(context, topology);
        boolean largeRoomLighting = topology == LevelZeroCellTopology.ROOM_LARGE;
        return new LevelZeroCellEvaluation(
                context,
                walkable,
                topology,
                connectionMask,
                geometryMask,
                microPatternStage.sample(context, topology, connectionMask, geometryMask),
                surfaceBiome,
                roomKind,
                largeRoom,
                walkable && sampleLight(context, surfaceBiome, largeRoomLighting, roomKind, regionWalkability));
    }

    private boolean hasWinningLightNeighbor(LevelZeroCellContext context,
                                            LevelZeroRegionWalkability regionWalkability) {
        // La proximite lumineuse s'arbitre au niveau de la pipeline complete,
        // pas seulement dans le LightStage, afin de comparer de vraies cellules
        // voisines avec leur biome, leur topologie ROOM_LARGE ou non et leur
        // roomKind reel.
        return neighborWins(context, regionWalkability, 1, 0)
                || neighborWins(context, regionWalkability, -1, 0)
                || neighborWins(context, regionWalkability, 0, 1)
                || neighborWins(context, regionWalkability, 0, -1)
                || neighborWins(context, regionWalkability, 1, 1)
                || neighborWins(context, regionWalkability, 1, -1)
                || neighborWins(context, regionWalkability, -1, 1)
                || neighborWins(context, regionWalkability, -1, -1);
    }

    private boolean neighborWins(LevelZeroCellContext context,
                                 LevelZeroRegionWalkability regionWalkability,
                                 int offsetX,
                                 int offsetZ) {
        LevelZeroCellContext neighbor = new LevelZeroCellContext(
                context.cellX() + offsetX,
                context.cellZ() + offsetZ,
                context.layoutSeed(),
                context.layerIndex());
        if (neighbor.cellX() < regionWalkability.minCellX()
                || neighbor.cellX() > regionWalkability.maxCellX()
                || neighbor.cellZ() < regionWalkability.minCellZ()
                || neighbor.cellZ() > regionWalkability.maxCellZ()) {
            return false;
        }
        if (!regionWalkability.sampleWalkableCell(neighbor.cellX(), neighbor.cellZ())) {
            return false;
        }

        boolean neighborLargeRoom = sampleLargeRoom(neighbor);
        LevelZeroCellTopology neighborTopology = topologyStage.sample(neighbor, regionWalkability, neighborLargeRoom);
        boolean neighborLargeRoomLighting = neighborTopology == LevelZeroCellTopology.ROOM_LARGE;
        LevelZeroSurfaceBiome neighborBiome = sampleSurfaceBiome(neighbor);
        LevelZeroSectorRoomKind neighborRoomKind =
                regionWalkability.sampleRoomKindCell(neighbor.cellX(), neighbor.cellZ());
        if (!lightStage.sampleCandidate(neighbor, neighborBiome, neighborLargeRoomLighting, neighborRoomKind)) {
            return false;
        }

        long selfScore = lightStage.lightScore(context);
        long neighborScore = lightStage.lightScore(neighbor);
        if (neighborScore != selfScore) {
            return neighborScore > selfScore;
        }
        if (neighbor.cellX() != context.cellX()) {
            return neighbor.cellX() > context.cellX();
        }
        return neighbor.cellZ() > context.cellZ();
    }

    /**
     * Derive la cle de cache historique d'un secteur.
     *
     * @param sectorX coordonnee secteur X
     * @param sectorZ coordonnee secteur Z
     * @param layoutSeed seed de layout
     * @return cle de cache deterministe
     */
    public long sampleSectorCacheKey(int sectorX, int sectorZ, long layoutSeed) {
        return sectorCacheKeyStage.sample(sectorX, sectorZ, layoutSeed);
    }
}
