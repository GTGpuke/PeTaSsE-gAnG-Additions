package com.petassegang.addons.backrooms.portal.client.world;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.transition.PortalCollision;

/**
 * Portails charges dans chaque monde client, principal ou distant : pendant du registre serveur.
 *
 * <p>Les parcours par image (visibilite, prediction de traversee, visee a travers le cadre) lisent cette liste
 * au lieu de toutes les entities du monde. Les mondes sont compares par identite : un monde distant et le
 * monde principal de la meme dimension ont chacun leur liste, et un monde abandonne est oublie par le registre.
 * La collision a travers les portails y trouve aussi les portails du client et le monde ou chacun mene.
 */
public final class ClientPortalIndex {

    private static final Map<World, List<BackroomsPortalEntity>> PORTALS = new IdentityHashMap<>();
    private static final List<BackroomsPortalEntity> NONE = List.of();

    private ClientPortalIndex() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Branche les evenements de chargement d'entities client et la collision a travers les portails (point d'entree client). */
    public static void init() {
        ClientEntityEvents.ENTITY_LOAD.register(ClientPortalIndex::onEntityLoad);
        ClientEntityEvents.ENTITY_UNLOAD.register(ClientPortalIndex::onEntityUnload);
        // Le serveur integre tourne dans le meme processus : ses mondes gardent le registre serveur.
        PortalCollision.setWorlds(new PortalCollision.Worlds() {
            @Override
            public List<BackroomsPortalEntity> portals(World world) {
                return world.isClient() ? ClientPortalIndex.portals(world) : PortalCollision.SERVER_WORLDS.portals(world);
            }

            @Override
            public @Nullable World destination(BackroomsPortalEntity portal) {
                return portal.getWorld().isClient() ? clientDestination(portal) : PortalCollision.SERVER_WORLDS.destination(portal);
            }
        });
    }

    /** Monde client ou mene un portail : le sien, le monde principal, ou le monde distant de sa dimension s'il est charge. */
    private static @Nullable World clientDestination(BackroomsPortalEntity portal) {
        RegistryKey<World> dimension = portal.destinationDim();
        World own = portal.getWorld();
        if (dimension.equals(own.getRegistryKey())) {
            return own;
        }
        ClientWorld main = MinecraftClient.getInstance().world;
        if (main != null && dimension.equals(main.getRegistryKey())) {
            return main;
        }
        RemoteWorld remote = RemoteWorldRegistry.get(dimension);
        return remote == null ? null : remote.world();
    }

    /** Portails charges dans ce monde ; liste vivante, a lire par index et a ne pas modifier. */
    public static List<BackroomsPortalEntity> portals(World world) {
        List<BackroomsPortalEntity> portals = PORTALS.get(world);
        return portals == null ? NONE : portals;
    }

    /** Un monde est abandonne (monde distant libere, monde principal remplace par vanilla) : ses portails avec lui. */
    public static void forget(World world) {
        PORTALS.remove(world);
    }

    /** Deconnexion : plus aucun monde. */
    public static void clear() {
        PORTALS.clear();
    }

    private static void onEntityLoad(Entity entity, ClientWorld world) {
        if (entity instanceof BackroomsPortalEntity portal) {
            List<BackroomsPortalEntity> portals = PORTALS.computeIfAbsent(world, key -> new ObjectArrayList<>());
            if (!portals.contains(portal)) {
                portals.add(portal);
            }
        }
    }

    private static void onEntityUnload(Entity entity, ClientWorld world) {
        if (entity instanceof BackroomsPortalEntity portal) {
            List<BackroomsPortalEntity> portals = PORTALS.get(world);
            if (portals != null) {
                portals.remove(portal);
            }
        }
    }
}
