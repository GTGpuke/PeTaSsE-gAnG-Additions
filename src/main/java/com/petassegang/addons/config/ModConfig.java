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
    public static final boolean ENABLE_GANG_BADGE = booleanProperty(
            "petassegang.enableGangBadge",
            true);
    /** Active ou desactive la micro-geometrie du Level 0. */
    public static final boolean ENABLE_LEVEL_ZERO_NOISE_GEOMETRY = booleanProperty(
            "petassegang.levelZero.noiseGeometry",
            true);
    /** Active un rendu debug des micro-features du Level 0. */
    public static final boolean DEBUG_LEVEL_ZERO_MICRO_GEOMETRY = booleanProperty(
            "petassegang.levelZero.debugMicroGeometry",
            false);

    private ModConfig() {
        throw new UnsupportedOperationException("Classe de configuration.");
    }

    private static boolean booleanProperty(String key, boolean defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }
}
