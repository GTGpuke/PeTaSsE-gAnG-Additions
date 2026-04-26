package com.petassegang.addons.init;

import com.petassegang.addons.util.ModConstants;

/**
 * Registre des block entities du mod.
 *
 * <p>Le papier peint adaptatif du Level 0 n'utilise plus de block entity :
 * le masque de faces est encode dans la propriete {@code face_mask} du block state.
 */
public final class ModBlockEntities {

    /**
     * Force le chargement de la classe.
     */
    public static void register() {
        ModConstants.LOGGER.debug("Block entities du mod enregistrees.");
    }

    private ModBlockEntities() {
        throw new UnsupportedOperationException("Classe de registre.");
    }
}
