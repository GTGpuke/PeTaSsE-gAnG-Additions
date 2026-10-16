package com.petassegang.addons.backrooms.portal.sync;

import java.util.Map;
import java.util.WeakHashMap;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.state.PlayStateFactories;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;

import com.petassegang.addons.core.ModConstants;

/**
 * Serveur vers client : un paquet d'entity vanilla destine a une dimension distante.
 *
 * <p>Le serveur branche le joueur comme auditeur du suivi vanilla des entities presentes dans sa
 * fenetre de chunks distante. Chaque paquet produit par ce suivi (apparition, deplacement, donnees,
 * equipement, passagers, disparition) part ici, enveloppe avec sa dimension, et le client l'applique
 * a son monde distant exactement comme il l'appliquerait a son propre monde.
 *
 * @param dimension identifiant de la dimension distante
 * @param packet paquet de jeu vanilla, serveur vers client
 */
public record RemoteEntityPacketPayload(Identifier dimension, Packet<? super ClientPlayPacketListener> packet) implements CustomPayload {

    public static final CustomPayload.Id<RemoteEntityPacketPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "portal_remote_entity"));

    public static final PacketCodec<RegistryByteBuf, RemoteEntityPacketPayload> CODEC =
            PacketCodec.of(RemoteEntityPacketPayload::write, RemoteEntityPacketPayload::read);

    /**
     * Codec des paquets de jeu, lie aux registres de la connexion. Un par jeu de registres : en solo, serveur
     * integre et client encodent et decodent dans la meme JVM avec des registres distincts.
     */
    private static final Map<DynamicRegistryManager, PacketCodec<ByteBuf, Packet<? super ClientPlayPacketListener>>> PACKET_CODECS =
            new WeakHashMap<>();

    private static synchronized PacketCodec<ByteBuf, Packet<? super ClientPlayPacketListener>> packetCodec(RegistryByteBuf buf) {
        return PACKET_CODECS.computeIfAbsent(buf.getRegistryManager(),
                registries -> PlayStateFactories.S2C.bind(RegistryByteBuf.makeFactory(registries)).codec());
    }

    private void write(RegistryByteBuf buf) {
        buf.writeIdentifier(this.dimension);
        packetCodec(buf).encode(buf, this.packet);
    }

    private static RemoteEntityPacketPayload read(RegistryByteBuf buf) {
        Identifier dimension = buf.readIdentifier();
        return new RemoteEntityPacketPayload(dimension, packetCodec(buf).decode(buf));
    }

    @Override
    public CustomPayload.Id<RemoteEntityPacketPayload> getId() {
        return ID;
    }
}
