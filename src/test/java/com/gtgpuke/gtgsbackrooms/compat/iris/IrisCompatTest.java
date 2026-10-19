package com.gtgpuke.gtgsbackrooms.compat.iris;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Compatibilite Iris, sans Iris")
class IrisCompatTest {

    @Test
    @DisplayName("Mod absent : rien de charge, aucun pack actif meme apres releve, et aucune classe d'Iris touchee")
    void absentMod() {
        assertFalse(IrisCompat.isLoaded(), "Iris n'est pas dans l'environnement de test.");
        IrisCompat.refresh();
        assertFalse(IrisCompat.shaderPackInUse(), "Sans Iris, aucun pack de shaders n'est actif.");
    }
}
