package com.petassegang.addons.world.backrooms.level0.stage;

import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionWalkability;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;

/**
 * Pipeline explicite de la version legacy du layout Level 0.
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
        return lightStage.sample(context);
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
        boolean walkable = regionWalkability.sampleWalkableCell(context.cellX(), context.cellZ());
        boolean largeRoom = sampleLargeRoom(context);
        LevelZeroCellTopology topology = topologyStage.sample(context, regionWalkability, largeRoom);
        int geometryMask = geometryStage.sample(context, topology);
        return new LevelZeroCellEvaluation(
                context,
                walkable,
                topology,
                geometryMask,
                microPatternStage.sample(context, topology, geometryMask),
                sampleSurfaceBiome(context),
                largeRoom,
                walkable && sampleLight(context));
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
