package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import com.petassegang.addons.block.LevelZeroFluorescentLightBlock;
import com.petassegang.addons.block.NeonColor;
import com.petassegang.addons.init.ModBlocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie les proprietes exposees par le neon fluorescent.
 */
@DisplayName("Etat du neon fluorescent du Level 0")
class LevelZeroFluorescentLightBlockStateTest {

    @Test
    @DisplayName("Le neon expose bien les proprietes attendues")
    void testDefaultStateExposesRequestedProperties() {
        BlockState state = ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT.get().defaultBlockState();

        assertEquals(Direction.DOWN, state.getValue(LevelZeroFluorescentLightBlock.FACING),
                "Le neon doit pointer vers le bas par defaut.");
        assertTrue(state.getValue(LevelZeroFluorescentLightBlock.LIT),
                "Le neon doit etre allume par defaut.");
        assertFalse(state.getValue(LevelZeroFluorescentLightBlock.BROKEN),
                "Le neon ne doit pas etre casse par defaut.");
        assertEquals(NeonColor.WARM_YELLOW, state.getValue(LevelZeroFluorescentLightBlock.NEON_COLOR),
                "La couleur par defaut du neon doit rester jaune chaud.");
    }
}
