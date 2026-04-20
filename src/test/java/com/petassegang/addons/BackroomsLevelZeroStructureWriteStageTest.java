package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.debug.LevelZeroStructureWriteStage;
import com.petassegang.addons.world.backrooms.level0.write.structure.LevelZeroStructureProfile;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifie le contrat declaratif du stage debug des structures rares.
 */
@DisplayName("Structure write stage du Level 0")
class BackroomsLevelZeroStructureWriteStageTest {

    @Test
    @DisplayName("Le stage expose toujours son constructeur par defaut")
    void testDefaultConstructorExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroStructureWriteStage.class.getDeclaredConstructor(),
                "Le stage debug des structures doit exposer son constructeur par defaut.");
    }

    @Test
    @DisplayName("Le stage expose toujours son constructeur avec toggle")
    void testBooleanConstructorExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroStructureWriteStage.class.getDeclaredConstructor(boolean.class),
                "Le stage debug des structures doit exposer son constructeur avec toggle.");
    }

    @Test
    @DisplayName("Le stage expose toujours son constructeur injectable pour les tests")
    void testInjectableConstructorExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroStructureWriteStage.class.getDeclaredConstructor(
                        boolean.class,
                        java.util.function.Function.class),
                "Le stage debug des structures doit exposer son constructeur injectable.");
    }

    @Test
    @DisplayName("Le stage expose toujours writeColumnSample")
    void testWriteColumnSampleExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroStructureWriteStage.class.getDeclaredMethod(
                        "writeColumnSample",
                        net.minecraft.block.BlockState[].class,
                        com.petassegang.addons.world.backrooms.level0.write.LevelZeroResolvedColumn.class),
                "Le stage debug des structures doit exposer writeColumnSample.");
    }
}
