package com.petassegang.addons;

import com.petassegang.addons.util.ModConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Vérifie l'intégrité de base des constantes du point d'entrée du mod.
 * Tests purement JVM — aucun bootstrap Minecraft/Forge requis.
 */
@DisplayName("Constantes de chargement du mod")
class ModLoadTest {

    @Test
    @DisplayName("MOD_ID est égal à 'petasse_gang_additions'")
    void testModId() {
        assertEquals("petasse_gang_additions", ModConstants.MOD_ID,
                "MOD_ID doit correspondre à la valeur déclarée dans mods.toml.");
    }

    @Test
    @DisplayName("MOD_NAME n'est pas vide")
    void testModName() {
        assertFalse(ModConstants.MOD_NAME.isBlank(),
                "MOD_NAME ne doit pas être vide.");
    }

    @Test
    @DisplayName("Le logger partagé est initialisé")
    void testLogger() {
        assertNotNull(ModConstants.LOGGER,
                "LOGGER ne doit pas être null.");
    }

    @Test
    @DisplayName("Le constructeur de ModConstants lève UnsupportedOperationException")
    void testUtilityClassPrivateConstructor() {
        assertThrows(UnsupportedOperationException.class, () -> {
            var ctor = ModConstants.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            try {
                ctor.newInstance();
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }, "Le constructeur privé de ModConstants doit lever UnsupportedOperationException.");
    }
}
