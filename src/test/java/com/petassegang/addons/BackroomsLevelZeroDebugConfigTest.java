package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.config.ModConfig;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifie les options de debug du Level 0.
 */
@DisplayName("Config debug du Level 0")
class BackroomsLevelZeroDebugConfigTest {

    @Test
    @DisplayName("La micro-geometrie reste active par defaut")
    void testNoiseGeometryEnabledByDefault() {
        assertTrue(ModConfig.ENABLE_LEVEL_ZERO_NOISE_GEOMETRY,
                "La micro-geometrie doit rester active par defaut dans le pipeline.");
    }

    @Test
    @DisplayName("Le debug visuel des micro-features reste desactive par defaut")
    void testMicroGeometryDebugDisabledByDefault() {
        assertFalse(ModConfig.DEBUG_LEVEL_ZERO_MICRO_GEOMETRY,
                "Le debug visuel doit rester desactive par defaut pour ne pas polluer le rendu normal.");
    }

    @Test
    @DisplayName("Le debug visuel des petits details muraux reste desactive par defaut")
    void testWallPropsDebugDisabledByDefault() {
        assertFalse(ModConfig.DEBUG_LEVEL_ZERO_WALL_PROPS,
                "Le debug des petits details muraux doit rester desactive par defaut.");
    }

    @Test
    @DisplayName("Le debug visuel des structures rares reste desactive par defaut")
    void testStructuresDebugDisabledByDefault() {
        assertFalse(ModConfig.DEBUG_LEVEL_ZERO_STRUCTURES,
                "Le debug des structures rares doit rester desactive par defaut.");
    }
}
