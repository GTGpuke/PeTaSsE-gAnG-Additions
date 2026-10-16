package com.petassegang.addons.backrooms.portal.transition;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cooldown anti double-teleportation par joueur.
 */
public final class TpCooldownTracker {

    private static final ConcurrentMap<UUID, Long> LAST_TP_TICK = new ConcurrentHashMap<>();

    private TpCooldownTracker() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    public static void markTeleported(UUID playerUuid, long currentTick) {
        LAST_TP_TICK.put(playerUuid, currentTick);
    }

    public static boolean isOnCooldown(UUID playerUuid, long currentTick, int cooldownTicks) {
        Long lastTick = LAST_TP_TICK.get(playerUuid);
        return lastTick != null && currentTick - lastTick < cooldownTicks;
    }

    public static void clear(UUID playerUuid) {
        LAST_TP_TICK.remove(playerUuid);
    }

    public static void clearAll() {
        LAST_TP_TICK.clear();
    }
}
