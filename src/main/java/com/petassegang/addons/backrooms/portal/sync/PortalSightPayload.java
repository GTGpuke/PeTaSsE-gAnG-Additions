package com.petassegang.addons.backrooms.portal.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import com.petassegang.addons.core.ModConstants;

/**
 * Client vers serveur : les portails que la camera du joueur voit, dans le monde ou il est (voir {@link PortalSight}).
 *
 * @param seen identifiants uniques des portails vus, au plus {@link #MAX_PORTALS}
 */
public record PortalSightPayload(List<UUID> seen) implements CustomPayload {

    /** Portails rapportes au plus : bien au-dela de ce qu'un ecran montre, en deca d'un abus. */
    public static final int MAX_PORTALS = 64;

    public static final CustomPayload.Id<PortalSightPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "portal_sight"));

    public static final PacketCodec<PacketByteBuf, PortalSightPayload> CODEC =
            PacketCodec.of(PortalSightPayload::write, PortalSightPayload::read);

    private void write(PacketByteBuf buf) {
        buf.writeCollection(this.seen, (out, uuid) -> out.writeUuid(uuid));
    }

    private static PortalSightPayload read(PacketByteBuf buf) {
        return new PortalSightPayload(buf.readCollection(PacketByteBuf.getMaxValidator(ArrayList::new, MAX_PORTALS), in -> in.readUuid()));
    }

    @Override
    public CustomPayload.Id<PortalSightPayload> getId() {
        return ID;
    }
}
