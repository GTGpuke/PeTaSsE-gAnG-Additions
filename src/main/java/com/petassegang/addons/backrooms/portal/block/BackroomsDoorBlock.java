package com.petassegang.addons.backrooms.portal.block;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import com.petassegang.addons.init.ModBlockEntities;

/**
 * Porte des Backrooms : un bloc oriente face au joueur qui la pose, dont la block entity porte le portail.
 *
 * <p>La porte n'a pas de collision : le portail affleure sa face, et celui qui en ressort n'est pas pousse par le
 * bloc. Sans la feature portail, la porte reste un bloc decoratif inerte.
 */
public final class BackroomsDoorBlock extends BlockWithEntity {

    public static final MapCodec<BackroomsDoorBlock> CODEC = createCodec(BackroomsDoorBlock::new);
    /** Cote de la porte par lequel on entre : la normale du portail d'entree. */
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public BackroomsDoorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BackroomsDoorBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof BackroomsDoorBlockEntity door) {
            door.discardPortals();
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // Le ticker est pose des deux cotes : hors serveur, ou portails desactives, serverTick ne fait rien.
        return validateTicker(type, ModBlockEntities.BACKROOMS_DOOR, (tickWorld, pos, blockState, door) -> door.serverTick());
    }
}
