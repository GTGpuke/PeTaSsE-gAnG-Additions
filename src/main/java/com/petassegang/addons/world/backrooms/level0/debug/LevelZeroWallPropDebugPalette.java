package com.petassegang.addons.world.backrooms.level0.debug;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroLayoutSampler;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroWallFixture;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroWallPropProfile;

/**
 * Faux assets debug pour les petits details muraux.
 */
public final class LevelZeroWallPropDebugPalette {

    private LevelZeroWallPropDebugPalette() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Bloc debug de plinthe.
     *
     * @param profile profil mural complet
     * @return bloc debug de plinthe
     */
    public static BlockState baseboard(LevelZeroWallPropProfile profile) {
        return switch (profile.baseboardVariant().shape()) {
            case SINGLE -> Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            case END -> Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.getDefaultState();
            case STRAIGHT -> Blocks.POLISHED_BLACKSTONE_BRICK_SLAB.getDefaultState();
            case CORNER -> Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
            case TEE -> Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            case CROSS -> Blocks.POLISHED_BLACKSTONE.getDefaultState();
        };
    }

    /**
     * Bloc debug d'interrupteur.
     *
     * @return bloc debug d'interrupteur
     */
    public static BlockState switchFixture(int faceMask) {
        return switch (faceMask) {
            case LevelZeroLayoutSampler.NORTH_MASK -> Blocks.LIME_CONCRETE.getDefaultState();
            case LevelZeroLayoutSampler.EAST_MASK -> Blocks.GREEN_CONCRETE.getDefaultState();
            case LevelZeroLayoutSampler.SOUTH_MASK -> Blocks.EMERALD_BLOCK.getDefaultState();
            case LevelZeroLayoutSampler.WEST_MASK -> Blocks.GREEN_WOOL.getDefaultState();
            default -> Blocks.LIME_CONCRETE.getDefaultState();
        };
    }

    /**
     * Bloc debug de prise electrique.
     *
     * @return bloc debug de prise
     */
    public static BlockState outletFixture(int faceMask) {
        return switch (faceMask) {
            case LevelZeroLayoutSampler.NORTH_MASK -> Blocks.REDSTONE_BLOCK.getDefaultState();
            case LevelZeroLayoutSampler.EAST_MASK -> Blocks.RED_CONCRETE.getDefaultState();
            case LevelZeroLayoutSampler.SOUTH_MASK -> Blocks.NETHER_WART_BLOCK.getDefaultState();
            case LevelZeroLayoutSampler.WEST_MASK -> Blocks.RED_WOOL.getDefaultState();
            default -> Blocks.REDSTONE_BLOCK.getDefaultState();
        };
    }

    /**
     * Bloc debug correspondant a la fixture donnee.
     *
     * @param fixture fixture murale
     * @return bloc debug ou {@code null}
     */
    public static BlockState fixture(LevelZeroWallFixture fixture, int faceMask) {
        return switch (fixture) {
            case SWITCH -> switchFixture(faceMask);
            case OUTLET -> outletFixture(faceMask);
            default -> null;
        };
    }
}
