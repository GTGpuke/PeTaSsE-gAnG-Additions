package com.petassegang.addons.block.backrooms;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import com.petassegang.addons.world.backrooms.BackroomsConstants;
import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;

/**
 * Bloc de papier peint adaptatif du Level 0.
 *
 * <p>Le masque de faces (4 bits, valeurs 0-15) est stocke directement dans le
 * block state via {@link #FACE_MASK}. Aucune block entity n'est necessaire :
 * le BakedModel lit la property depuis le state, ce qui supprime l'overhead
 * NBT, reseau et GC associe aux anciennes block entities.
 */
public final class LevelZeroWallpaperBlock extends Block {

    public static final int NORTH_MASK = 1;
    public static final int SOUTH_MASK = 1 << 1;
    public static final int WEST_MASK  = 1 << 2;
    public static final int EAST_MASK  = 1 << 3;
    public static final int FULL_MASK  = NORTH_MASK | SOUTH_MASK | WEST_MASK | EAST_MASK;

    /** Masque de faces (0-15) encode dans le block state — pas de block entity. */
    public static final IntProperty FACE_MASK = IntProperty.of("face_mask", 0, 15);

    private static final int MAX_SURFACE_PROBE_DISTANCE = BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 4;
    private static final int FALLBACK_BIOME_SAMPLE_DISTANCE = BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 2;

    public LevelZeroWallpaperBlock(AbstractBlock.Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACE_MASK, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACE_MASK);
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        // Evite la recursivite : setBlockState() ci-dessous re-declenche onBlockAdded
        // avec oldState.getBlock() == LevelZeroWallpaperBlock, ce qui sort immediatement.
        if (world.isClient() || state.isOf(oldState.getBlock())) {
            return;
        }
        int newMask = sampleFaceMask(world, pos);
        if (state.get(FACE_MASK) != newMask) {
            world.setBlockState(pos, state.with(FACE_MASK, newMask), Block.NOTIFY_LISTENERS);
        }
    }

    /**
     * Calcule le masque de faces d'un mur a partir du monde.
     * Methode commune (utilisable cote serveur et client).
     *
     * @param level acces bloc du niveau
     * @param pos   position du mur
     * @return masque des faces alternatives (0-15)
     */
    public static int sampleFaceMask(BlockView level, BlockPos pos) {
        int mask = 0;
        if (isAlternate(level, pos, Direction.NORTH)) mask |= NORTH_MASK;
        if (isAlternate(level, pos, Direction.SOUTH)) mask |= SOUTH_MASK;
        if (isAlternate(level, pos, Direction.WEST))  mask |= WEST_MASK;
        if (isAlternate(level, pos, Direction.EAST))  mask |= EAST_MASK;
        return mask;
    }

    private static boolean isAlternate(BlockView level, BlockPos pos, Direction direction) {
        return findVisibleSurfaceBiome(level, pos, direction) == LevelZeroSurfaceBiome.RED;
    }

    private static LevelZeroSurfaceBiome findVisibleSurfaceBiome(BlockView level,
                                                                   BlockPos origin,
                                                                   Direction direction) {
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        for (int dist = 1; dist <= MAX_SURFACE_PROBE_DISTANCE; dist++) {
            int sx = origin.getX() + direction.getOffsetX() * dist;
            int sz = origin.getZ() + direction.getOffsetZ() * dist;
            mutablePos.set(sx, BackroomsConstants.LEVEL_ZERO_AIR_MIN_Y, sz);
            if (!level.getBlockState(mutablePos).isAir()) {
                continue;
            }
            return LevelZeroSurfaceBiome.sampleAtWorld(sx, sz);
        }
        int fallbackX = origin.getX() + direction.getOffsetX() * FALLBACK_BIOME_SAMPLE_DISTANCE;
        int fallbackZ = origin.getZ() + direction.getOffsetZ() * FALLBACK_BIOME_SAMPLE_DISTANCE;
        return LevelZeroSurfaceBiome.sampleAtWorld(fallbackX, fallbackZ);
    }
}
