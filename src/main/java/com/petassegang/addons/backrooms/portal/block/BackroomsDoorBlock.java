package com.petassegang.addons.backrooms.portal.block;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import com.petassegang.addons.init.ModBlockEntities;

/**
 * Porte des Backrooms : une vraie porte de deux blocs, en chene noir, qui s'ouvre et se ferme comme les autres.
 *
 * <p>La moitie basse porte la block entity qui gere les portails : le portail d'entree remplit l'encadrement des que la
 * porte connait sa destination, et la porte fermee le cache et le ferme sans le retirer, pour que l'autre cote reste
 * synchronise et que l'ouverture soit immediate. On entre par le cote d'ou la porte a ete posee, l'oppose de son
 * orientation. Sans la feature portail, c'est une porte ordinaire.
 */
public final class BackroomsDoorBlock extends DoorBlock implements BlockEntityProvider {

    /** Codec du bloc, exige par {@link DoorBlock}. */
    public static final MapCodec<BackroomsDoorBlock> CODEC = createCodec(BackroomsDoorBlock::new);

    /** Porte du jeu de blocs du chene noir (sons, ouverture a la main) ; resistance et rendu viennent des reglages. */
    public BackroomsDoorBlock(Settings settings) {
        super(BlockSetType.DARK_OAK, settings);
    }

    @Override
    public MapCodec<? extends DoorBlock> getCodec() {
        return CODEC;
    }

    /** Cote par lequel on entre dans le portail : celui d'ou le joueur a pose la porte, oppose a son orientation. */
    public static Direction entrySide(BlockState state) {
        return state.get(FACING).getOpposite();
    }

    /** Seule la moitie basse porte la block entity : une porte, un portail. */
    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER ? new BackroomsDoorBlockEntity(pos, state) : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (state.get(HALF) != DoubleBlockHalf.LOWER || type != ModBlockEntities.BACKROOMS_DOOR) {
            return null;
        }
        // Le ticker est pose des deux cotes : hors serveur, ou portails desactives, serverTick ne fait rien.
        return (BlockEntityTicker<T>) (BlockEntityTicker<BackroomsDoorBlockEntity>) (tickWorld, pos, blockState, door) -> door.serverTick();
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof BackroomsDoorBlockEntity door) {
            door.discardPortals();
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
