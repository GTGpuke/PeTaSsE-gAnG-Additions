package com.petassegang.addons.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Neon fluorescent minimal du Level 0.
 *
 * <p>Le bloc est toujours orienté vers le bas (plafond → sol). Pas de propriété
 * FACING : la direction est fixe et implicite.
 */
public final class LevelZeroFluorescentLightBlock extends Block {

    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");
    public static final EnumProperty<NeonColor> NEON_COLOR = EnumProperty.create("neon_color", NeonColor.class);

    /**
     * Construit le neon fluorescent du Level 0.
     *
     * @param properties proprietes du bloc
     */
    public LevelZeroFluorescentLightBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(LIT, true)
                .setValue(BROKEN, false)
                .setValue(NEON_COLOR, NeonColor.WARM_YELLOW));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && state.getValue(LIT) && !state.getValue(BROKEN)) {
            level.getLightEngine().checkBlock(pos);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        // Forcer le recalcul du faisceau quand un voisin change, pour éviter que
        // vanilla ne propage les blocs du faisceau sans notre logique custom.
        if (!level.isClientSide() && state.getValue(LIT) && !state.getValue(BROKEN)) {
            level.getLightEngine().checkBlock(pos);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, BROKEN, NEON_COLOR);
    }
}
