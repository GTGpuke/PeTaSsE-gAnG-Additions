package com.petassegang.addons.backrooms.level.level0.client.model;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

import com.petassegang.addons.backrooms.level.level0.block.LevelZeroBaseboardBlock;
import com.petassegang.addons.backrooms.level.level0.block.LevelZeroWallpaperBlock;

/**
 * Modele adaptatif des plinthes du Level 0.
 */
public final class LevelZeroBaseboardBakedModel implements BakedModel {

    private static volatile RenderMaterial cachedMaterial;

    private final BakedModel itemModel;
    private final BakedModel northModel;
    private final BakedModel southModel;
    private final BakedModel westModel;
    private final BakedModel eastModel;

    public LevelZeroBaseboardBakedModel(BakedModel itemModel,
                                        BakedModel northModel,
                                        BakedModel southModel,
                                        BakedModel westModel,
                                        BakedModel eastModel) {
        this.itemModel = itemModel;
        this.northModel = northModel;
        this.southModel = southModel;
        this.westModel = westModel;
        this.eastModel = eastModel;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView,
                               BlockState state,
                               BlockPos pos,
                               Supplier<Random> randomSupplier,
                               RenderContext context) {
        int faceMask = state != null && state.contains(LevelZeroBaseboardBlock.FACE_MASK)
                ? state.get(LevelZeroBaseboardBlock.FACE_MASK)
                : 0;
        Random random = randomSupplier.get();
        RenderMaterial material = getMaterial();
        QuadEmitter emitter = context.getEmitter();

        emitMaskedModel(emitter, material, state, random,
                (faceMask & LevelZeroWallpaperBlock.NORTH_MASK) != 0,
                northModel);
        emitMaskedModel(emitter, material, state, random,
                (faceMask & LevelZeroWallpaperBlock.SOUTH_MASK) != 0,
                southModel);
        emitMaskedModel(emitter, material, state, random,
                (faceMask & LevelZeroWallpaperBlock.WEST_MASK) != 0,
                westModel);
        emitMaskedModel(emitter, material, state, random,
                (faceMask & LevelZeroWallpaperBlock.EAST_MASK) != 0,
                eastModel);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        ((FabricBakedModel) itemModel).emitItemQuads(stack, randomSupplier, context);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        return itemModel.getQuads(state, face, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return itemModel.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth() {
        return itemModel.hasDepth();
    }

    @Override
    public boolean isSideLit() {
        return itemModel.isSideLit();
    }

    @Override
    public boolean isBuiltin() {
        return itemModel.isBuiltin();
    }

    @Override
    public Sprite getParticleSprite() {
        return itemModel.getParticleSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return itemModel.getTransformation();
    }

    @Override
    public ModelOverrideList getOverrides() {
        return itemModel.getOverrides();
    }

    private static void emitMaskedModel(QuadEmitter emitter,
                                        RenderMaterial material,
                                        BlockState state,
                                        Random random,
                                        boolean enabled,
                                        BakedModel model) {
        if (!enabled || model == null) {
            return;
        }
        for (Direction dir : Direction.values()) {
            for (BakedQuad quad : model.getQuads(state, dir, random)) {
                emitter.fromVanilla(quad, material, dir);
                emitter.emit();
            }
        }
        for (BakedQuad quad : model.getQuads(state, null, random)) {
            emitter.fromVanilla(quad, material, null);
            emitter.emit();
        }
    }

    private static RenderMaterial getMaterial() {
        RenderMaterial mat = cachedMaterial;
        if (mat == null) {
            mat = RendererAccess.INSTANCE.getRenderer().materialFinder().find();
            cachedMaterial = mat;
        }
        return mat;
    }
}
