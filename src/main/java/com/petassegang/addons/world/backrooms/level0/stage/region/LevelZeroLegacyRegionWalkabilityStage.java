package com.petassegang.addons.world.backrooms.level0.stage.region;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroRegionWalkability;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroWalkabilitySampler;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroRegionContext;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroRegionStage;

/**
 * Etape regionale qui materialise la verite brute de walkability d'une fenetre
 * de cellules.
 *
 * <p>Avant toute topologie, biome ou lumiere, la pipeline commence ici par une
 * simple carte regionale : quelles cellules sont ouvertes, et a quel type de
 * salle historique elles appartiennent.
 */
public final class LevelZeroLegacyRegionWalkabilityStage implements LevelZeroRegionStage<LevelZeroRegionWalkability> {

    private final LevelZeroWalkabilitySampler walkabilitySampler;

    /**
     * Construit l'etape regionale de traversabilite legacy.
     *
     * @param walkabilitySampler source de traversabilite des cellules
     */
    public LevelZeroLegacyRegionWalkabilityStage(LevelZeroWalkabilitySampler walkabilitySampler) {
        this.walkabilitySampler = walkabilitySampler;
    }

    @Override
    public LevelZeroRegionWalkability sample(LevelZeroRegionContext context) {
        int minCellX = context.window().minCellX();
        int minCellZ = context.window().minCellZ();
        int maxCellX = context.window().maxCellX();
        int maxCellZ = context.window().maxCellZ();
        int width = context.window().width();
        int height = context.window().height();
        boolean[] walkable = new boolean[width * height];
        com.petassegang.addons.world.backrooms.level0.layout.sector.LevelZeroSectorRoomKind[] roomKinds =
                new com.petassegang.addons.world.backrooms.level0.layout.sector.LevelZeroSectorRoomKind[width * height];

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                int index = (cellZ - minCellZ) * width + (cellX - minCellX);
                walkable[index] = walkabilitySampler.sampleWalkableCell(cellX, cellZ);
                roomKinds[index] = walkabilitySampler.sampleRoomKindCell(cellX, cellZ);
            }
        }

        return new LevelZeroRegionWalkability(minCellX, minCellZ, maxCellX, maxCellZ, walkable, roomKinds);
    }
}
