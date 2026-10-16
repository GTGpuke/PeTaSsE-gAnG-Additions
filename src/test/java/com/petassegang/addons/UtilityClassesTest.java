package com.petassegang.addons;

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
                "com.petassegang.addons.core.ModConstants",
                "com.petassegang.addons.backrooms.BackroomsConstants",
                "com.petassegang.addons.init.ModNetworking",
                "com.petassegang.addons.init.ModBlocks",
                "com.petassegang.addons.init.ModItems",
                "com.petassegang.addons.init.ModChunkGenerators",
                "com.petassegang.addons.init.ModBlockEntities",
                "com.petassegang.addons.init.ModCreativeTab",
                "com.petassegang.addons.perf.ModPerformanceMonitor",
                "com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroCoords",
                "com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroLayerStackLayout",
                "com.petassegang.addons.backrooms.level0.generation.layout.LevelZeroCellConnections",
                "com.petassegang.addons.backrooms.level0.generation.layout.LevelZeroCellMicroPattern",
                "com.petassegang.addons.backrooms.level0.generation.layout.LevelZeroLargeRooms",
                "com.petassegang.addons.backrooms.level0.generation.layout.LevelZeroLightPattern",
                "com.petassegang.addons.backrooms.level0.generation.layout.LevelZeroTieBreak",
                "com.petassegang.addons.backrooms.level0.generation.layout.LevelZeroTopology",
                "com.petassegang.addons.backrooms.level0.generation.noise.LevelZeroSeedResolver",
                "com.petassegang.addons.backrooms.level0.generation.noise.StageRandom",
                "com.petassegang.addons.backrooms.level0.generation.write.LevelZeroDebugPalette",
                "com.petassegang.addons.backrooms.portal.block.BackroomsDoorTargets",
                "com.petassegang.addons.backrooms.portal.client.view.PortalRenderState",
                "com.petassegang.addons.backrooms.portal.client.view.PortalViewTransform",
                "com.petassegang.addons.backrooms.portal.debug.DebugPortalCommand",
                "com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntityRegistry",
                "com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntityType",
                "com.petassegang.addons.backrooms.portal.interaction.RemoteInteractionHandler",
                "com.petassegang.addons.backrooms.portal.interaction.RemoteMiningTracker",
                "com.petassegang.addons.backrooms.portal.sync.PortalViewTracker",
                "com.petassegang.addons.backrooms.portal.sync.RemoteChunkWindow",
                "com.petassegang.addons.backrooms.portal.transition.DestinationLoader",
                "com.petassegang.addons.backrooms.portal.transition.PortalRelevance",
                "com.petassegang.addons.backrooms.portal.transition.TeleportExecutor",
                "com.petassegang.addons.backrooms.portal.transition.TpCooldownTracker");
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
