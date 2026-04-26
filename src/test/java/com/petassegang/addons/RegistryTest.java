package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.creative.ModCreativeTab;
import com.petassegang.addons.init.ModItems;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifie que les points d'entree declaratifs des registres existent.
 */
@DisplayName("Objets de registre avant enregistrement")
class RegistryTest {

    @Test
    @DisplayName("GANG_BADGE existe dans ModItems")
    void testGangBadgeFieldExists() throws NoSuchFieldException {
        assertNotNull(ModItems.class.getDeclaredField("GANG_BADGE"),
                "Le champ GANG_BADGE doit exister dans ModItems.");
    }

    @Test
    @DisplayName("PETASSEGANG_TAB existe dans ModCreativeTab")
    void testPetasseGangTabFieldExists() throws NoSuchFieldException {
        assertNotNull(ModCreativeTab.class.getDeclaredField("PETASSEGANG_TAB"),
                "Le champ PETASSEGANG_TAB doit exister dans ModCreativeTab.");
    }
}
