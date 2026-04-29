package com.petassegang.addons.backrooms.level.level0.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;

/**
 * Bloc de plinthe adaptative du Level 0.
 *
 * <p>La plinthe ne porte qu'un masque de faces exposees et ne depend pas d'une
 * block entity. Le rendu client choisit ensuite quelles bandes basses
 * afficher selon ce masque.
 */
public final class LevelZeroBaseboardBlock extends Block {

    /** Masque des faces porte par la plinthe (0-15). */
    public static final IntProperty FACE_MASK = IntProperty.of("face_mask", 0, 15);

    public LevelZeroBaseboardBlock(AbstractBlock.Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACE_MASK, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACE_MASK);
    }
}
