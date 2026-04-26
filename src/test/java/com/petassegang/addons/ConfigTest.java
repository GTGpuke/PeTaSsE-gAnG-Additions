package com.petassegang.addons;

import com.petassegang.addons.config.ModConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vérifie que les constantes de ModConfig ont les valeurs par défaut attendues.
 *
 * <p>En Fabric, la configuration n'utilise pas ForgeConfigSpec — les valeurs par défaut
 * sont des constantes statiques. Ces tests s'exécutent sans bootstrap Minecraft.
 */
@DisplayName("Intégrité des constantes ModConfig")
class ConfigTest {

    @Test
    @DisplayName("ENABLE_GANG_BADGE vaut true par défaut")
    void testEnableGangBadgeDefault() {
        assertTrue(ModConfig.ENABLE_GANG_BADGE,
                "enableGangBadge doit valoir true par defaut.");
    }

    @Test
    @DisplayName("ENABLE_LEVEL_ZERO_NOISE_GEOMETRY vaut true par defaut")
    void testEnableLevelZeroNoiseGeometryDefault() {
        assertTrue(ModConfig.ENABLE_LEVEL_ZERO_NOISE_GEOMETRY,
                "La micro-geometrie du Level 0 doit rester active par defaut.");
    }

    @Test
    @DisplayName("Le constructeur de ModConfig lève UnsupportedOperationException")
    void testUtilityClassConstructors() {
        assertThrows(UnsupportedOperationException.class, () -> {
            var ctor = ModConfig.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            try {
                ctor.newInstance();
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }, "Le constructeur privé de ModConfig doit lever UnsupportedOperationException.");
    }
}
