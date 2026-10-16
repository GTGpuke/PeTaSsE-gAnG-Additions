package com.petassegang.addons.backrooms.portal.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.World;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/**
 * Liste serveur des portails charges, tenue a jour par les evenements de chargement d'entity.
 *
 * <p>Evite de parcourir toutes les entities de tous les mondes a chaque tick : la teleportation,
 * le prechargement et la synchronisation ne lisent que cette liste, iteree par index sans allocation.
 */
public final class BackroomsPortalEntityRegistry {

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
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PORTALS.clear());
        initialized = true;
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
