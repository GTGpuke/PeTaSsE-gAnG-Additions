package com.petassegang.addons.backrooms.portal.client.world;

import it.unimi.dsi.fastutil.ints.IntList;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.petassegang.addons.backrooms.portal.sync.RemoteBlockBreakingPayload;
import com.petassegang.addons.backrooms.portal.sync.RemoteBlockUpdatePayload;
import com.petassegang.addons.backrooms.portal.sync.RemoteChunkPayload;
import com.petassegang.addons.backrooms.portal.sync.RemoteChunkUnloadPayload;
import com.petassegang.addons.backrooms.portal.sync.RemoteDimensionPayload;
import com.petassegang.addons.backrooms.portal.sync.RemoteEntityPacketPayload;
import com.petassegang.addons.config.ModConfig;

/**
 * Recoit les payloads de synchronisation des portails (chunks, blocs, entities) et les applique aux mondes distants.
 */
public final class RemoteChunkReceiver {

    private RemoteChunkReceiver() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Enregistre les recepteurs ; a appeler une fois depuis le point d'entree client. Fabric les appelle deja sur le
     * thread de rendu, dans l'ordre des paquets : les differer avec {@code client.execute} les ferait passer apres les
     * paquets vanilla deja en file (un chunk du nouveau monde applique a l'ancien, par exemple).
     */
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(RemoteDimensionPayload.ID,
                (payload, context) -> onDimension(context.client(), payload));
        ClientPlayNetworking.registerGlobalReceiver(RemoteChunkPayload.ID,
                (payload, context) -> onChunk(context.client(), payload));
        ClientPlayNetworking.registerGlobalReceiver(RemoteChunkUnloadPayload.ID,
                (payload, context) -> onChunkUnload(context.client(), payload));
        ClientPlayNetworking.registerGlobalReceiver(RemoteBlockUpdatePayload.ID,
                (payload, context) -> onBlockUpdate(context.client(), payload));
        ClientPlayNetworking.registerGlobalReceiver(RemoteEntityPacketPayload.ID,
                (payload, context) -> onEntityPacket(context.client(), payload));
        ClientPlayNetworking.registerGlobalReceiver(RemoteBlockBreakingPayload.ID,
                (payload, context) -> onBlockBreaking(context.client(), payload));
    }

    private static void onDimension(MinecraftClient client, RemoteDimensionPayload payload) {
        if (!ModConfig.ENABLE_PORTAL_FEATURE) {
            return;
        }
        RemoteWorldRegistry.syncWithMainWorld(client.world);
        RemoteWorldRegistry.announce(worldKey(payload.dimension()), payload.dimensionType());
    }

    private static void onChunk(MinecraftClient client, RemoteChunkPayload payload) {
        RemoteWorld remote = remoteWorld(client, payload.dimension());
        if (remote != null) {
            remote.loadChunk(payload.chunkX(), payload.chunkZ(), payload.chunkData(), payload.lightData());
        }
    }

    private static void onChunkUnload(MinecraftClient client, RemoteChunkUnloadPayload payload) {
        RemoteWorld remote = remoteWorld(client, payload.dimension());
        if (remote != null) {
            remote.unloadChunk(payload.chunkPos());
        }
    }

    private static void onBlockUpdate(MinecraftClient client, RemoteBlockUpdatePayload payload) {
        RemoteWorld remote = remoteWorld(client, payload.dimension());
        if (remote != null) {
            remote.updateBlock(payload.pos(), payload.state());
        }
    }

    private static void onEntityPacket(MinecraftClient client, RemoteEntityPacketPayload payload) {
        if (client.world != null && client.world.getRegistryKey().equals(worldKey(payload.dimension()))) {
            // La dimension est devenue celle du joueur (traversee) : seules les destructions comptent, elles retirent
            // les copies distantes que vanilla ne connait pas ; le reste du monde est a vanilla.
            if (ModConfig.ENABLE_PORTAL_FEATURE && payload.packet() instanceof EntitiesDestroyS2CPacket destroy) {
                IntList ids = destroy.getEntityIds();
                for (int i = 0; i < ids.size(); i++) {
                    client.world.removeEntity(ids.getInt(i), Entity.RemovalReason.DISCARDED);
                }
            }
            return;
        }
        RemoteWorld remote = remoteWorld(client, payload.dimension());
        if (remote != null) {
            remote.applyEntityPacket(client, payload.packet());
        }
    }

    /** Fissures d'un bloc mine a travers un portail, sur la vue qui le montre (monde distant ou vue secondaire). */
    private static void onBlockBreaking(MinecraftClient client, RemoteBlockBreakingPayload payload) {
        if (!ModConfig.ENABLE_PORTAL_FEATURE || client.player == null) {
            return;
        }
        RemoteWorldRegistry.syncWithMainWorld(client.world);
        RemoteWorld view = RemoteWorldRegistry.viewOf(worldKey(payload.dimension()));
        if (view != null) {
            view.renderer().setBlockBreakingInfo(client.player.getId(), payload.pos(), payload.stage());
        }
    }

    private static RemoteWorld remoteWorld(MinecraftClient client, Identifier dimension) {
        if (!ModConfig.ENABLE_PORTAL_FEATURE) {
            return null;
        }
        RemoteWorldRegistry.syncWithMainWorld(client.world);
        return RemoteWorldRegistry.get(worldKey(dimension));
    }

    private static RegistryKey<World> worldKey(Identifier dimension) {
        return RegistryKey.of(RegistryKeys.WORLD, dimension);
    }
}
