package com.petassegang.addons.world.backrooms.level0.stage;

import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellState;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTag;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;

/**
 * Resultat agrege de la pipeline legacy pour une cellule logique.
 */
public record LevelZeroCellEvaluation(
        LevelZeroCellContext context,
        boolean walkable,
        LevelZeroCellTopology topology,
        int geometryMask,
        int microPattern,
        LevelZeroSurfaceBiome surfaceBiome,
        boolean largeRoom,
        boolean lighted) {

    /**
     * Retourne le tag semantique minimal de la cellule evaluee.
     *
     * @return tag semantique derive
     */
    public LevelZeroCellTag cellTag() {
        if (topology == LevelZeroCellTopology.WALL) {
            return LevelZeroCellTag.WALL;
        }
        return topology == LevelZeroCellTopology.ROOM_LARGE ? LevelZeroCellTag.ROOM_LARGE : LevelZeroCellTag.CORRIDOR;
    }

    /**
     * Retourne l'etat semantique minimal de la cellule evaluee.
     *
     * @return etat semantique derive
     */
    public LevelZeroCellState cellState() {
        return new LevelZeroCellState(
                cellTag(),
                topology,
                geometryMask,
                microPattern,
                1,
                1,
                surfaceBiome,
                largeRoom,
                lighted);
    }
}
