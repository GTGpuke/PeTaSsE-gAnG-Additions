package com.gtgpuke.gtgsbackrooms.backrooms.portal.block;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
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
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.init.ModBlockEntities;

/**
 * Bloc portail : il pose dans son bloc un portail plein cadre, le plan au milieu du bloc, qui monte et s'elargit selon les
 * reglages de l'item qui l'a pose. A deux faces (par defaut), le portail se voit et se traverse des deux cotes ; a une
 * face, de dos il n'y a rien.
 *
 * <p>Le bloc lui-meme ne se voit pas et ne bloque rien : seul le portail compte. Il ne se vise qu'accroupi, pour le casser ;
 * debout, le regard passe a travers lui et vise ce qu'on voit par le portail. Sa face avant regarde le joueur qui l'a pose.
 */
public final class BackroomsPortalBlock extends BlockWithEntity {

    /** Codec du bloc, exige par {@link BlockWithEntity}. */
    public static final MapCodec<BackroomsPortalBlock> CODEC = createCodec(BackroomsPortalBlock::new);
    /** Orientation de la face avant du portail, vers le joueur qui a pose le bloc. */
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    /** Plan d'un portail face au nord ou au sud, epaissi d'un huitieme de bloc pour etre visable. */
    private static final VoxelShape Z_PLANE = Block.createCuboidShape(0.0, 0.0, 7.0, 16.0, 16.0, 9.0);
    /** Plan d'un portail face a l'est ou a l'ouest. */
    private static final VoxelShape X_PLANE = Block.createCuboidShape(7.0, 0.0, 0.0, 9.0, 16.0, 16.0);

    /** Bloc portail aux reglages donnes (son, solidite). */
    public BackroomsPortalBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Face avant vers le joueur ; refuse la pose si le cadre aux reglages de l'item traverserait un bloc. */
    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        Direction front = context.getHorizontalPlayerFacing().getOpposite();
        PortalSettings settings = PortalSettings.of(context.getStack());
        if (!PortalArea.isFree(context.getWorld(), context.getBlockPos(), PortalFacing.fromDirection(front), settings)) {
            return null;
        }
        return getDefaultState().with(FACING, front);
    }

    /**
     * Visable seulement par qui s'accroupit (un joueur, le temps de le casser) ; sinon le regard passe a travers et vise ce
     * qu'on voit par le portail.
     */
    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (!context.isDescending()) {
            return VoxelShapes.empty();
        }
        return state.get(FACING).getAxis() == Direction.Axis.Z ? Z_PLANE : X_PLANE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BackroomsPortalBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        // Le ticker est pose des deux cotes : hors serveur, serverTick ne fait rien (le client recoit les portails comme entities).
        return validateTicker(type, ModBlockEntities.BACKROOMS_PORTAL, (tickWorld, pos, blockState, portal) -> portal.serverTick());
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof BackroomsPortalBlockEntity portal) {
            portal.discardPortals();
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
