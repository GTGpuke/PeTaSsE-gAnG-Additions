package com.petassegang.addons.backrooms.level0.client.model;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

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

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;

import com.petassegang.addons.backrooms.level0.block.LevelZeroWallpaperBlock;
import com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;
import com.petassegang.addons.backrooms.level0.generation.write.LevelZeroWallProps;

/**
 * Modele du mur adaptatif : papier peint jauni ou blanc par face selon
 * {@link LevelZeroWallpaperBlock#FACE_MASK}, plus plinthes et petits elements
 * muraux dessines en overlay sur les faces exposees a l'air.
 *
 * <p>Les details muraux sont rederives cote client depuis les voisins d'air
 * et le resolveur deterministe : aucun bloc supplementaire n'existe dans le monde.
 */
public final class LevelZeroWallpaperBakedModel implements BakedModel {

    private static volatile RenderMaterial cachedMaterial;
    private static volatile RenderMaterial cachedOverlayMaterial;

    private final BakedModel baseModel;
    private final BakedModel alternateModel;
    private final DirectionalModels baseboards;
    private final DirectionalModels switches;
    private final DirectionalModels outlets;

    /**
     * Modeles d'un overlay pour les quatre faces horizontales.
     *
     * @param north modele face nord
     * @param south modele face sud
     * @param west modele face ouest
     * @param east modele face est
     */
    public record DirectionalModels(BakedModel north, BakedModel south, BakedModel west, BakedModel east) {

        /** Modele de la face donnee, {@code null} pour une face verticale. */
        @Nullable BakedModel forDirection(Direction direction) {
            return switch (direction) {
                case NORTH -> north;
                case SOUTH -> south;
                case WEST -> west;
                case EAST -> east;
                default -> null;
            };
        }
    }

    public LevelZeroWallpaperBakedModel(BakedModel baseModel,
                                        BakedModel alternateModel,
                                        DirectionalModels baseboards,
                                        DirectionalModels switches,
                                        DirectionalModels outlets) {
        this.baseModel = baseModel;
        this.alternateModel = alternateModel;
        this.baseboards = baseboards;
        this.switches = switches;
        this.outlets = outlets;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView blockView, BlockState state, BlockPos pos,
                               Supplier<Random> randomSupplier, RenderContext context) {
        int faceMask = state != null && state.contains(LevelZeroWallpaperBlock.FACE_MASK)
                ? state.get(LevelZeroWallpaperBlock.FACE_MASK)
                : 0;
        Random random = randomSupplier.get();
        RenderMaterial material = material();
        QuadEmitter emitter = context.getEmitter();
        LevelZeroWallProps props = pos != null ? sampleWallProps(blockView, pos) : LevelZeroWallProps.NONE;
        boolean baseboardRow = pos != null && LevelZeroLayerStackLayout.isCanonicalWallBaseY(pos.getY());

        for (Direction direction : Direction.values()) {
            int bit = LevelZeroWallpaperBlock.maskFor(direction);
            BakedModel model = bit != 0 && (faceMask & bit) != 0 ? alternateModel : baseModel;
            emitQuads(emitter, material, state, random, direction, model);
            if (bit == 0) {
                continue;
            }
            if (baseboardRow && (props.baseboardFaceMask() & bit) != 0) {
                emitOverlay(emitter, material, state, random, direction, baseboards.forDirection(direction));
            }
            if (props.hasFixture() && pos.getY() == props.fixtureY() && (props.fixtureFaceMask() & bit) != 0) {
                DirectionalModels fixtureModels = props.fixture() == LevelZeroWallProps.Fixture.SWITCH ? switches : outlets;
                emitOverlay(emitter, overlayMaterial(), state, random, direction, fixtureModels.forDirection(direction));
            }
        }
        for (BakedQuad quad : baseModel.getQuads(state, null, random)) {
            emitter.fromVanilla(quad, material, null);
            emitter.emit();
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context) {
        ((FabricBakedModel) baseModel).emitItemQuads(stack, randomSupplier, context);
    }

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

    private static void emitQuads(QuadEmitter emitter, RenderMaterial material, BlockState state, Random random,
                                  Direction direction, BakedModel model) {
        for (BakedQuad quad : model.getQuads(state, direction, random)) {
            emitter.fromVanilla(quad, material, direction);
            emitter.emit();
        }
    }

    private static void emitOverlay(QuadEmitter emitter, RenderMaterial material, BlockState state, Random random,
                                    Direction direction, @Nullable BakedModel model) {
        if (model == null) {
            return;
        }
        emitQuads(emitter, material, state, random, direction, model);
        for (BakedQuad quad : model.getQuads(state, null, random)) {
            emitter.fromVanilla(quad, material, null);
            emitter.emit();
        }
    }

    private static LevelZeroWallProps sampleWallProps(BlockRenderView blockView, BlockPos pos) {
        if (!LevelZeroLayerStackLayout.isInsideStack(pos.getY())) {
            return LevelZeroWallProps.NONE;
        }
        int exposedMask = 0;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            if (blockView.getBlockState(pos.offset(direction)).isAir()) {
                exposedMask |= LevelZeroWallpaperBlock.maskFor(direction);
            }
        }
        if (exposedMask == 0) {
            return LevelZeroWallProps.NONE;
        }
        return LevelZeroWallProps.resolve(pos.getX(), pos.getZ(), exposedMask,
                LevelZeroLayerStackLayout.sliceAtY(pos.getY()));
    }

    private static RenderMaterial material() {
        RenderMaterial material = cachedMaterial;
        if (material == null) {
            material = RendererAccess.INSTANCE.getRenderer().materialFinder().find();
            cachedMaterial = material;
        }
        return material;
    }

    private static RenderMaterial overlayMaterial() {
        RenderMaterial material = cachedOverlayMaterial;
        if (material == null) {
            material = RendererAccess.INSTANCE.getRenderer().materialFinder().blendMode(BlendMode.TRANSLUCENT).find();
            cachedOverlayMaterial = material;
        }
        return material;
    }
}
