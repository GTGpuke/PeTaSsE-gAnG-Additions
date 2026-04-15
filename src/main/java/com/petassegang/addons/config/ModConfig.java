package com.petassegang.addons.config;

/**
 * Configuration simplifiee du mod pour Fabric.
 *
 * <p>La configuration Forge (ForgeConfigSpec) n'existe pas dans Fabric.
 * Les options sont en constantes statiques avec leur valeur par defaut.
 * Une implementation complete via cloth-config peut etre ajoutee ulterieurement.
 */
public final class ModConfig {

    /** Active ou desactive le Gang Badge. Valeur par defaut : true. */
    public static final boolean ENABLE_GANG_BADGE = true;

    private ModConfig() {
        throw new UnsupportedOperationException("Classe de configuration.");
    }
}
