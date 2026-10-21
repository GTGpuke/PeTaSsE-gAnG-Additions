package com.gtgpuke.gtgsbackrooms.core.portal.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/**
 * Liste serveur des portails charges, tenue a jour par les evenements de chargement d'entity.
 *
 * <p>Evite de parcourir toutes les entities de tous les mondes a chaque tick : la teleportation,
 * le prechargement et la synchronisation ne lisent que cette liste, iteree par index sans allocation. Le registre
 * retire aussi, de temps en temps, les portails dont le bloc portail a disparu.
 */
public final class BackroomsPortalEntityRegistry {

    /** Intervalle de verification des blocs proprietaires. */
    private static final int OWNER_CHECK_INTERVAL_TICKS = 100;

    private static final List<BackroomsPortalEntity> PORTALS = new ArrayList<>();
    private static boolean initialized;

    private BackroomsPortalEntityRegistry() {
        throw new UnsupportedOperationException("Classe de registre.");
    }

    /** Branche les evenements de chargement ; idempotent. */
    public static void init() {
        if (initialized) {
            return;
        }
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity instanceof BackroomsPortalEntity portal && !PORTALS.contains(portal)) {
                PORTALS.add(portal);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof BackroomsPortalEntity portal) {
                PORTALS.remove(portal);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(BackroomsPortalEntityRegistry::discardOrphans);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PORTALS.clear());
        initialized = true;
    }

    /** Toutes les cinq secondes, les portails dont le bloc proprietaire a disparu sont retires. */
    private static void discardOrphans(MinecraftServer server) {
        if (server.getTicks() % OWNER_CHECK_INTERVAL_TICKS != 0) {
            return;
        }
        // A rebours : un portail retire quitte la liste, les suivants changent d'indice.
        for (int i = PORTALS.size() - 1; i >= 0; i--) {
            BackroomsPortalEntity portal = PORTALS.get(i);
            if (portal.isOrphan(server)) {
                portal.discard();
            }
        }
    }

    /** Portails actuellement charges ; liste vivante, a lire par index et a ne pas modifier. */
    public static List<BackroomsPortalEntity> all() {
        return PORTALS;
    }

    /** Portail charge d'identifiant donne dans ce monde, ou {@code null}. */
    public static @Nullable BackroomsPortalEntity find(World world, UUID uuid) {
        for (int i = 0; i < PORTALS.size(); i++) {
            BackroomsPortalEntity portal = PORTALS.get(i);
            if (portal.getWorld() == world && portal.getUuid().equals(uuid)) {
                return portal;
            }
        }
        return null;
    }
}
