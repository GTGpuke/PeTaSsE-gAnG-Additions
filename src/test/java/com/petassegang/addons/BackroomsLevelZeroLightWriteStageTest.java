package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroBlockPalette;
import com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroLightWriteStage;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifie le contrat declaratif du stage d'ecriture des neons.
 */
@DisplayName("Light write stage du Level 0")
class BackroomsLevelZeroLightWriteStageTest {

    @Test
    @DisplayName("Le stage expose toujours un constructeur avec palette")
    void testConstructorExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroLightWriteStage.class.getDeclaredConstructor(LevelZeroBlockPalette.class),
                "Le stage des neons doit accepter une palette injectable.");
    }

    @Test
    @DisplayName("Le stage expose toujours writeColumnSample")
    void testWriteColumnSampleExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroLightWriteStage.class.getDeclaredMethod(
                        "writeColumnSample",
                        net.minecraft.block.BlockState[].class,
                        com.petassegang.addons.backrooms.level.level0.generation.write.LevelZeroResolvedColumn.class),
                "Le stage des neons doit exposer writeColumnSample.");
    }
}
