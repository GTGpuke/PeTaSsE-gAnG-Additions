package com.gtgpuke.gtgsbackrooms.core.portal.block;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
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

import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.core.registry.ModBlockEntities;

/**
 * Bloc portail : il pose dans son bloc un portail plein cadre, le plan au milieu du bloc, qui monte et s'elargit selon les
 * reglages de la baguette qui l'a pose. A deux faces (par defaut), le portail se voit et se traverse des deux cotes ; a une
 * face, de dos il n'y a rien.
 *
 * <p>Le bloc lui-meme ne se voit pas et ne bloque rien : seul le portail compte. Il ne se vise qu'accroupi, pour le casser
 * (il ne rend rien) ou en copier les reglages ; debout, le regard passe a travers lui et vise ce qu'on voit par le portail.
 * Sa face avant regarde le joueur qui l'a pose.
 */
public final class BackroomsPortalBlock extends BlockWithEntity {

    /** Codec du bloc, exige par {@link BlockWithEntity}. */
    public static final MapCodec<BackroomsPortalBlock> CODEC = createCodec(BackroomsPortalBlock::new);
    /** Orientation de la face avant du portail : vers le joueur qui a pose le bloc, ou vers le haut (cadre au sol) ou le bas (au plafond). */
    public static final DirectionProperty FACING = Properties.FACING;
    /** Regard plus incline que cela vers un sol ou un plafond sur lequel on pose le bloc : le cadre se couche dessus. */
    static final float LYING_PLACEMENT_PITCH = 60.0f;

    /** Plan d'un portail face au nord ou au sud, epaissi d'un huitieme de bloc pour etre visable. */
    private static final VoxelShape Z_PLANE = Block.createCuboidShape(0.0, 0.0, 7.0, 16.0, 16.0, 9.0);
    /** Plan d'un portail face a l'est ou a l'ouest. */
    private static final VoxelShape X_PLANE = Block.createCuboidShape(7.0, 0.0, 0.0, 9.0, 16.0, 16.0);
    /** Plan d'un portail couche, au sol ou au plafond. */
    private static final VoxelShape Y_PLANE = Block.createCuboidShape(0.0, 7.0, 0.0, 16.0, 9.0, 16.0);
    /** Plan d'un portail au bord du bloc, sur sa face arriere, par direction de la face avant ({@link Direction#getId()}). */
    private static final VoxelShape[] FLUSH_PLANES = flushPlanes();

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

    /**
     * Face avant vers le joueur ; pose en regardant franchement vers le bas ou vers le haut, le cadre se couche, face au
     * haut (au sol : on y tombe) ou au bas (au plafond : on y monte). Refuse la pose si le cadre aux reglages de l'item
     * traverserait un bloc, ou si un cadre couche n'a pas deux blocs libres derriere lui.
     */
    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        Direction front = lyingFacing(context);
        if (front == null) {
            front = context.getHorizontalPlayerFacing().getOpposite();
        }
        PortalSettings settings = PortalSettings.of(context.getStack());
        if (!PortalArea.isFree(context.getWorld(), context.getBlockPos(), PortalFacing.fromDirection(front), settings)) {
            return null;
        }
        return getDefaultState().with(FACING, front);
    }

    /**
     * Orientation d'un cadre couche : le joueur pose le bloc en regardant vers le bas a plus de {@value #LYING_PLACEMENT_PITCH}
     * degres (cadre au sol, face au haut : on y tombe) ou vers le haut (cadre au plafond, face au bas : on y monte), quel
     * que soit le bloc clique ; {@code null} pour un cadre debout. Le bloc se pose la ou l'on vise, dans le vide qui le
     * recevra : un cadre au sol au-dessus d'un puits, un cadre au plafond sous une cheminee.
     */
    static @Nullable Direction lyingFacing(ItemPlacementContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null) {
            return null;
        }
        if (player.getPitch() > LYING_PLACEMENT_PITCH) {
            return Direction.UP;
        }
        return player.getPitch() < -LYING_PLACEMENT_PITCH ? Direction.DOWN : null;
    }

    /**
     * Visable seulement par qui s'accroupit (un joueur, le temps de le casser) ; sinon le regard passe a travers et vise ce
     * qu'on voit par le portail. Le plan visable suit celui du portail : au milieu du bloc, ou sur sa face arriere.
     */
    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (!context.isDescending()) {
            return VoxelShapes.empty();
        }
        if (world.getBlockEntity(pos) instanceof BackroomsPortalBlockEntity portal && portal.settings().flush()) {
            return FLUSH_PLANES[state.get(FACING).getId()];
        }
        Direction.Axis axis = state.get(FACING).getAxis();
        if (axis == Direction.Axis.Y) {
            return Y_PLANE;
        }
        return axis == Direction.Axis.Z ? Z_PLANE : X_PLANE;
    }

    /** Plaque d'un huitieme de bloc contre la face arriere, pour chaque direction de la face avant. */
    private static VoxelShape[] flushPlanes() {
        VoxelShape[] planes = new VoxelShape[Direction.values().length];
        for (Direction front : Direction.values()) {
            Direction back = front.getOpposite();
            double[] min = {0.0, 0.0, 0.0};
            double[] max = {16.0, 16.0, 16.0};
            int axis = back.getAxis().ordinal();
            min[axis] = back.getDirection() == Direction.AxisDirection.POSITIVE ? 14.0 : 0.0;
            max[axis] = min[axis] + 2.0;
            planes[front.getId()] = Block.createCuboidShape(min[0], min[1], min[2], max[0], max[1], max[2]);
        }
        return planes;
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
