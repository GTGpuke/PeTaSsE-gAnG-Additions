package com.petassegang.addons.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constantes globales pour PétasseGang Addons.
 *
 * <p>Tous les identifiants du mod et le logger partagé sont définis ici.
 * Importer cette classe dans chaque classe qui a besoin de MOD_ID ou LOGGER.
 */
public final class ModConstants {

    public static final String MOD_ID   = "petassegang_addons";
    public static final String MOD_NAME = "PétasseGang Addons";

    /** Logger SLF4J partagé — à utiliser partout à la place de nouveaux loggers. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private ModConstants() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }
}
