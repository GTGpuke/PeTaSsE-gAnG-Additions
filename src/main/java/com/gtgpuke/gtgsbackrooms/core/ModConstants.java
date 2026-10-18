package com.gtgpuke.gtgsbackrooms.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constantes globales pour GTG's Backrooms.
 *
 * <p>Tous les identifiants du mod et le logger partage sont definis ici.
 * Importer cette classe dans chaque classe qui a besoin de MOD_ID ou LOGGER.
 */
public final class ModConstants {

    /** Identifiant unique du mod, doit correspondre a la valeur dans {@code fabric.mod.json}. */
    public static final String MOD_ID   = "gtgs_backrooms";

    /** Nom d'affichage du mod, utilise dans les messages de log. */
    public static final String MOD_NAME = "GTG's Backrooms";

    /** Logger SLF4J partage, a utiliser partout a la place de nouveaux loggers. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private ModConstants() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }
}
