package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.write.LevelZeroBlockPalette;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroColumnMaterial;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroResolvedColumnResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifie le contrat declaratif des materiaux de colonne du Level 0.
 */
@DisplayName("Materiaux de colonne du Level 0")
class BackroomsLevelZeroColumnMaterialTest {

    @Test
    @DisplayName("LevelZeroColumnMaterial expose bien ses quatre composantes")
    void testColumnMaterialRecordShape() {
        assertEquals(4, LevelZeroColumnMaterial.class.getRecordComponents().length,
                "LevelZeroColumnMaterial doit rester un record minimal a quatre composantes.");
        assertEquals("walkable", LevelZeroColumnMaterial.class.getRecordComponents()[0].getName(),
                "La premiere composante doit rester walkable.");
        assertEquals("floor", LevelZeroColumnMaterial.class.getRecordComponents()[1].getName(),
                "La deuxieme composante doit rester floor.");
        assertEquals("interior", LevelZeroColumnMaterial.class.getRecordComponents()[2].getName(),
                "La troisieme composante doit rester interior.");
        assertEquals("ceiling", LevelZeroColumnMaterial.class.getRecordComponents()[3].getName(),
                "La quatrieme composante doit rester ceiling.");
    }

    @Test
    @DisplayName("Le resolveur de colonnes accepte toujours une palette injectable")
    void testResolvedColumnResolverConstructorExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroResolvedColumnResolver.class.getDeclaredConstructor(LevelZeroBlockPalette.class),
                "Le resolveur de colonnes doit accepter une palette injectable.");
    }

    @Test
    @DisplayName("La palette expose bien son constructeur de test avec air injectable")
    void testBlockPaletteInjectableConstructorExists() throws NoSuchMethodException {
        assertNotNull(LevelZeroBlockPalette.class.getDeclaredConstructor(
                        net.minecraft.block.BlockState.class,
                        net.minecraft.block.BlockState.class,
                        net.minecraft.block.BlockState.class,
                        net.minecraft.block.BlockState.class,
                        net.minecraft.block.BlockState.class,
                        net.minecraft.block.BlockState.class,
                        net.minecraft.block.BlockState.class,
                        net.minecraft.block.BlockState.class),
                "La palette doit exposer un constructeur de test avec air injectable.");
    }
}
