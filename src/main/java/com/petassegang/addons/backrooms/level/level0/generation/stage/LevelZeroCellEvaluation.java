package com.petassegang.addons.backrooms.level.level0.generation.stage;

import com.petassegang.addons.backrooms.level.level0.biome.LevelZeroSurfaceBiome;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellState;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellTag;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellTopology;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorRoomKind;

/**
 * Resultat agrege de la pipeline legacy pour une cellule logique.
 *
 * <p>Cette structure marque la frontiere entre l'evaluation semantique et les
 * couches ulterieures du pipeline. Elle rassemble, pour une meme cellule, les
 * decisions de topologie, biome, micro-geometrie et lumiere avant leur
 * projection dans le layout extrait puis dans le writer.</p>
 */
public record LevelZeroCellEvaluation(
        LevelZeroCellContext context,
        boolean walkable,
        LevelZeroCellTopology topology,
        int connectionMask,
        int geometryMask,
        int microPattern,
        LevelZeroSurfaceBiome surfaceBiome,
        LevelZeroSectorRoomKind roomKind,
        boolean largeRoom,
        boolean lighted) {

    /**
     * Retourne le tag semantique minimal de la cellule evaluee.
     *
     * <p>Ce tag compact sert surtout aux couches qui n'ont pas besoin du detail
     * complet de la topologie, mais seulement de distinguer mur, couloir ou
     * grande piece.</p>
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
     * <p>Cet etat est volontairement compact : il transporte juste assez
     * d'information pour permettre au layout extrait, au rendu mural et au
     * writer de rester coherents sans recalculer toute la pipeline.</p>
     *
     * @return etat semantique derive
     */
    public LevelZeroCellState cellState() {
        return new LevelZeroCellState(
                cellTag(),
                topology,
                connectionMask,
                geometryMask,
                microPattern,
                1,
                1,
                surfaceBiome,
                roomKind,
                largeRoom,
                lighted);
    }
}
