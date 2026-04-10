package com.petassegang.addons;

import com.petassegang.addons.creative.ModCreativeTab;
import com.petassegang.addons.init.ModItems;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Vérifie que tous les DeferredRegister et RegistryObject sont non-null
 * au chargement de la classe (avant que Forge ait déclenché les événements de registre).
 *
 * <p>Modèle : pour chaque nouvel objet de registre, ajouter un {@code assertNotNull} ici.
 */
@DisplayName("Objets de registre avant enregistrement")
class RegistryTest {

    @Test
    @DisplayName("ModItems.ITEMS DeferredRegister n'est pas null")
    void testItemsDeferredRegisterNotNull() {
        assertNotNull(ModItems.ITEMS,
                "Le DeferredRegister ITEMS doit être créé de façon eagerly.");
    }

    @Test
    @DisplayName("GANG_BADGE RegistryObject n'est pas null")
    void testGangBadgeRegistryObjectNotNull() {
        assertNotNull(ModItems.GANG_BADGE,
                "Le RegistryObject GANG_BADGE doit être non-null avant le déclenchement de l'enregistrement.");
    }

    @Test
    @DisplayName("ModCreativeTab.CREATIVE_MODE_TABS DeferredRegister n'est pas null")
    void testCreativeTabDeferredRegisterNotNull() {
        assertNotNull(ModCreativeTab.CREATIVE_MODE_TABS,
                "Le DeferredRegister CREATIVE_MODE_TABS doit être créé de façon eagerly.");
    }

    @Test
    @DisplayName("PETASSEGANG_TAB RegistryObject n'est pas null")
    void testPetasseGangTabRegistryObjectNotNull() {
        assertNotNull(ModCreativeTab.PETASSEGANG_TAB,
                "Le RegistryObject PETASSEGANG_TAB doit être non-null avant le déclenchement de l'enregistrement.");
    }
}
