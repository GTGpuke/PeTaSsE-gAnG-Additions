package com.petassegang.addons.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import com.petassegang.addons.block.entity.LevelZeroWallpaperBlockEntity;

/**
 * Bloc de papier peint du Level 0.
 *
 * <p>Le rendu reel des faces est adapte cote client selon le biome de surface
 * adjacent, mais le bloc reste unique cote monde.
 */
public final class LevelZeroWallpaperBlock extends Block implements EntityBlock {

    /**
     * Construit le bloc de papier peint du Level 0.
     *
     * @param properties proprietes du bloc
     */
    public LevelZeroWallpaperBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LevelZeroWallpaperBlockEntity(pos, state);
    }
}
