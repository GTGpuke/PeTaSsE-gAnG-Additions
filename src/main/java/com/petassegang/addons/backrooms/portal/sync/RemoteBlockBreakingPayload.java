package com.petassegang.addons.backrooms.portal.sync;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import com.petassegang.addons.core.ModConstants;

/**
 * Serveur vers client : avancement du minage d'un bloc a travers un portail, pour dessiner ses fissures.
 *
 * @param dimension dimension du bloc
 * @param pos position du bloc
 * @param stage etape de fissure de 0 a 9, ou -1 pour l'effacer
 */
public record RemoteBlockBreakingPayload(Identifier dimension, BlockPos pos, int stage) implements CustomPayload {

    public static final CustomPayload.Id<RemoteBlockBreakingPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ModConstants.MOD_ID, "portal_remote_block_breaking"));

    public static final PacketCodec<PacketByteBuf, RemoteBlockBreakingPayload> CODEC =
            PacketCodec.of(RemoteBlockBreakingPayload::write, RemoteBlockBreakingPayload::read);

    private void write(PacketByteBuf buf) {
        buf.writeIdentifier(this.dimension);
        buf.writeBlockPos(this.pos);
        buf.writeByte(this.stage);
    }

    private static RemoteBlockBreakingPayload read(PacketByteBuf buf) {
        return new RemoteBlockBreakingPayload(buf.readIdentifier(), buf.readBlockPos(), buf.readByte());
    }

    @Override
    public CustomPayload.Id<RemoteBlockBreakingPayload> getId() {
        return ID;
    }
}
