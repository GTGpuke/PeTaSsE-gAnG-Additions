package com.gtgpuke.gtgsbackrooms;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Les classes utilitaires et de registre du mod ne s'instancient pas : classe finale, constructeur prive qui
 * refuse l'appel. Le test les enumere toutes pour qu'un oubli se voie.
 */
@DisplayName("Classes utilitaires")
class UtilityClassesTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    static Stream<String> utilityClasses() {
        return Stream.of(
                "com.gtgpuke.gtgsbackrooms.core.ModConstants",
                "com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants",
                "com.gtgpuke.gtgsbackrooms.config.ModConfig",
                "com.gtgpuke.gtgsbackrooms.init.ModNetworking",
                "com.gtgpuke.gtgsbackrooms.init.ModBlocks",
                "com.gtgpuke.gtgsbackrooms.init.ModItems",
                "com.gtgpuke.gtgsbackrooms.init.ModChunkGenerators",
                "com.gtgpuke.gtgsbackrooms.init.ModBlockEntities",
                "com.gtgpuke.gtgsbackrooms.init.ModEntities",
                "com.gtgpuke.gtgsbackrooms.init.ModCreativeTab",
                "com.gtgpuke.gtgsbackrooms.init.ModDataComponents",
                "com.gtgpuke.gtgsbackrooms.perf.ModPerformanceMonitor",
                "com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroCoords",
                "com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroLayerStackLayout",
                "com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroCellConnections",
                "com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroCellMicroPattern",
                "com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroLargeRooms",
                "com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroLightPattern",
                "com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroTieBreak",
                "com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.layout.LevelZeroTopology",
                "com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.noise.LevelZeroSeedResolver",
                "com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.noise.StageRandom",
                "com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.write.LevelZeroDebugPalette",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.block.PortalArea",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.block.PortalTargets",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.ClientPortalLight",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.client.sound.PortalSound",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.LazyVertexBuffers",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.PortalClipping",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.PortalRenderState",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.PortalViewTransform",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.ShaderClipInjector",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.ThirdPersonPortal",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.debug.DebugPortalCommand",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntityRegistry",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.interaction.RemoteInteractionHandler",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.interaction.RemoteMiningTracker",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.propagation.PortalPursuit",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.PortalNetwork",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.PortalSight",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.PortalViewTracker",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.RemoteChunkWindow",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.transition.DestinationLoader",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.transition.PortalCollision",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.transition.PortalRelevance",
                "com.gtgpuke.gtgsbackrooms.backrooms.portal.transition.TeleportExecutor");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("utilityClasses")
    @DisplayName("Classe finale, constructeur prive qui refuse l'instanciation")
    void cannotBeInstantiated(String className) throws ReflectiveOperationException {
        Class<?> type = Class.forName(className);
        assertTrue(Modifier.isFinal(type.getModifiers()), "La classe est finale.");
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        assertTrue(constructors.length == 1 && Modifier.isPrivate(constructors[0].getModifiers()), "Un seul constructeur, prive.");
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException failure = assertThrows(InvocationTargetException.class, constructor::newInstance, "L'appel echoue.");
        assertInstanceOf(UnsupportedOperationException.class, failure.getCause(), "Refus explicite.");
    }
}
