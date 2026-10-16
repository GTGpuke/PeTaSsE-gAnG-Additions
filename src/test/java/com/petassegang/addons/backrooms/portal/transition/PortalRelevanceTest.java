package com.petassegang.addons.backrooms.portal.transition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.Vec3d;

@DisplayName("Pertinence d'un portail pour un observateur")
class PortalRelevanceTest {

    private static final Vec3d EYE = new Vec3d(0.0, 65.0, 0.0);
    private static final Vec3d LOOK_NORTH = new Vec3d(0.0, 0.0, -1.0);

    @Test
    @DisplayName("Un portail devant l'observateur et a portee compte")
    void inFrontAndInRange() {
        assertTrue(PortalRelevance.isRelevant(EYE, LOOK_NORTH, new Vec3d(0.0, 65.0, -20.0), 64.0), "Devant, a 20 blocs.");
        assertTrue(PortalRelevance.isRelevant(EYE, LOOK_NORTH, new Vec3d(30.0, 65.0, -1.0), 64.0), "A peine devant, sur le cote.");
    }

    @Test
    @DisplayName("Un portail dans le dos ne compte pas, sauf s'il est tout proche")
    void behindIsIgnoredUnlessVeryClose() {
        assertFalse(PortalRelevance.isRelevant(EYE, LOOK_NORTH, new Vec3d(0.0, 65.0, 20.0), 64.0), "Derriere, a 20 blocs.");
        assertTrue(PortalRelevance.isRelevant(EYE, LOOK_NORTH, new Vec3d(0.0, 65.0, 3.0), 64.0), "Derriere mais a 3 blocs.");
        assertFalse(PortalRelevance.isRelevant(EYE, LOOK_NORTH, new Vec3d(0.0, 65.0, 4.5), 64.0),
                "Derriere, juste au-dela de la distance de securite.");
    }

    @Test
    @DisplayName("Au-dela de la distance maximale, rien ne compte")
    void outOfRangeIsIgnored() {
        assertFalse(PortalRelevance.isRelevant(EYE, LOOK_NORTH, new Vec3d(0.0, 65.0, -65.0), 64.0), "Devant mais trop loin.");
        assertTrue(PortalRelevance.isRelevant(EYE, LOOK_NORTH, new Vec3d(0.0, 65.0, -64.0), 64.0), "Exactement a la limite.");
    }
}
