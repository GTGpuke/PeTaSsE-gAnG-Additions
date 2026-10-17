package com.petassegang.addons.backrooms.level0.generation.write;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import com.petassegang.addons.backrooms.level0.block.LevelZeroWallpaperBlock;

/**
 * Blocs vanilla voyants utilises par les toggles de debug de la generation.
 */
public final class LevelZeroDebugPalette {

    private LevelZeroDebugPalette() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Sol d'une cellule portant une anomalie geometrique. */
    public static BlockState gapFloor() {
        return Blocks.WHITE_CONCRETE.getDefaultState();
    }

    /** Bloc ferme par la micro-geometrie dans une cellule ouverte. */
    public static BlockState gapWall() {
        return Blocks.GRAY_CONCRETE.getDefaultState();
    }

    /**
     * Bloc de debug d'un element mural, colore selon la face qu'il occupe.
     *
     * @param fixture element mural
     * @param faceMask face de l'element
     * @return bloc de debug
     * @throws IllegalArgumentException sans element ({@code NONE}) : rien a poser
     */
    public static BlockState fixture(LevelZeroWallProps.Fixture fixture, int faceMask) {
        return switch (fixture) {
            case SWITCH -> switch (faceMask) {
                case LevelZeroWallpaperBlock.EAST_MASK -> Blocks.GREEN_CONCRETE.getDefaultState();
                case LevelZeroWallpaperBlock.SOUTH_MASK -> Blocks.EMERALD_BLOCK.getDefaultState();
                case LevelZeroWallpaperBlock.WEST_MASK -> Blocks.GREEN_WOOL.getDefaultState();
                default -> Blocks.LIME_CONCRETE.getDefaultState();
            };
            case OUTLET -> switch (faceMask) {
                case LevelZeroWallpaperBlock.EAST_MASK -> Blocks.RED_CONCRETE.getDefaultState();
                case LevelZeroWallpaperBlock.SOUTH_MASK -> Blocks.NETHER_WART_BLOCK.getDefaultState();
                case LevelZeroWallpaperBlock.WEST_MASK -> Blocks.RED_WOOL.getDefaultState();
                default -> Blocks.REDSTONE_BLOCK.getDefaultState();
            };
            case NONE -> throw new IllegalArgumentException("Element mural absent : aucun bloc de debug a poser.");
        };
    }
}
