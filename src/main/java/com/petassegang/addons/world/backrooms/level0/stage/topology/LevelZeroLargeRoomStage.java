package com.petassegang.addons.world.backrooms.level0.stage.topology;

import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellContext;
import com.petassegang.addons.world.backrooms.level0.stage.LevelZeroCellStage;

/**
 * Etape historique de marquage des grandes pieces.
 *
 * <p>Cette etape reste volontairement simple : elle marque des cellules
 * candidates a etre traitees comme grandes rooms, mais elle ne redefinit ni la
 * walkability brute du secteur, ni la topologie fine a elle seule.
 *
 * <p>Autrement dit, ce fichier ne "creuse" pas une salle. Il fournit un
 * marquage stable qui sera ensuite relu par la topologie, la lumiere et le
 * rendu.
 */
public final class LevelZeroLargeRoomStage implements LevelZeroCellStage<Boolean> {

    @Override
    public Boolean sample(LevelZeroCellContext context) {
        long hash = StageRandom.mixLegacy(
                context.layoutSeed(),
                StageRandom.Stage.LARGE_ROOMS,
                context.cellX(),
                context.cellZ());
        // Un peu plus frequent que l'ancienne proba 1/4, sans basculer vers
        // un niveau trop ouvert : ~31.25 % des cellules candidates.
        // Le but ici est seulement de fournir un marquage stable qui sera
        // ensuite reinterprete par la topologie, la lumiere et le rendu.
        return Math.floorMod(hash, 16) < 5;
    }
}
