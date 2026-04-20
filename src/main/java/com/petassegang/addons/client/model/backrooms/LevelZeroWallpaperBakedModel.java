package com.petassegang.addons.client.model.backrooms;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
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

import com.petassegang.addons.block.backrooms.LevelZeroWallpaperBlock;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroLayerStackLayout;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroVerticalSlice;
import com.petassegang.addons.world.backrooms.level0.coord.LevelZeroVerticalLayout;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroBaseboardStyle;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroWallFixture;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroWallPropProfile;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroWallPropResolver;

/**
 * Modele de bloc adaptatif qui choisit le papier peint selon le masque de faces
 * encode dans la propriete {@link LevelZeroWallpaperBlock#FACE_MASK} du block state.
 *
 * <p>Le masque est lu directement depuis le {@link BlockState} — aucune block
 * entity n'est consultee, ce qui supprime l'appel reseau et le cout GC
 * associes a {@code getBlockEntityRenderData()}.
 */
public final class LevelZeroWallpaperBakedModel implements BakedModel {

    private static final LevelZeroVerticalSlice LEGACY_VERTICAL_SLICE =
            LevelZeroVerticalSlice.legacySingleLayer();

    /**
     * Materiau de rendu par defaut, mis en cache pour eviter une allocation
     * par appel a {@code emitBlockQuads()}.
     */
    private static volatile RenderMaterial cachedMaterial;
    private static volatile RenderMaterial cachedOverlayMaterial;

    private final BakedModel baseModel;
    private final BakedModel alternateModel;
    private final BakedModel northBaseboardModel;
    private final BakedModel southBaseboardModel;
    private final BakedModel westBaseboardModel;
    private final BakedModel eastBaseboardModel;
    private final BakedModel northSwitchModel;
    private final BakedModel southSwitchModel;
    private final BakedModel westSwitchModel;
    private final BakedModel eastSwitchModel;
    private final BakedModel northOutletModel;
    private final BakedModel southOutletModel;
    private final BakedModel westOutletModel;
    private final BakedModel eastOutletModel;

