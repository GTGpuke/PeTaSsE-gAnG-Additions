package com.petassegang.addons.backrooms.portal.sync;

import java.util.UUID;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.core.ModConstants;

/**
 * Client vers serveur : la camera du joueur vient de franchir le plan d'un portail.
 *
 * <p>Le client a deja bascule de son cote (prediction) ; il transmet l'etat exact qu'il a utilise
 * pour que le serveur calcule la meme arrivee, apres verification de plausibilite.
 *
 * @param portal UUID du portail traverse
 * @param position position des pieds au moment du franchissement
 * @param velocity vitesse au moment du franchissement
 * @param yaw lacet en degres
 * @param pitch tangage en degres
 */
public record PortalCrossPayload(UUID portal, Vec3d position, Vec3d velocity, float yaw, float pitch) implements CustomPayload {

    public static final CustomPayload.Id<PortalCrossPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "portal_cross"));

    public static final PacketCodec<PacketByteBuf, PortalCrossPayload> CODEC =
            PacketCodec.of(PortalCrossPayload::write, PortalCrossPayload::read);

    private void write(PacketByteBuf buf) {
        buf.writeUuid(this.portal);
        buf.writeVec3d(this.position);
        buf.writeVec3d(this.velocity);
        buf.writeFloat(this.yaw);
        buf.writeFloat(this.pitch);
    }

    private static PortalCrossPayload read(PacketByteBuf buf) {
        return new PortalCrossPayload(buf.readUuid(), buf.readVec3d(), buf.readVec3d(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public CustomPayload.Id<PortalCrossPayload> getId() {
        return ID;
    }
}
