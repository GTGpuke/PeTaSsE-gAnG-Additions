package com.petassegang.addons.client.model;

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

import com.petassegang.addons.block.LevelZeroWallpaperBlock;

/**
 * Modele de bloc adaptatif qui choisit le papier peint selon le masque de faces
 * encode dans la propriete {@link LevelZeroWallpaperBlock#FACE_MASK} du block state.
 *
 * <p>Le masque est lu directement depuis le {@link BlockState} — aucune block
 * entity n'est consultee, ce qui supprime l'appel reseau et le cout GC
 * associes a {@code getBlockEntityRenderData()}.
 */
public final class LevelZeroWallpaperBakedModel implements BakedModel {

    /**
     * Materiau de rendu par defaut, mis en cache pour eviter une allocation
     * par appel a {@code emitBlockQuads()}.
     */
    private static volatile RenderMaterial cachedMaterial;

    private final BakedModel baseModel;
    private final BakedModel alternateModel;

    public LevelZeroWallpaperBakedModel(BakedModel baseModel, BakedModel alternateModel) {
        this.baseModel = baseModel;
        this.alternateModel = alternateModel;
    }

    // ── FabricBakedModel ─────────────────────────────────────────────────────

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos,
                               Supplier<Random> randomSupplier, RenderContext context) {
        // Lecture du masque directement depuis le block state — pas de block entity.
        int faceMask = state != null && state.contains(LevelZeroWallpaperBlock.FACE_MASK)
                ? state.get(LevelZeroWallpaperBlock.FACE_MASK)
                : 0;

        // Un seul appel a randomSupplier.get() — evite 7 allocations Random par rebuild de chunk.
        Random random = randomSupplier.get();
        RenderMaterial material = getMaterial();
        QuadEmitter emitter = context.getEmitter();

        for (Direction dir : Direction.values()) {
            BakedModel model = isAlternate(faceMask, dir) ? alternateModel : baseModel;
            for (BakedQuad quad : model.getQuads(state, dir, random)) {
                emitter.fromVanilla(quad, material, dir);
                emitter.emit();
            }
        }

        // Quads non-culles (faces internes / particules)
        for (BakedQuad quad : baseModel.getQuads(state, null, random)) {
            emitter.fromVanilla(quad, material, null);
            emitter.emit();
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        ((FabricBakedModel) baseModel).emitItemQuads(stack, randomSupplier, context);
    }

    // ── BakedModel (delegation vers baseModel) ────────────────────────────────

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random) {
        return baseModel.getQuads(state, face, random);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth() {
        return baseModel.hasDepth();
    }

    @Override
    public boolean isSideLit() {
        return baseModel.isSideLit();
    }

    @Override
    public boolean isBuiltin() {
        return baseModel.isBuiltin();
    }

    @Override
    public Sprite getParticleSprite() {
        return baseModel.getParticleSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return baseModel.getTransformation();
    }

    @Override
    public ModelOverrideList getOverrides() {
        return baseModel.getOverrides();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isAlternate(int faceMask, @Nullable Direction dir) {
        if (dir == null || dir.getAxis().isVertical()) {
            return false;
        }
        int mask = switch (dir) {
            case NORTH -> LevelZeroWallpaperBlock.NORTH_MASK;
            case SOUTH -> LevelZeroWallpaperBlock.SOUTH_MASK;
            case WEST  -> LevelZeroWallpaperBlock.WEST_MASK;
            case EAST  -> LevelZeroWallpaperBlock.EAST_MASK;
            default    -> 0;
        };
        return (faceMask & mask) != 0;
    }

    /** Retourne le materiau par defaut, en le creant a la premiere invocation. */
    private static RenderMaterial getMaterial() {
        RenderMaterial mat = cachedMaterial;
        if (mat == null) {
            mat = RendererAccess.INSTANCE.getRenderer().materialFinder().find();
            cachedMaterial = mat;
        }
        return mat;
    }
}