    public LevelZeroWallpaperBakedModel(BakedModel baseModel,
                                        BakedModel alternateModel,
                                        BakedModel northBaseboardModel,
                                        BakedModel southBaseboardModel,
                                        BakedModel westBaseboardModel,
                                        BakedModel eastBaseboardModel,
                                        BakedModel northSwitchModel,
                                        BakedModel southSwitchModel,
                                        BakedModel westSwitchModel,
                                        BakedModel eastSwitchModel,
                                        BakedModel northOutletModel,
                                        BakedModel southOutletModel,
                                        BakedModel westOutletModel,
                                        BakedModel eastOutletModel) {
        this.baseModel = baseModel;
        this.alternateModel = alternateModel;
        this.northBaseboardModel = northBaseboardModel;
        this.southBaseboardModel = southBaseboardModel;
        this.westBaseboardModel = westBaseboardModel;
        this.eastBaseboardModel = eastBaseboardModel;
        this.northSwitchModel = northSwitchModel;
        this.southSwitchModel = southSwitchModel;
        this.westSwitchModel = westSwitchModel;
        this.eastSwitchModel = eastSwitchModel;
        this.northOutletModel = northOutletModel;
        this.southOutletModel = southOutletModel;
        this.westOutletModel = westOutletModel;
        this.eastOutletModel = eastOutletModel;
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
        RenderMaterial overlayMaterial = getOverlayMaterial();
        QuadEmitter emitter = context.getEmitter();
        LevelZeroWallPropProfile wallPropProfile = pos != null
                ? sampleWallPropProfile(blockView, pos)
                : LevelZeroWallPropProfile.none();
        // TODO Level 0: rebrancher ici les overlays surfaciques si la couche
        // `surface details` repasse en rendu actif. Pour l'instant, ce chemin
        // reste volontairement retire du runtime client.

        for (Direction dir : Direction.values()) {
            BakedModel model = isAlternate(faceMask, dir) ? alternateModel : baseModel;
            for (BakedQuad quad : model.getQuads(state, dir, random)) {
                emitter.fromVanilla(quad, material, dir);
                emitter.emit();
            }
            emitBaseboardOverlay(emitter, material, state, random, dir, wallPropProfile);
            emitFixtureOverlay(emitter, overlayMaterial, state, pos, random, dir, wallPropProfile);
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

    private void emitBaseboardOverlay(QuadEmitter emitter,
                                      RenderMaterial material,
                                      BlockState state,
                                      Random random,
                                      @Nullable Direction dir,
                                      LevelZeroWallPropProfile baseboardProfile) {
        if (dir == null || dir.getAxis().isVertical()) {
            return;
        }
        int mask = switch (dir) {
            case NORTH -> LevelZeroWallpaperBlock.NORTH_MASK;
            case SOUTH -> LevelZeroWallpaperBlock.SOUTH_MASK;
            case WEST -> LevelZeroWallpaperBlock.WEST_MASK;
            case EAST -> LevelZeroWallpaperBlock.EAST_MASK;
            default -> 0;
        };
        if (!baseboardProfile.hasBaseboard() || (baseboardProfile.baseboardFaceMask() & mask) == 0) {
            return;
        }
        BakedModel model = modelForBaseboard(baseboardProfile.baseboardStyle(), dir);
        if (model == null) {
            return;
        }
        for (BakedQuad quad : model.getQuads(state, dir, random)) {
            emitter.fromVanilla(quad, material, dir);
            emitter.emit();
        }
        for (BakedQuad quad : model.getQuads(state, null, random)) {
            emitter.fromVanilla(quad, material, null);
            emitter.emit();
        }
    }

    private void emitFixtureOverlay(QuadEmitter emitter,
                                    RenderMaterial material,
                                    BlockState state,
                                    @Nullable BlockPos pos,
                                    Random random,
                                    @Nullable Direction dir,
                                    LevelZeroWallPropProfile wallPropProfile) {
        if (pos == null || dir == null || dir.getAxis().isVertical()) {
            return;
        }
        if (!wallPropProfile.hasFixture() || pos.getY() != wallPropProfile.fixtureY()) {
            return;
        }
        int mask = maskFor(dir);
        if ((wallPropProfile.fixtureFaceMask() & mask) == 0) {
            return;
        }
        BakedModel model = modelForFixture(wallPropProfile.fixture(), dir);
        if (model == null) {
            return;
        }
        for (BakedQuad quad : model.getQuads(state, dir, random)) {
            emitter.fromVanilla(quad, material, dir);
            emitter.emit();
        }
        for (BakedQuad quad : model.getQuads(state, null, random)) {
            emitter.fromVanilla(quad, material, null);
            emitter.emit();
        }
    }

    private BakedModel modelForBaseboard(LevelZeroBaseboardStyle style, Direction dir) {
        return switch (style) {
            case WHITE -> switch (dir) {
                case NORTH -> northBaseboardModel;
                case SOUTH -> southBaseboardModel;
                case WEST -> westBaseboardModel;
                case EAST -> eastBaseboardModel;
                default -> null;
            };
            default -> null;
        };
    }

    private BakedModel modelForFixture(LevelZeroWallFixture fixture, Direction dir) {
        return switch (fixture) {
            case SWITCH -> switch (dir) {
                case NORTH -> northSwitchModel;
                case SOUTH -> southSwitchModel;
                case WEST -> westSwitchModel;
                case EAST -> eastSwitchModel;
                default -> null;
            };
            case OUTLET -> switch (dir) {
                case NORTH -> northOutletModel;
                case SOUTH -> southOutletModel;
                case WEST -> westOutletModel;
                case EAST -> eastOutletModel;
                default -> null;
            };
            default -> null;
        };
    }

    private static LevelZeroWallPropProfile sampleWallPropProfile(BlockRenderView blockView, BlockPos pos) {
        int mask = 0;
        if (blockView.getBlockState(pos.north()).isAir()) {
            mask |= LevelZeroWallpaperBlock.NORTH_MASK;
        }
        if (blockView.getBlockState(pos.south()).isAir()) {
            mask |= LevelZeroWallpaperBlock.SOUTH_MASK;
        }
        if (blockView.getBlockState(pos.west()).isAir()) {
            mask |= LevelZeroWallpaperBlock.WEST_MASK;
        }
        if (blockView.getBlockState(pos.east()).isAir()) {
            mask |= LevelZeroWallpaperBlock.EAST_MASK;
        }
        if (mask == 0) {
            return LevelZeroWallPropProfile.none();
        }
        // Le rendu client rederive les wall props a partir des faces exposees
        // du mur. Cela permet de garder les plinthes en simple detail visuel
        // sans poser de bloc supplementaire dans le monde.
        LevelZeroVerticalSlice slice = verticalSliceForWallY(pos.getY());
        LevelZeroWallPropProfile fullProfile = LevelZeroWallPropResolver.resolveAtWorld(
                pos.getX(),
                pos.getZ(),
                mask,
                slice);
        int baseboardFaceMask = isWallBaseY(pos.getY())
                ? fullProfile.baseboardFaceMask()
                : 0;
        LevelZeroBaseboardStyle baseboardStyle = baseboardFaceMask != 0
                ? fullProfile.baseboardStyle()
                : LevelZeroBaseboardStyle.NONE;
        LevelZeroWallFixture fixture = pos.getY() == fullProfile.fixtureY()
                ? fullProfile.fixture()
                : LevelZeroWallFixture.NONE;
        int fixtureFaceMask = fixture != LevelZeroWallFixture.NONE
                ? fullProfile.fixtureFaceMask()
                : 0;
        int fixtureY = fixture != LevelZeroWallFixture.NONE
                ? fullProfile.fixtureY()
                : 0;
        if (baseboardFaceMask == 0 && fixture == LevelZeroWallFixture.NONE) {
            return LevelZeroWallPropProfile.none();
        }
        return new LevelZeroWallPropProfile(
                baseboardFaceMask,
                baseboardStyle,
                fixture,
                fixtureFaceMask,
                fixtureY);
    }

    private static boolean isWallBaseY(int y) {
        return y == LevelZeroVerticalLayout.airMinY()
                || LevelZeroLayerStackLayout.isCanonicalWallBaseY(y);
    }

    private static LevelZeroVerticalSlice verticalSliceForWallY(int y) {
        if (y >= LevelZeroLayerStackLayout.minimumY()
                && y < LevelZeroLayerStackLayout.recommendedWorldHeight()) {
            return LevelZeroLayerStackLayout.sliceAtY(y);
        }
        return LEGACY_VERTICAL_SLICE;
    }

    private static int maskFor(Direction dir) {
        return switch (dir) {
            case NORTH -> LevelZeroWallpaperBlock.NORTH_MASK;
            case SOUTH -> LevelZeroWallpaperBlock.SOUTH_MASK;
            case WEST -> LevelZeroWallpaperBlock.WEST_MASK;
            case EAST -> LevelZeroWallpaperBlock.EAST_MASK;
            default -> 0;
        };
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

    /** Retourne le materiau translucide utilise pour les overlays. */
    private static RenderMaterial getOverlayMaterial() {
        RenderMaterial mat = cachedOverlayMaterial;
        if (mat == null) {
            mat = RendererAccess.INSTANCE.getRenderer()
                    .materialFinder()
                    .blendMode(BlendMode.TRANSLUCENT)
                    .find();
            cachedOverlayMaterial = mat;
        }
        return mat;
    }
}
