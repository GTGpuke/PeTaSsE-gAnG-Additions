package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.write.LevelZeroBlockPalette;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroFoundationWriteStage;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifie le contrat declaratif du stage d'ecriture des fondations.
 */
@DisplayName("Foundation write stage du Level 0")
class BackroomsLevelZeroFoundationWriteStageTest {

    @Test
    @DisplayName("Le stage expose toujours un constructeur avec palette")
    void testConstructorExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroFoundationWriteStage.class.getDeclaredConstructor(LevelZeroBlockPalette.class),
                "Le stage des fondations doit accepter une palette injectable.");
    }

    @Test
    @DisplayName("Le stage expose toujours initializeColumnSample")
    void testInitializeColumnSampleExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroFoundationWriteStage.class.getDeclaredMethod(
                        "initializeColumnSample",
                        net.minecraft.block.BlockState[].class),
                "Le stage des fondations doit exposer initializeColumnSample.");
    }

    @Test
    @DisplayName("Le stage expose toujours writeColumnSample")
    void testWriteColumnSampleExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroFoundationWriteStage.class.getDeclaredMethod(
                        "writeColumnSample",
                        net.minecraft.block.BlockState[].class,
                        com.petassegang.addons.world.backrooms.level0.write.LevelZeroResolvedColumn.class),
                "Le stage des fondations doit exposer writeColumnSample.");
    }
}
