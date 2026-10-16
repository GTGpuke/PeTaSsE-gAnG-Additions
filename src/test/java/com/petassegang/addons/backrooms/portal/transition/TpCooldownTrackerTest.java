package com.petassegang.addons.backrooms.portal.transition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Cooldown de teleportation")
class TpCooldownTrackerTest {

    @Test
    @DisplayName("Un joueur fraichement teleporte est en cooldown pendant la duree demandee")
    void cooldownWindow() {
        UUID player = UUID.randomUUID();
        assertFalse(TpCooldownTracker.isOnCooldown(player, 100L, 20), "Jamais teleporte : pas de cooldown.");
        TpCooldownTracker.markTeleported(player, 100L);
        assertTrue(TpCooldownTracker.isOnCooldown(player, 100L, 20), "Au tick de teleportation : cooldown.");
        assertTrue(TpCooldownTracker.isOnCooldown(player, 119L, 20), "Juste avant la fin : cooldown.");
        assertFalse(TpCooldownTracker.isOnCooldown(player, 120L, 20), "A la fin du cooldown : libre.");
    }

    @Test
    @DisplayName("Oublier un joueur ou tout vider leve le cooldown")
    void clearing() {
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        TpCooldownTracker.markTeleported(player, 10L);
        TpCooldownTracker.markTeleported(other, 10L);
        TpCooldownTracker.clear(player);
        assertFalse(TpCooldownTracker.isOnCooldown(player, 11L, 20), "Le joueur oublie est libre.");
        assertTrue(TpCooldownTracker.isOnCooldown(other, 11L, 20), "L'autre joueur reste en cooldown.");
        TpCooldownTracker.clearAll();
        assertFalse(TpCooldownTracker.isOnCooldown(other, 11L, 20), "Tout le monde est libre apres clearAll.");
    }
}
