package com.petassegang.addons.backrooms.level.level0.generation.debug;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import com.petassegang.addons.backrooms.level.level0.generation.write.structure.LevelZeroStructureCellRole;
import com.petassegang.addons.backrooms.level.level0.generation.write.structure.LevelZeroStructureGameplayPointKind;

/**
 * Faux assets de debug pour lire rapidement les structures rares du Level 0.
 */
public final class LevelZeroStructureDebugPalette {

    private LevelZeroStructureDebugPalette() {
        throw new UnsupportedOperationException("Palette utilitaire.");
    }

    /**
     * Retourne un bloc de debug pour un point de gameplay potentiel.
     *
     * @param pointKind type de point de gameplay
     * @return bloc de debug visible
     */
    public static BlockState gameplayPoint(LevelZeroStructureGameplayPointKind pointKind) {
        return switch (pointKind) {
            case ENTRY -> Blocks.YELLOW_CONCRETE.getDefaultState();
            case FOCAL_POINT -> Blocks.CYAN_CONCRETE.getDefaultState();
            case LOOT_HINT -> Blocks.LIME_CONCRETE.getDefaultState();
            case UTILITY_HINT -> Blocks.RED_CONCRETE.getDefaultState();
            default -> Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
        };
    }

    /**
     * Retourne un bloc secondaire pour visualiser seulement le role local dans
     * le footprint.
     *
     * @param role role local de la cellule
     * @return bloc de debug
     */
    public static BlockState structureRole(LevelZeroStructureCellRole role) {
        return switch (role) {
            case ANCHOR -> Blocks.WHITE_CONCRETE.getDefaultState();
            case EDGE -> Blocks.GRAY_CONCRETE.getDefaultState();
            case CENTER -> Blocks.BLUE_CONCRETE.getDefaultState();
            case INTERIOR -> Blocks.LIGHT_BLUE_CONCRETE.getDefaultState();
            default -> Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
        };
    }
}
