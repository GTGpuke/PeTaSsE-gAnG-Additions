package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.serialization.Lifecycle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroVerticalSlice;
import com.gtgpuke.gtgsbackrooms.init.ModBlocks;

@DisplayName("Generateur de chunks du Level 0")
class LevelZeroChunkGeneratorTest {

    private static final HeightLimitView HEIGHT = new HeightLimitView() {
        @Override
        public int getHeight() {
            return LevelZeroLayerStackLayout.DIMENSION_HEIGHT;
        }

        @Override
        public int getBottomY() {
            return LevelZeroLayerStackLayout.MIN_Y;
        }
    };

    private static LevelZeroChunkGenerator generator;
    private static NoiseConfig noiseConfig;
    private static Registry<Biome> biomes;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
        Biome biome = new Biome.Builder()
                .precipitation(false)
                .temperature(0.5f)
                .downfall(0.0f)
                .effects(new BiomeEffects.Builder().waterColor(0).waterFogColor(0).fogColor(0).skyColor(0).build())
                .spawnSettings(SpawnSettings.INSTANCE)
                .generationSettings(GenerationSettings.INSTANCE)
                .build();
        SimpleRegistry<Biome> registry = new SimpleRegistry<>(RegistryKeys.BIOME, Lifecycle.stable());
        Registry.register(registry, BiomeKeys.PLAINS, biome);
        registry.freeze();
        biomes = registry;
        generator = new LevelZeroChunkGenerator(RegistryEntry.of(biome));
        noiseConfig = NoiseConfig.create(
                ChunkGeneratorSettings.createMissingSettings(),
                BuiltinRegistries.createWrapperLookup().getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS),
                4242L);
    }

    private static ProtoChunk generate(int chunkX, int chunkZ) {
        ProtoChunk chunk = new ProtoChunk(new ChunkPos(chunkX, chunkZ), UpgradeData.NO_UPGRADE_DATA, HEIGHT, biomes, null);
        generator.populateNoise(Blender.getNoBlending(), noiseConfig, null, chunk).join();
        return chunk;
    }

    @Test
    @DisplayName("Les dimensions annoncees correspondent a la pile verticale")
    void dimensions() {
        assertEquals(LevelZeroLayerStackLayout.DIMENSION_HEIGHT, generator.getWorldHeight(), "Hauteur de dimension.");
        assertEquals(LevelZeroLayerStackLayout.MIN_Y, generator.getMinimumY(), "Y minimal.");
        assertEquals(LevelZeroLayerStackLayout.floorY(0), generator.getSeaLevel(), "Niveau de mer technique.");
        assertEquals(LevelZeroLayerStackLayout.heightmapTopY(),
                generator.getHeight(5, 5, Heightmap.Type.WORLD_SURFACE_WG, HEIGHT, noiseConfig), "Sommet du heightmap.");
        assertEquals(LevelZeroChunkGenerator.CODEC, generator.getCodec(), "Codec expose.");
    }

    @Test
    @DisplayName("Les etapes de surface, de grottes et de spawn sont des no-op et le F3 decrit le layer")
    void noOpStagesAndDebugHud() {
        ProtoChunk chunk = generate(0, 0);
        BlockState before = chunk.getBlockState(new BlockPos(1, 1, 1));
        generator.buildSurface(null, null, noiseConfig, chunk);
        generator.carve(null, 0L, noiseConfig, null, null, chunk, null);
        generator.populateEntities(null);
        assertEquals(before, chunk.getBlockState(new BlockPos(1, 1, 1)), "Aucune etape ne modifie le chunk.");

        java.util.List<String> hud = new java.util.ArrayList<>();
        generator.getDebugHudText(hud, noiseConfig, new BlockPos(3, 23, 3));
        assertEquals("Backrooms : Level 0", hud.get(0), "Entete du F3.");
        assertTrue(hud.get(1).startsWith("Layer : 2"), "Le F3 indique le layer du joueur : " + hud.get(1));
        hud.clear();
        generator.getDebugHudText(hud, noiseConfig, new BlockPos(3, 60, 3));
        assertEquals(1, hud.size(), "Hors de la pile, seule l'entete est affichee.");
    }

    @Test
    @DisplayName("Le chunk d'origine est jouable sur les cinq layers")
    void originChunkIsPlayableOnEveryLayer() {
        ProtoChunk chunk = generate(0, 0);
        for (LevelZeroVerticalSlice slice : LevelZeroLayerStackLayout.slices()) {
            BlockPos center = new BlockPos(1, slice.floorY(), 1);
            BlockState floor = chunk.getBlockState(center);
            assertTrue(floor.isOf(ModBlocks.LEVEL_ZERO_DAMP_CARPET) || floor.isOf(ModBlocks.LEVEL_ZERO_DAMP_CARPET_AGED),
                    "Sol en moquette au layer " + slice.layerIndex() + ".");
            for (int y = slice.airMinY(); y <= slice.airMaxY(); y++) {
                assertTrue(chunk.getBlockState(new BlockPos(1, y, 1)).isAir(), "Air jouable en Y=" + y + ".");
            }
            BlockState ceiling = chunk.getBlockState(new BlockPos(1, slice.ceilingY(), 1));
            assertTrue(ceiling.isOf(ModBlocks.LEVEL_ZERO_CEILING_TILE) || ceiling.isOf(ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT),
                    "Plafond au layer " + slice.layerIndex() + ".");
            for (int y = slice.bedrockMinY(); y <= slice.bedrockMaxY(); y++) {
                assertTrue(chunk.getBlockState(new BlockPos(1, y, 1)).isOf(Blocks.BEDROCK), "Bedrock inter-layer en Y=" + y + ".");
            }
        }
        for (int y = LevelZeroLayerStackLayout.worldHeight(); y < LevelZeroLayerStackLayout.DIMENSION_HEIGHT; y++) {
            assertTrue(chunk.getBlockState(new BlockPos(7, y, 7)).isAir(), "Rien au-dessus de la pile en Y=" + y + ".");
        }
    }

    @Test
    @DisplayName("Un chunk eloigne contient des murs visibles, des murs enfouis et des neons")
    void farChunkHasWallsAndLights() {
        ProtoChunk chunk = generate(9, -4);
        int wallpaper = 0;
        int bedrockWalls = 0;
        int lights = 0;
        int airColumns = 0;
        LevelZeroVerticalSlice slice = LevelZeroVerticalSlice.of(3);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockState interior = chunk.getBlockState(new BlockPos(x, slice.airMinY(), z));
                wallpaper += interior.isOf(ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE) ? 1 : 0;
                bedrockWalls += interior.isOf(Blocks.BEDROCK) ? 1 : 0;
                airColumns += interior.isAir() ? 1 : 0;
                lights += chunk.getBlockState(new BlockPos(x, slice.ceilingY(), z)).isOf(ModBlocks.LEVEL_ZERO_FLUORESCENT_LIGHT) ? 1 : 0;
            }
        }
        assertEquals(256, wallpaper + bedrockWalls + airColumns, "Chaque colonne est soit un mur, soit de l'air.");
        assertTrue(wallpaper > 0, "Des murs visibles.");
        assertTrue(bedrockWalls > 0, "Des murs enfouis.");
        assertTrue(airColumns > 0, "Des couloirs.");
        assertTrue(lights > 0, "Des neons.");
    }

    @Test
    @DisplayName("L'echantillon de colonne reproduit exactement le chunk genere")
    void columnSampleMatchesPopulatedChunk() {
        int chunkX = 3;
        int chunkZ = 5;
        ProtoChunk chunk = generate(chunkX, chunkZ);
        for (int localX = 0; localX < 16; localX += 3) {
            for (int localZ = 0; localZ < 16; localZ += 5) {
                int worldX = chunkX * 16 + localX;
                int worldZ = chunkZ * 16 + localZ;
                VerticalBlockSample sample = generator.getColumnSample(worldX, worldZ, HEIGHT, noiseConfig);
                for (int y = 0; y < LevelZeroLayerStackLayout.DIMENSION_HEIGHT; y++) {
                    assertEquals(chunk.getBlockState(new BlockPos(localX, y, localZ)), sample.getState(y),
                            "Colonne " + worldX + "," + worldZ + " en Y=" + y + ".");
                }
            }
        }
    }

    @Test
    @DisplayName("Deux generations du meme chunk sont identiques, deux seeds different")
    void deterministic() {
        ProtoChunk first = generate(-7, 11);
        ProtoChunk second = generate(-7, 11);
        NoiseConfig otherConfig = NoiseConfig.create(
                ChunkGeneratorSettings.createMissingSettings(),
                BuiltinRegistries.createWrapperLookup().getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS),
                99L);
        ProtoChunk other = new ProtoChunk(new ChunkPos(-7, 11), UpgradeData.NO_UPGRADE_DATA, HEIGHT, biomes, null);
        generator.populateNoise(Blender.getNoBlending(), otherConfig, null, other).join();
        int differences = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                BlockPos pos = new BlockPos(x, 1, z);
                assertEquals(first.getBlockState(pos), second.getBlockState(pos), "Generation deterministe.");
                differences += first.getBlockState(pos).equals(other.getBlockState(pos)) ? 0 : 1;
            }
        }
        assertTrue(differences > 0, "Une autre seed monde change le labyrinthe.");
    }
}
