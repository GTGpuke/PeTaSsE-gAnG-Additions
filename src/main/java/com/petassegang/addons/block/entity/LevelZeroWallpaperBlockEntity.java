package com.petassegang.addons.block.entity;

import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraftforge.client.model.data.ModelData;

import com.petassegang.addons.client.model.LevelZeroWallpaperBlockStateModel;
import com.petassegang.addons.init.ModBlockEntities;

/**
 * Block entity minimale du papier peint du Level 0.
 *
 * <p>Elle sert uniquement a fournir une ModelData dependante de la position,
 * afin que le rendu client puisse choisir une texture par face exposee.
 */
public final class LevelZeroWallpaperBlockEntity extends BlockEntity {

    private static final String FACE_MASK_TAG = "FaceMask";
    private static final int UNSET_FACE_MASK = -1;

    private int faceMask = UNSET_FACE_MASK;

    /**
     * Construit la block entity du papier peint du Level 0.
     *
     * @param pos position du bloc
     * @param state etat du bloc
     */
    public LevelZeroWallpaperBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEVEL_ZERO_WALLPAPER.get(), pos, state);
    }

    /**
     * Construit la block entity du papier peint du Level 0 avec un masque
     * de faces pre-calcule.
     *
     * @param pos position du bloc
     * @param state etat du bloc
     * @param faceMask masque des faces alternatives
     */
    public LevelZeroWallpaperBlockEntity(BlockPos pos, BlockState state, int faceMask) {
        this(pos, state);
        this.faceMask = faceMask;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null) {
            return;
        }
        if (!level.isClientSide() && faceMask == UNSET_FACE_MASK) {
            faceMask = LevelZeroWallpaperBlockStateModel.sampleFaceMask(level, worldPosition);
            setChanged();
        }
        refreshClientRender(false);
    }

    @Override
    public void handleUpdateTag(ValueInput tag, HolderLookup.Provider holders) {
        int previousFaceMask = faceMask;
        super.handleUpdateTag(tag, holders);
        if (previousFaceMask != faceMask) {
            refreshClientRender(true);
        }
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput data, HolderLookup.Provider lookup) {
        int previousFaceMask = faceMask;
        super.onDataPacket(connection, data, lookup);
        if (previousFaceMask != faceMask) {
            refreshClientRender(true);
        }
    }

    @Override
    public @NotNull ModelData getModelData() {
        return LevelZeroWallpaperBlockStateModel.createModelData(faceMask);
    }

    /**
     * Met a jour le masque de faces stocke puis marque la block entity comme
     * modifiee.
     *
     * @param newFaceMask nouveau masque de faces
     */
    public boolean setFaceMask(int newFaceMask) {
        if (faceMask == newFaceMask) {
            return false;
        }
        faceMask = newFaceMask;
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(FACE_MASK_TAG, faceMask);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        faceMask = input.getIntOr(FACE_MASK_TAG, UNSET_FACE_MASK);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt(FACE_MASK_TAG, faceMask);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Force la mise a jour visuelle cote client apres chargement ou reception
     * des donnees synchronisees.
     */
    private void refreshClientRender(boolean needsBlockUpdate) {
        Level currentLevel = level;
        if (currentLevel == null || !currentLevel.isClientSide()) {
            return;
        }
        requestModelDataUpdate();
        if (needsBlockUpdate) {
            currentLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
