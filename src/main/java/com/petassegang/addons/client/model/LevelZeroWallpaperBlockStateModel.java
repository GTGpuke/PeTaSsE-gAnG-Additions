package com.petassegang.addons.client.model;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import com.petassegang.addons.util.ModConstants;
import com.petassegang.addons.world.backrooms.BackroomsConstants;
import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
/**
 * Modele de bloc adaptatif qui choisit le bon papier peint selon la face
 * exposee et le biome de surface du couloir adjacent.
 */
public final class LevelZeroWallpaperBlockStateModel implements BlockStateModel {

    private static final int NORTH_MASK = 1;
    private static final int SOUTH_MASK = 1 << 1;
    private static final int WEST_MASK = 1 << 2;
    private static final int EAST_MASK = 1 << 3;
    private static final int FULL_MASK = NORTH_MASK | SOUTH_MASK | WEST_MASK | EAST_MASK;
    private static final int MAX_SURFACE_PROBE_DISTANCE = BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 4;
    private static final int FALLBACK_BIOME_SAMPLE_DISTANCE = BackroomsConstants.LEVEL_ZERO_CELL_SCALE * 2;
    private static boolean mixedFacesLogged;
    private static final ModelProperty<Integer> FACE_MASK = new ModelProperty<>();
    private static final ModelData[] PRECOMPUTED_MODEL_DATA = buildPrecomputedModelData();

    private final BlockStateModel baseModel;
    private final BlockStateModel alternateModel;
    private final List<List<BlockStateModelPart>> partsByMask;

