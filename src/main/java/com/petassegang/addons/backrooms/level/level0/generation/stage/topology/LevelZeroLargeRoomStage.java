package com.petassegang.addons.backrooms.level.level0.generation.stage.topology;

import com.petassegang.addons.backrooms.level.level0.generation.noise.StageRandom;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroCellContext;
import com.petassegang.addons.backrooms.level.level0.generation.stage.LevelZeroCellStage;

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
        if (!isCandidate(context)) {
            return false;
        }
        long selfScore = roomScore(context);
        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                if (offsetX == 0 && offsetZ == 0) {
                    continue;
                }
                LevelZeroCellContext neighbor = new LevelZeroCellContext(
                        context.cellX() + offsetX,
                        context.cellZ() + offsetZ,
                        context.layoutSeed(),
                        context.layerIndex());
                if (isCandidate(neighbor) && neighborWins(neighbor, context, selfScore)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isCandidate(LevelZeroCellContext context) {
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

    private long roomScore(LevelZeroCellContext context) {
        return StageRandom.mixLegacy(
                context.layoutSeed(),
                StageRandom.Stage.LARGE_ROOMS,
                context.cellX() * 17 + context.layerIndex(),
                context.cellZ() * 31 - context.layerIndex());
    }

    private boolean neighborWins(LevelZeroCellContext neighbor, LevelZeroCellContext context, long selfScore) {
        long neighborScore = roomScore(neighbor);
        if (neighborScore != selfScore) {
            return neighborScore > selfScore;
        }
        if (neighbor.cellX() != context.cellX()) {
            return neighbor.cellX() > context.cellX();
        }
        return neighbor.cellZ() > context.cellZ();
    }
}
