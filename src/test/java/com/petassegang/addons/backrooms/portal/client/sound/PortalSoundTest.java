package com.petassegang.addons.backrooms.portal.client.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Son a travers un portail")
class PortalSoundTest {

    private static final double EPSILON = 1.0e-6;

    @Test
    @DisplayName("La portee d'un son suit son volume, entre seize et soixante-quatre blocs")
    void radiusFollowsVolume() {
        assertEquals(PortalSound.MIN_RADIUS, PortalSound.radius(0.2f), EPSILON, "Un son faible porte quand meme seize blocs.");
        assertEquals(32.0, PortalSound.radius(2.0f), EPSILON, "Seize blocs par unite de volume.");
        assertEquals(PortalSound.MAX_RADIUS, PortalSound.radius(10000.0f), EPSILON, "Un tonnerre ne porte pas au-dela de soixante-quatre blocs.");
    }

    @Test
    @DisplayName("Le trajet jusqu'au cadre consomme un seizieme de volume par bloc, sans descendre sous zero")
    void travelToTheFrameCostsVolume() {
        assertEquals(1.0f, PortalSound.volumeThroughPortal(1.0f, 0.0), 1.0e-6f, "Un son colle au cadre passe entier.");
        assertEquals(0.5f, PortalSound.volumeThroughPortal(1.0f, 8.0), 1.0e-6f, "A huit blocs, la moitie.");
        assertEquals(0.0f, PortalSound.volumeThroughPortal(1.0f, 40.0), 1.0e-6f, "Trop loin : le son s'est eteint en chemin.");
    }
}
