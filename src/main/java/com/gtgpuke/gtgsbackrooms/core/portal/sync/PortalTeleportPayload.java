package com.gtgpuke.gtgsbackrooms.core.portal.sync;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;

/**
 * Serveur vers client : le joueur vient de passer un portail, voici ou il se trouve maintenant.
 *
 * <p>Remplace le paquet de respawn vanilla pour un changement de dimension par portail : le client
 * bascule sur le monde distant qu'il a deja charge, sans ecran de chargement. Si le client avait deja
 * predit le passage, il ne fait qu'aligner sa position sur celle du serveur. Le meme payload sert a
 * ramener un client dont la prediction a ete refusee.
 *
 * @param dimension dimension ou se trouve le joueur
 * @param dimensionType type de cette dimension, pour creer le monde client si besoin
 * @param position position des pieds
 * @param yaw lacet en degres
 * @param pitch tangage en degres
 * @param velocity vitesse
 */
public record PortalTeleportPayload(Identifier dimension, Identifier dimensionType, Vec3d position, float yaw, float pitch, Vec3d velocity)
        implements CustomPayload {

    public static final CustomPayload.Id<PortalTeleportPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "portal_teleport"));

    public static final PacketCodec<PacketByteBuf, PortalTeleportPayload> CODEC =
            PacketCodec.of(PortalTeleportPayload::write, PortalTeleportPayload::read);

    /** Payload decrivant l'etat d'un joueur dans un monde serveur. */
    public static PortalTeleportPayload of(ServerWorld world, Vec3d position, float yaw, float pitch, Vec3d velocity) {
        return new PortalTeleportPayload(
                world.getRegistryKey().getValue(), RemoteDimensionPayload.dimensionTypeId(world), position, yaw, pitch, velocity);
    }

    private void write(PacketByteBuf buf) {
        buf.writeIdentifier(this.dimension);
        buf.writeIdentifier(this.dimensionType);
        buf.writeVec3d(this.position);
        buf.writeFloat(this.yaw);
        buf.writeFloat(this.pitch);
        buf.writeVec3d(this.velocity);
    }

    private static PortalTeleportPayload read(PacketByteBuf buf) {
        return new PortalTeleportPayload(
                buf.readIdentifier(), buf.readIdentifier(), buf.readVec3d(), buf.readFloat(), buf.readFloat(), buf.readVec3d());
    }

    @Override
    public CustomPayload.Id<PortalTeleportPayload> getId() {
        return ID;
    }
}
