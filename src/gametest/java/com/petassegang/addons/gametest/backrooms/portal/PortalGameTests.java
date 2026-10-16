package com.petassegang.addons.gametest.backrooms.portal;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.backrooms.portal.sync.RemoteChunkPayload;
import com.petassegang.addons.backrooms.portal.transition.TeleportExecutor;
import com.petassegang.addons.backrooms.portal.transition.TpCooldownTracker;
import com.petassegang.addons.gametest.support.GameTestPlayers;

/**
 * Tests en jeu des portails sur un vrai serveur : teleportation du joueur et des objets dans le meme monde, payload de chunk.
 *
 * <p>Les portails sont poses a la main entre deux points de l'overworld, ce qui exerce la transposition, le cooldown
 * et le reseau sans autre dimension.
 */
public final class PortalGameTests implements FabricGameTest {

    private static final double EPSILON = 1.0e-3;

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 100)
    @SuppressWarnings("removal")
    public void crossingAPortalTeleportsThePlayerTowardsTheExit(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d source = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 2, 1)));
        Vec3d destination = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(6, 2, 6)));
        BackroomsPortalEntity portal = BackroomsPortalEntity.builder(world)
                .position(source)
                .facingSource(PortalFacing.NORTH)
                .facingDestination(PortalFacing.EAST)
                .destinationDim(World.OVERWORLD)
                .destinationPos(destination)
                .size(1.0f, 2.0f)
                .build();
        world.spawnEntity(portal);
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        try {
            // Le joueur vient de franchir le plan (5 cm derriere), les pieds au bas du cadre, et regarde vers le sud.
            player.refreshPositionAndAngles(source.x, source.y - 1.0, source.z + 0.05, 0.0f, 0.0f);
            context.assertTrue(TeleportExecutor.execute(player, portal), "La teleportation a lieu.");

            // 5 cm derriere le portail d'entree = 5 cm devant le portail de sortie, plus la marge de sortie, vers l'est.
            Vec3d expected = destination.add(0.05 + TeleportExecutor.EXIT_OFFSET, -1.0, 0.0);
            context.assertTrue(player.getPos().isInRange(expected, EPSILON), "Le joueur ressort devant le portail de sortie.");
            context.assertTrue(Math.abs(player.getYaw() + 90.0f) < 0.01f, "Le joueur regarde vers l'est, loin du portail de sortie.");
            context.assertTrue(TpCooldownTracker.isOnCooldown(player.getUuid(), world.getTime(), 20), "Le cooldown est arme.");
        } finally {
            world.getServer().getPlayerManager().remove(player);
            portal.discard();
        }
        context.complete();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 100)
    public void itemsThrownThroughAPortalComeOutOfTheExit(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d source = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 2, 1)));
        Vec3d destination = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(4, 2, 6)));
        BackroomsPortalEntity portal = BackroomsPortalEntity.builder(world)
                .position(source)
                .facingSource(PortalFacing.NORTH)
                .facingDestination(PortalFacing.WEST)
                .destinationDim(World.OVERWORLD)
                .destinationPos(destination)
                .size(1.0f, 2.0f)
                .build();
        world.spawnEntity(portal);
        // Un diamant lance du cote d'entree, droit vers le plan, sans gravite pour une trajectoire previsible.
        ItemEntity item = new ItemEntity(world, source.x, source.y, source.z - 0.3, new ItemStack(Items.DIAMOND));
        item.setNoGravity(true);
        item.setVelocity(0.0, 0.0, 0.5);
        world.spawnEntity(item);

        // Sortie vers l'ouest et verification rapide : l'objet reste dans la zone degagee du test (les voisins sont a quelques blocs).
        context.runAtTick(6, () -> {
            try {
                context.assertTrue(!item.isRemoved(), "Dans la meme dimension, l'objet reste la meme entity.");
                context.assertTrue(item.getX() < destination.x - TeleportExecutor.EXIT_OFFSET
                        && Math.abs(item.getZ() - destination.z) < 0.5 && Math.abs(item.getY() - destination.y) < 0.5,
                        "L'objet ressort devant le portail de sortie et file vers l'ouest : " + item.getPos() + ".");
                context.assertTrue(item.getVelocity().x < 0.0 && Math.abs(item.getVelocity().z) < 1.0e-6,
                        "La vitesse est transposee vers l'ouest : " + item.getVelocity() + ".");
                context.assertTrue(item.hasPortalCooldown(), "Le cooldown de portail vanilla est arme.");
            } finally {
                item.discard();
                portal.discard();
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA)
    public void remoteChunkPayloadSurvivesEncoding(TestContext context) {
        ServerWorld world = context.getWorld();
        WorldChunk chunk = world.getWorldChunk(context.getAbsolutePos(BlockPos.ORIGIN));
        RemoteChunkPayload payload = RemoteChunkPayload.of(world, chunk);
        RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), world.getRegistryManager());
        RemoteChunkPayload.CODEC.encode(buf, payload);
        RemoteChunkPayload decoded = RemoteChunkPayload.CODEC.decode(buf);

        context.assertTrue(buf.readableBytes() == 0, "Tout le tampon est consomme.");
        context.assertTrue(decoded.dimension().equals(payload.dimension())
                && decoded.chunkX() == chunk.getPos().x && decoded.chunkZ() == chunk.getPos().z, "Dimension et coordonnees conservees.");
        context.assertTrue(ByteBufUtil.equals(decoded.chunkData().getSectionsDataBuf(), payload.chunkData().getSectionsDataBuf()),
                "Les sections sont identiques octet pour octet.");
        context.assertTrue(decoded.chunkData().getHeightmap().equals(payload.chunkData().getHeightmap()), "Les heightmaps sont identiques.");
        context.assertTrue(decoded.lightData().getInitedBlock().equals(payload.lightData().getInitedBlock())
                && decoded.lightData().getInitedSky().equals(payload.lightData().getInitedSky())
                && decoded.lightData().getSkyNibbles().size() == payload.lightData().getSkyNibbles().size(), "La lumiere est conservee.");
        context.complete();
    }
}
