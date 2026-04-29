package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroWallPropWriteStage;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifie le contrat declaratif du stage debug des details muraux.
 */
@DisplayName("Wall prop write stage du Level 0")
class BackroomsLevelZeroWallPropWriteStageTest {

    @Test
    @DisplayName("Le stage expose toujours son constructeur par defaut")
    void testDefaultConstructorExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroWallPropWriteStage.class.getDeclaredConstructor(),
                "Le stage des details muraux doit exposer son constructeur par defaut.");
    }

    @Test
    @DisplayName("Le stage expose toujours son constructeur avec toggle")
    void testBooleanConstructorExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroWallPropWriteStage.class.getDeclaredConstructor(boolean.class),
                "Le stage des details muraux doit exposer son constructeur avec toggle.");
    }

    @Test
    @DisplayName("Le stage expose toujours son constructeur injectable pour les tests")
    void testInjectableConstructorExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroWallPropWriteStage.class.getDeclaredConstructor(
                        boolean.class,
                        java.util.function.BiFunction.class),
                "Le stage des details muraux doit exposer son constructeur injectable.");
    }

    @Test
    @DisplayName("Le stage expose toujours writeColumnSample")
    void testWriteColumnSampleExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroWallPropWriteStage.class.getDeclaredMethod(
                        "writeColumnSample",
                        net.minecraft.block.BlockState[].class,
                        com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroResolvedColumn.class),
                "Le stage des details muraux doit exposer writeColumnSample.");
    }
}
