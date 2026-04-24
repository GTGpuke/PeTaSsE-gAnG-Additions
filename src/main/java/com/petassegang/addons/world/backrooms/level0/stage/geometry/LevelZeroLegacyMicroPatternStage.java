package com.petassegang.addons.world.backrooms.level0.stage.geometry;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellMicroPattern;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;

/**
 * Etape legacy de projection bloc-par-bloc des anomalies geometriques.
 *
 * <p>La projection est vide tant qu'aucune variante de noise n'a ete
 * reconstruite. Elle conserve seulement le contrat entre le geometry stage et
 * le layout final : un mur reste ferme, tout le reste reste ouvert.
 */
public final class LevelZeroLegacyMicroPatternStage {

    /**
     * Projette le masque geometrique sur un motif local 3x3.
     *
     * @param context contexte canonique de cellule
     * @param topology topologie semantique fine
     * @param connectionMask masque des connexions cardinales
     * @param geometryMask masque de features geometriques
     * @return motif 3x3 bloc-par-bloc
     */
    public int sample(LevelZeroCellContext context, LevelZeroCellTopology topology, int connectionMask, int geometryMask) {
        return topology == LevelZeroCellTopology.WALL
                ? LevelZeroCellMicroPattern.FULL_CLOSED
                : LevelZeroCellMicroPattern.FULL_OPEN;
    }
}
