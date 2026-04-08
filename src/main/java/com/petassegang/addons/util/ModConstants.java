package com.petassegang.addons.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constantes globales pour PeTaSsE_gAnG_Additions.
 *
 * <p>Tous les identifiants du mod et le logger partagé sont définis ici.
 * Importer cette classe dans chaque classe qui a besoin de MOD_ID ou LOGGER.
 */
public final class ModConstants {

    /** Identifiant unique du mod, doit correspondre à la valeur dans {@code mods.toml}. */
    public static final String MOD_ID   = "petasse_gang_additions";

    /** Nom d'affichage du mod, utilisé dans les messages de log. */
    public static final String MOD_NAME = "PeTaSsE_gAnG_Additions";

    /** Logger SLF4J partagé — à utiliser partout à la place de nouveaux loggers. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private ModConstants() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }
}
