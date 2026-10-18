package com.gtgpuke.gtgsbackrooms.backrooms.portal.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.math.ChunkPos;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.feature.gang.GangBadgeActivatePayload;

@DisplayName("Envoi des payloads des portails")
class PortalNetworkTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("La taille mesuree d'un payload est celle de son encodage ; un payload etranger aux portails ne compte pas")
    void sizeOfEncodesWithTheRegisteredCodec() {
        RemoteChunkUnloadPayload unload = new RemoteChunkUnloadPayload(1, new ChunkPos(3, -7));
        RegistryByteBuf reference = new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
        RemoteChunkUnloadPayload.CODEC.encode(reference, unload);
        // Un octet pour l'index de la dimension, puis la position du chunk en entier long variable, dix octets pour un Z negatif.
        assertEquals(1 + 10, reference.readableBytes(), "Encodage de reference.");
        assertEquals(reference.readableBytes(), PortalNetwork.sizeOf(DynamicRegistryManager.EMPTY, unload),
                "Taille encodee du dechargement d'un chunk.");
        reference.release();
        assertEquals(0, PortalNetwork.sizeOf(DynamicRegistryManager.EMPTY, GangBadgeActivatePayload.INSTANCE),
                "Un payload dont le codec n'est pas garde ici mesure zero.");
    }
}
