package com.gtgpuke.gtgsbackrooms.core.portal.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Retour d'un portail")
class PortalWayBackTest {

    @Test
    @DisplayName("Chaque combinaison des reglages donne un retour, et chaque retour redonne ses reglages")
    void settingsAndWayBackMatch() {
        assertEquals(PortalWayBack.ROUND_TRIP, PortalWayBack.of(PortalSettings.DEFAULT), "Par defaut : aller-retour.");
        assertEquals(PortalWayBack.ONE_WAY, PortalWayBack.of(new PortalSettings(1, 2, true, true, true, false)), "Aller simple.");
        assertEquals(PortalWayBack.FLEETING_EXIT, PortalWayBack.of(new PortalSettings(1, 2, true, true, false, true)), "Sortie fugace.");
        assertEquals(PortalWayBack.ONE_WAY, PortalWayBack.of(new PortalSettings(1, 2, true, true, true, true)),
                "Aller simple et fugace : sans sortie, c'est un aller simple.");
        assertFalse(PortalWayBack.ROUND_TRIP.oneWay() || PortalWayBack.ROUND_TRIP.fleetingExit(), "Aller-retour : ni l'un ni l'autre.");
        assertTrue(PortalWayBack.ONE_WAY.oneWay() && !PortalWayBack.ONE_WAY.fleetingExit(), "Aller simple : sans sortie fugace.");
        assertTrue(PortalWayBack.FLEETING_EXIT.fleetingExit() && !PortalWayBack.FLEETING_EXIT.oneWay(), "Sortie fugace : avec retour.");
        for (PortalWayBack way : PortalWayBack.values()) {
            PortalSettings settings = new PortalSettings(1, 2, true, true, way.oneWay(), way.fleetingExit());
            assertEquals(way, PortalWayBack.of(settings), way + " se retrouve dans ses reglages.");
        }
    }

    @Test
    @DisplayName("Chaque retour a sa cle de langue, celle des reglages")
    void keys() {
        Set<String> keys = new HashSet<>();
        for (PortalWayBack way : PortalWayBack.values()) {
            keys.add(way.key());
        }
        assertEquals(Set.of("round_trip", "one_way", "fleeting_exit"), keys, "Trois cles distinctes.");
    }
}
