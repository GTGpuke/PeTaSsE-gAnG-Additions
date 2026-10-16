package com.petassegang.addons.backrooms.level0.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import com.petassegang.addons.backrooms.level0.biome.LevelZeroSurfaceBiome;
import com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroCoords;
import com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;

/**
 * Mur de papier peint adaptatif du Level 0.
 *
 * <p>Chaque face horizontale affiche soit le papier peint jauni, soit la
 * variante blanche, selon le biome visible depuis cette face. Le choix est
 * encode dans la propriete {@link #FACE_MASK} (4 bits) : aucune block entity,
 * donc aucun cout NBT ou reseau. Le generateur calcule le masque a la pose ;
 * un bloc place a la main le recalcule depuis le monde.
 */
public final class LevelZeroWallpaperBlock extends Block {

    public static final int NORTH_MASK = 1;
    public static final int SOUTH_MASK = 1 << 1;
    public static final int WEST_MASK = 1 << 2;
    public static final int EAST_MASK = 1 << 3;
    public static final int FULL_MASK = NORTH_MASK | SOUTH_MASK | WEST_MASK | EAST_MASK;

    /** Masque des faces affichant la variante blanche. */
    public static final IntProperty FACE_MASK = IntProperty.of("face_mask", 0, FULL_MASK);

    /** Distance maximale, en blocs, pour trouver la premiere colonne d'air en face d'un mur. */
    public static final int MAX_SURFACE_PROBE_DISTANCE = LevelZeroCoords.CELL_SCALE * 4;

    /** Distance utilisee pour echantillonner le biome quand aucune colonne d'air n'est trouvee. */
    public static final int FALLBACK_BIOME_SAMPLE_DISTANCE = LevelZeroCoords.CELL_SCALE * 2;

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
        // Le setBlockState ci-dessous redeclenche onBlockAdded avec oldState == ce bloc : on sort.
        if (world.isClient() || state.isOf(oldState.getBlock())) {
            return;
        }
        int newMask = sampleFaceMask(world, pos);
        if (state.get(FACE_MASK) != newMask) {
            world.setBlockState(pos, state.with(FACE_MASK, newMask), Block.NOTIFY_LISTENERS);
        }
    }

    /**
     * Bit de masque associe a une direction horizontale.
     *
     * @param direction direction horizontale
     * @return bit du masque, 0 pour une direction verticale
     */
    public static int maskFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH_MASK;
            case SOUTH -> SOUTH_MASK;
            case WEST -> WEST_MASK;
            case EAST -> EAST_MASK;
            default -> 0;
        };
    }

    /**
     * Calcule le masque de faces d'un mur a partir des blocs du monde.
     *
     * @param level acces aux blocs
     * @param pos position du mur
     * @return masque des faces affichant la variante blanche
     */
    public static int sampleFaceMask(BlockView level, BlockPos pos) {
        int layerIndex = LevelZeroLayerStackLayout.isInsideStack(pos.getY())
                ? LevelZeroLayerStackLayout.sliceAtY(pos.getY()).layerIndex()
                : 0;
        int mask = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (findVisibleSurfaceBiome(level, pos, direction, layerIndex) == LevelZeroSurfaceBiome.RED) {
                mask |= maskFor(direction);
            }
        }
        return mask;
    }

    private static LevelZeroSurfaceBiome findVisibleSurfaceBiome(BlockView level, BlockPos origin,
                                                                 Direction direction, int layerIndex) {
        BlockPos.Mutable mutablePos = new BlockPos.Mutable();
        for (int distance = 1; distance <= MAX_SURFACE_PROBE_DISTANCE; distance++) {
            int sampleX = origin.getX() + direction.getOffsetX() * distance;
            int sampleZ = origin.getZ() + direction.getOffsetZ() * distance;
            if (level.getBlockState(mutablePos.set(sampleX, origin.getY(), sampleZ)).isAir()) {
                return LevelZeroSurfaceBiome.sampleAtWorld(sampleX, sampleZ, layerIndex);
            }
        }
        return LevelZeroSurfaceBiome.sampleAtWorld(
                origin.getX() + direction.getOffsetX() * FALLBACK_BIOME_SAMPLE_DISTANCE,
                origin.getZ() + direction.getOffsetZ() * FALLBACK_BIOME_SAMPLE_DISTANCE,
                layerIndex);
    }
}
