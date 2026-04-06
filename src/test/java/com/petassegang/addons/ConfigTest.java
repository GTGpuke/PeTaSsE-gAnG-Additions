package com.petassegang.addons;

import com.petassegang.addons.config.ModConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vérifie que les objets ForgeConfigSpec sont construits correctement et que
 * les valeurs par défaut correspondent à ce que le code annonce.
 *
 * <p>Ces tests s'exécutent sans bootstrap Forge — ForgeConfigSpec est pur Java
 * et ne nécessite pas une instance Minecraft en cours d'exécution.
 */
@DisplayName("Intégrité des specs ModConfig")
class ConfigTest {

    @Test
    @DisplayName("SERVER_SPEC n'est pas null")
    void testServerSpecNotNull() {
        assertNotNull(ModConfig.SERVER_SPEC,
                "SERVER_SPEC doit être construit dans l'initialiseur statique.");
    }

    @Test
    @DisplayName("CLIENT_SPEC n'est pas null")
    void testClientSpecNotNull() {
        assertNotNull(ModConfig.CLIENT_SPEC,
                "CLIENT_SPEC doit être construit dans l'initialiseur statique.");
    }

    @Test
    @DisplayName("ENABLE_GANG_BADGE existe et vaut true par défaut")
    void testEnableGangBadgeDefault() {
        assertNotNull(ModConfig.ENABLE_GANG_BADGE,
                "La valeur de config ENABLE_GANG_BADGE ne doit pas être null.");

        // ForgeConfigSpec.BooleanValue.getDefault() est disponible avant le chargement du fichier par Forge.
        assertTrue((boolean) ModConfig.ENABLE_GANG_BADGE.getDefault(),
                "enableGangBadge doit valoir true par défaut.");
    }

    @Test
    @DisplayName("SERVER_SPEC définit correctement la catégorie 'items'")
    void testServerSpecHasItemsCategory() {
        // Vérifie que le chemin de config est 'items.enableGangBadge'.
        String path = String.join(".", ModConfig.ENABLE_GANG_BADGE.getPath());
        assertEquals("items.enableGangBadge", path,
                "Le chemin de la clé de config doit être 'items.enableGangBadge'.");
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
