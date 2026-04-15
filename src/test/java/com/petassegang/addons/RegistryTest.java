package com.petassegang.addons;

import com.petassegang.addons.creative.ModCreativeTab;
import com.petassegang.addons.init.ModItems;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Vérifie que les champs de registre sont non-null au chargement de la classe.
 *
 * <p>En Fabric, les objets sont enregistrés directement via {@code Registry.register()}
 * lors de l'initialisation statique — il n'y a pas de DeferredRegister.
 *
 * <p>Modèle : pour chaque nouvel objet de registre, ajouter un {@code assertNotNull} ici.
 */
@DisplayName("Objets de registre avant enregistrement")
class RegistryTest {

    @Test
    @DisplayName("GANG_BADGE n'est pas null")
    void testGangBadgeRegistryObjectNotNull() {
        assertNotNull(ModItems.GANG_BADGE,
                "Le champ GANG_BADGE doit être non-null.");
    }

    @Test
    @DisplayName("PETASSEGANG_TAB n'est pas null")
    void testPetasseGangTabRegistryObjectNotNull() {
        assertNotNull(ModCreativeTab.PETASSEGANG_TAB,
                "Le champ PETASSEGANG_TAB doit être non-null.");
    }
}
