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
}