    /**
     * Construit un modele adaptatif pour le papier peint du Level 0.
     *
     * @param baseModel modele du papier peint jaune
     * @param alternateModel modele du papier peint blanc
     */
    public LevelZeroWallpaperBlockStateModel(BlockStateModel baseModel, BlockStateModel alternateModel) {
        this.baseModel = baseModel;
        this.alternateModel = alternateModel;
        this.partsByMask = createPartBuckets();
        initializeParts();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        output.addAll(partsByMask.get(0));
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output, ModelData data) {
        output.addAll(partsByMask.get(faceMask(data)));
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level,
                                           @NotNull BlockPos pos,
                                           @NotNull BlockState state,
                                           @NotNull ModelData modelData) {
        return modelData;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Material.Baked particleMaterial() {
        return baseModel.particleMaterial();
    }

    @Override
    @SuppressWarnings("deprecation")
    public Material.Baked particleMaterial(@NotNull ModelData data) {
        return faceMask(data) == FULL_MASK ? alternateModel.particleMaterial() : baseModel.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return baseModel.materialFlags() | alternateModel.materialFlags();
    }

    private void initializeParts() {
        List<BlockStateModelPart> baseParts = collectStableParts(baseModel);
        List<BlockStateModelPart> alternateParts = collectStableParts(alternateModel);
        int partCount = Math.min(baseParts.size(), alternateParts.size());

        partsByMask.set(0, List.copyOf(baseParts));
        partsByMask.set(FULL_MASK, List.copyOf(alternateParts));

        for (int faceMask = 1; faceMask < FULL_MASK; faceMask++) {
            ArrayList<BlockStateModelPart> mixedParts = new ArrayList<>(partCount);
            for (int partIndex = 0; partIndex < partCount; partIndex++) {
                mixedParts.add(new MixedPart(baseParts.get(partIndex), alternateParts.get(partIndex), faceMask));
            }
            partsByMask.set(faceMask, List.copyOf(mixedParts));
        }
    }

    @SuppressWarnings("deprecation")
    private static List<BlockStateModelPart> collectStableParts(BlockStateModel model) {
        ArrayList<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0L), parts);
        return List.copyOf(parts);
    }

    private static List<List<BlockStateModelPart>> createPartBuckets() {
        ArrayList<List<BlockStateModelPart>> buckets = new ArrayList<>(FULL_MASK + 1);
        for (int index = 0; index <= FULL_MASK; index++) {
            buckets.add(List.of());
        }
        return buckets;
    }

    private static int faceMask(@Nullable ModelData data) {
        if (data == null) {
            return 0;
        }
        Integer value = data.get(FACE_MASK);
        return value == null ? 0 : value;
    }

    /**
     * Cree la ModelData du papier peint pour une position monde donnee.
     *
     * @param faceMask masque pre-calcule des faces alternatives
     * @return donnees de rendu pre-calculees pour ce mur
     */
    public static @NotNull ModelData createModelData(int faceMask) {
        if (faceMask <= 0) {
            return ModelData.EMPTY;
        }
        return PRECOMPUTED_MODEL_DATA[Math.min(faceMask, FULL_MASK)];
    }

    /**
     * Calcule le masque de faces d'un mur a partir du monde.
     *
     * @param level acces bloc du niveau
     * @param pos position du mur
     * @return masque des faces alternatives
     */
    public static int sampleFaceMask(BlockGetter level, BlockPos pos) {
        int faceMask = 0;
        if (isAlternate(level, pos, Direction.NORTH)) {
            faceMask |= NORTH_MASK;
        }
        if (isAlternate(level, pos, Direction.SOUTH)) {
            faceMask |= SOUTH_MASK;
        }
        if (isAlternate(level, pos, Direction.WEST)) {
            faceMask |= WEST_MASK;
        }
        if (isAlternate(level, pos, Direction.EAST)) {
            faceMask |= EAST_MASK;
        }
        if (!mixedFacesLogged && faceMask != 0 && faceMask != FULL_MASK) {
            mixedFacesLogged = true;
            ModConstants.LOGGER.info("Des faces mixtes de papier peint du Level 0 ont ete detectees cote client.");
        }
        return faceMask;
    }

    private static boolean isAlternate(BlockGetter level, BlockPos pos, Direction direction) {
        if (direction.getAxis().isVertical()) {
            return false;
        }
        LevelZeroSurfaceBiome biome = findVisibleSurfaceBiome(level, pos, direction);
        return biome == LevelZeroSurfaceBiome.RED;
    }

    private static LevelZeroSurfaceBiome findVisibleSurfaceBiome(BlockGetter level,
                                                                 BlockPos origin,
                                                                 Direction direction) {
        for (int distance = 1; distance <= MAX_SURFACE_PROBE_DISTANCE; distance++) {
            int sampleX = origin.getX() + direction.getStepX() * distance;
            int sampleZ = origin.getZ() + direction.getStepZ() * distance;
            BlockPos airPos = new BlockPos(sampleX, BackroomsConstants.LEVEL_ZERO_AIR_MIN_Y, sampleZ);
            if (!level.getBlockState(airPos).isAir()) {
                continue;
            }
            return LevelZeroSurfaceBiome.sampleAtWorld(sampleX, sampleZ);
        }

        int fallbackX = origin.getX() + direction.getStepX() * FALLBACK_BIOME_SAMPLE_DISTANCE;
        int fallbackZ = origin.getZ() + direction.getStepZ() * FALLBACK_BIOME_SAMPLE_DISTANCE;
        return LevelZeroSurfaceBiome.sampleAtWorld(fallbackX, fallbackZ);
    }

    private static boolean isAlternate(int faceMask, @Nullable Direction direction) {
        if (direction == null) {
            return false;
        }
        int mask = switch (direction) {
            case NORTH -> NORTH_MASK;
            case SOUTH -> SOUTH_MASK;
            case WEST -> WEST_MASK;
            case EAST -> EAST_MASK;
            default -> 0;
        };
        return (faceMask & mask) != 0;
    }

    private static ModelData[] buildPrecomputedModelData() {
        ModelData[] data = new ModelData[FULL_MASK + 1];
        data[0] = ModelData.EMPTY;
        for (int faceMask = 1; faceMask <= FULL_MASK; faceMask++) {
            data[faceMask] = ModelData.builder().with(FACE_MASK, faceMask).build();
        }
        return data;
    }

    /**
     * Part de modele qui choisit les quads a afficher face par face.
     */
    private static final class MixedPart implements BlockStateModelPart {

        private final BlockStateModelPart basePart;
        private final BlockStateModelPart alternatePart;
        private final int faceMask;

        private MixedPart(BlockStateModelPart basePart, BlockStateModelPart alternatePart, int faceMask) {
            this.basePart = basePart;
            this.alternatePart = alternatePart;
            this.faceMask = faceMask;
        }

        @Override
        public List<net.minecraft.client.resources.model.geometry.BakedQuad> getQuads(@Nullable Direction direction) {
            if (direction == null || direction.getAxis().isVertical()) {
                return basePart.getQuads(direction);
            }
            return isAlternate(faceMask, direction) ? alternatePart.getQuads(direction) : basePart.getQuads(direction);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return basePart.useAmbientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return basePart.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return basePart.materialFlags() | alternatePart.materialFlags();
        }
    }
}
