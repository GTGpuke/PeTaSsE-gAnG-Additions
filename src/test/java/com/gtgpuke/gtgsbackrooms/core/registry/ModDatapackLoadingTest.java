package com.gtgpuke.gtgsbackrooms.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.VanillaDataPackProvider;
import net.minecraft.server.SaveLoading;
import net.minecraft.server.command.CommandManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.storage.LevelStorage;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.LevelZeroChunkGenerator;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;
import com.gtgpuke.gtgsbackrooms.core.ModConstants;

/**
 * Test d'integration : le pipeline de chargement de monde de Minecraft (datapacks vanilla + mod,
 * registres dynamiques) doit enregistrer la dimension Level 0 avec son type et son generateur.
 *
 * <p>C'est exactement ce que fait un serveur ou un client au moment de creer ou charger un monde,
 * sans demarrer de serveur.
 */
@DisplayName("Chargement du datapack du mod")
class ModDatapackLoadingTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("Le pipeline de chargement de monde enregistre la dimension Level 0 depuis le datapack")
    void worldLoadingPipelineRegistersLevelZero(@TempDir Path dataPacksDirectory) throws Exception {
        ResourcePackManager packs = VanillaDataPackProvider.createManager(
                dataPacksDirectory,
                LevelStorage.createSymlinkFinder(dataPacksDirectory.resolve("allowed_symlinks.txt")));
        SaveLoading.ServerConfig config = new SaveLoading.ServerConfig(
                new SaveLoading.DataPacks(packs, DataConfiguration.SAFE_MODE, false, true),
                CommandManager.RegistrationEnvironment.DEDICATED,
                2);

        Registry<DimensionOptions> dimensions = SaveLoading.<Registry<DimensionOptions>, Registry<DimensionOptions>>load(
                config,
                context -> new SaveLoading.LoadContext<>(
                        context.dimensionsRegistryManager().get(RegistryKeys.DIMENSION),
                        context.dimensionsRegistryManager()),
                (resourceManager, dataPackContents, registries, dimensionRegistry) -> dimensionRegistry,
                Util.getMainWorkerExecutor(),
                Runnable::run).get(3, TimeUnit.MINUTES);

        Identifier levelZeroId = BackroomsConstants.LEVEL_ZERO_WORLD.getValue();
        DimensionOptions levelZero = dimensions.get(levelZeroId);
        assertNotNull(levelZero, "La dimension " + levelZeroId + " doit etre chargee depuis data/.../dimension/.");
        assertTrue(levelZero.chunkGenerator() instanceof LevelZeroChunkGenerator, "Le generateur est celui du Level 0.");
        assertEquals(Identifier.of(ModConstants.MOD_ID, "backrooms_level_0_type"),
                levelZero.dimensionTypeEntry().getKey().orElseThrow().getValue(), "Type de dimension du Level 0.");
        assertEquals(LevelZeroLayerStackLayout.MIN_Y, levelZero.dimensionTypeEntry().value().minY(), "Y minimal.");
        assertEquals(LevelZeroLayerStackLayout.DIMENSION_HEIGHT, levelZero.dimensionTypeEntry().value().height(), "Hauteur.");
        assertTrue(levelZero.chunkGenerator().getBiomeSource().getBiomes().stream()
                        .anyMatch(biome -> biome.getKey().map(key -> key.getValue().equals(Identifier.of(ModConstants.MOD_ID, "backrooms_level_0"))).orElse(false)),
                "Le biome du Level 0 est resolu depuis le datapack.");
        // Les dimensions vanilla viennent du preset de monde, pas du datapack : ce registre ne contient
        // que les dimensions declarees en JSON, la notre doit donc y etre seule.
        assertEquals(1, dimensions.size(), "Le datapack ne declare qu'une dimension.");
    }
}
