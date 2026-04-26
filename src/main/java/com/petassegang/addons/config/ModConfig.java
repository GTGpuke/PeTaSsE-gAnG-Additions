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
    /**
     * Met en pause toute la couche de details surfaciques du Level 0.
     *
     * <p>Les taches, l'humidite, l'usure et les overlays muraux sont
     * volontairement desactives pour se concentrer sur les plinthes,
     * interrupteurs et prises.
     */
    public static final boolean ENABLE_LEVEL_ZERO_SURFACE_DETAILS = booleanProperty(
            "petassegang.levelZero.surfaceDetails",
            false);
    /** Active un rendu debug des petits details muraux du Level 0. */
    public static final boolean DEBUG_LEVEL_ZERO_WALL_PROPS = booleanProperty(
            "petassegang.levelZero.debugWallProps",
            false);
    /**
     * Active un rendu debug des structures rares du Level 0.
     *
     * <p>Ce toggle ne spawn pas de vraies structures finales. Il affiche
     * seulement la couche semantique encore en cours de maturation.
     */
    public static final boolean DEBUG_LEVEL_ZERO_STRUCTURES = booleanProperty(
            "petassegang.levelZero.debugStructures",
            false);
    /**
     * Active un monitoring de performance opt-in.
     *
     * <p>Ce mode publie des resumes periodiques dans les logs et ajoute une
     * synthese au F3 du Level 0.
     */
    public static final boolean DEBUG_PERFORMANCE_MONITOR = booleanProperty(
            "petassegang.debugPerformanceMonitor",
            false);
    /** Intervalle entre deux snapshots de performance dans les logs. */
    public static final int PERFORMANCE_LOG_INTERVAL_SECONDS = intProperty(
            "petassegang.performanceLogIntervalSeconds",
            10);

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

    private static int intProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
