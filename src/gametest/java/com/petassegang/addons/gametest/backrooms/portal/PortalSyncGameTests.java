package com.petassegang.addons.gametest.backrooms.portal;

import java.util.UUID;

import net.minecraft.block.Blocks;
import net.minecraft.block.SnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntityRegistry;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.backrooms.portal.sync.PortalCrossPayload;
import com.petassegang.addons.backrooms.portal.sync.PortalViewTracker;
import com.petassegang.addons.backrooms.portal.sync.RemoteDimensionPayload;
import com.petassegang.addons.backrooms.portal.transition.TeleportExecutor;
import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.gametest.support.GameTestPlayers;

/**
 * Tests en jeu de la synchronisation des dimensions distantes et des traversees entre dimensions.
 *
 * <p>Le serveur de test possede l'overworld, le Nether et l'End, mais pas le Level 0 : le Nether tient lieu de
 * dimension d'arrivee. Son point d'arrivee est au-dessus du plafond de bedrock, dans l'air. Le monde est neuf a
 * chaque lancement : les tests qui attendent des chunks du Nether guettent leur arrivee au lieu de compter les ticks.
 */
public final class PortalSyncGameTests implements FabricGameTest {

    private static final Vec3d NETHER_EXIT = new Vec3d(8.5, 200.0, 8.5);
    private static final BlockPos PORTAL = new BlockPos(1, 2, 6);
    /** Trois blocs et demi au nord du portail, qui regarde le nord : cote d'entree, de face. */
    private static final Vec3d VIEWER = new Vec3d(1.5, 1.0, 3.0);
    private static final double EPSILON = 1.0e-3;
    /** Ticks laisses a la generation du Nether avant de declarer un test en echec (le serveur de test tique sans attendre). */
    private static final int SYNC_PATIENCE_TICKS = 2000;
    /** Rayon, en chunks, de la zone d'arrivee generee d'avance : la fenetre synchronisee et le ticket elargi tiennent dedans. */
    private static final int PREGENERATED_RADIUS = 4;

    private static ServerWorld nether(TestContext context) {
        ServerWorld nether = context.getWorld().getServer().getWorld(World.NETHER);
        if (nether == null) {
            throw new GameTestException("Le serveur de test n'a pas de Nether.");
        }
        return nether;
    }

    /**
     * Genere d'avance les chunks du Nether autour de l'arrivee. Le serveur de test tique sans attendre et repart d'un
     * monde neuf : la generation asynchrone du Nether, elle, prend ses secondes, et depasserait la patience des tests.
     */
    private static void pregenerateArrival(ServerWorld nether, Vec3d arrival) {
        int centerX = ChunkSectionPos.getSectionCoord(arrival.x);
        int centerZ = ChunkSectionPos.getSectionCoord(arrival.z);
        for (int dx = -PREGENERATED_RADIUS; dx <= PREGENERATED_RADIUS; dx++) {
            for (int dz = -PREGENERATED_RADIUS; dz <= PREGENERATED_RADIUS; dz++) {
                nether.getChunk(centerX + dx, centerZ + dz);
            }
        }
    }

    /** Portail vers le Nether, regardant le nord, au point donne du test. */
    private static BackroomsPortalEntity netherPortal(TestContext context, BlockPos relative, Vec3d destination) {
        ServerWorld world = context.getWorld();
        BackroomsPortalEntity portal = BackroomsPortalEntity.builder(world)
                .position(Vec3d.ofCenter(context.getAbsolutePos(relative)))
                .facingSource(PortalFacing.NORTH)
                .facingDestination(PortalFacing.SOUTH)
                .destinationDim(World.NETHER)
                .destinationPos(destination)
                .size(1.0f, 2.0f)
                .build();
        world.spawnEntity(portal);
        return portal;
    }

    private static BackroomsPortalEntity netherPortal(TestContext context) {
        return netherPortal(context, PORTAL, NETHER_EXIT);
    }

    private static ServerPlayerEntity viewer(TestContext context, GameMode mode) {
        return GameTestPlayers.connect(context, mode, context.getAbsolute(VIEWER), 0.0f, 0.0f);
    }

    private static void moveTo(ServerPlayerEntity player, Vec3d position, float yaw) {
        player.refreshPositionAndAngles(position.x, position.y, position.z, yaw, 0.0f);
        player.networkHandler.syncWithPlayerPosition();
    }

    /** Portail du meme monde, du nord vers l'est, entre deux points du test. */
    private static BackroomsPortalEntity localPortal(TestContext context, Vec3d source, Vec3d destination, boolean teleportable) {
        ServerWorld world = context.getWorld();
        BackroomsPortalEntity portal = BackroomsPortalEntity.builder(world)
                .position(source).facingSource(PortalFacing.NORTH).facingDestination(PortalFacing.EAST)
                .destinationDim(World.OVERWORLD).destinationPos(destination).size(1.0f, 2.0f).teleportable(teleportable).build();
        world.spawnEntity(portal);
        return portal;
    }

    private static RegistryKey<World> nowhere() {
        return RegistryKey.of(RegistryKeys.WORLD, Identifier.of(ModConstants.MOD_ID, "nulle_part"));
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 3000, batchId = "portal_sync_window")
    public void aPlayerLookingAtDimensionPortalsReceivesTheRemoteWindow(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerWorld nether = nether(context);
        pregenerateArrival(nether, NETHER_EXIT);
        // Le portail lointain est inscrit en premier : le portail proche, examine ensuite, le remplace comme foyer.
        BackroomsPortalEntity farPortal = netherPortal(context, new BlockPos(13, 2, 16), NETHER_EXIT.add(480.0, 0.0, 0.0));
        BackroomsPortalEntity portal = netherPortal(context);
        // Un troisieme portail dont l'arrivee tient dans la fenetre du premier : leurs chunks voulus se cumulent.
        BackroomsPortalEntity sibling = netherPortal(context, PORTAL.east(2), NETHER_EXIT.add(16.0, 0.0, 0.0));
        // Un quatrieme portail vers la meme arrivee que le premier : ils partagent la fenetre chargee et son ticket.
        BackroomsPortalEntity twin = netherPortal(context, PORTAL.west(), NETHER_EXIT);
        ServerPlayerEntity player = viewer(context, GameMode.CREATIVE);
        BlockPos remoteBlock = BlockPos.ofFloored(NETHER_EXIT).down(3);
        BlockPos farRemoteBlock = BlockPos.ofFloored(NETHER_EXIT).add(600, 0, 0);
        Vec3d plane = portal.getPos();
        int[] phase = {0};
        int[] phaseStart = {0};
        ItemEntity[] remoteItem = new ItemEntity[1];
        ItemEntity[] thrown = new ItemEntity[1];
        BlockPos localBlock = context.getAbsolutePos(new BlockPos(20, 0, 20));

        for (int tick = 1; tick < 2990; tick++) {
            int now = tick;
            context.runAtTick(tick, () -> {
                int elapsed = now - phaseStart[0];
                switch (phase[0]) {
                    case 0 -> {
                        // La fenetre du premier portail arrive, chunk par chunk, des que le Nether est genere.
                        if (PortalViewTracker.watchedChunkCount() > 0) {
                            remoteItem[0] = new ItemEntity(nether, NETHER_EXIT.x, NETHER_EXIT.y, NETHER_EXIT.z + 1.0, new ItemStack(Items.NETHER_STAR));
                            remoteItem[0].setNoGravity(true);
                            nether.spawnEntity(remoteItem[0]);
                            nether.setBlockState(remoteBlock, Blocks.GLOWSTONE.getDefaultState());
                            // Un bloc change hors de la fenetre : relaye a personne.
                            nether.setBlockState(farRemoteBlock, Blocks.GLOWSTONE.getDefaultState());
                            phase[0] = 1;
                            phaseStart[0] = now;
                        } else if (now > SYNC_PATIENCE_TICKS) {
                            throw new GameTestException("Aucun chunk du Nether envoye au joueur en " + now + " ticks.");
                        }
                    }
                    case 1 -> {
                        if (elapsed == 30) {
                            // L'objet distant reste : son suivi sera retire avec la fenetre, puis avec la vue.
                            context.assertTrue(remoteItem[0].isAlive(), "L'objet distant vit dans le Nether.");
                            nether.setBlockState(remoteBlock, Blocks.AIR.getDefaultState());
                            nether.setBlockState(farRemoteBlock, Blocks.AIR.getDefaultState());
                            // Un bloc change dans la dimension du joueur, qu'aucune vue distante ne tient : relaye a personne.
                            world.setBlockState(localBlock, Blocks.GLOWSTONE.getDefaultState());
                            // Un objet lance a travers le portail change de dimension.
                            thrown[0] = new ItemEntity(world, plane.x, plane.y, plane.z - 0.3, new ItemStack(Items.GOLD_NUGGET));
                            thrown[0].setNoGravity(true);
                            thrown[0].setVelocity(0.0, 0.0, 0.5);
                            world.spawnEntity(thrown[0]);
                            // Le jumeau disparait : la fenetre reste chargee pour le premier portail.
                            twin.discard();
                            // Le joueur se rapproche du portail lointain, sans etre a portee imminente : le foyer change et la
                            // fenetre du premier portail, qui ne tient pas autour du nouveau foyer, est dechargee.
                            moveTo(player, context.getAbsolute(new Vec3d(13.5, 1.0, 7.0)), 0.0f);
                        } else if (elapsed == 50) {
                            context.assertTrue(thrown[0].isRemoved() || thrown[0].getWorld() == nether, "La pepite a quitte l'overworld par le portail.");
                            world.setBlockState(localBlock, Blocks.AIR.getDefaultState());
                        } else if (elapsed == 110) {
                            // Retour devant le premier portail : sa fenetre, toujours chargee, est renvoyee.
                            moveTo(player, context.getAbsolute(VIEWER), 0.0f);
                            phase[0] = 2;
                            phaseStart[0] = now;
                        }
                    }
                    case 2 -> {
                        if (PortalViewTracker.watchedChunkCount() > 0) {
                            // Puis le joueur part a cent blocs : plus aucun portail regarde, tout est decharge apres le delai de grace.
                            moveTo(player, context.getAbsolute(VIEWER).add(100.0, 0.0, 0.0), 0.0f);
                            phase[0] = 3;
                            phaseStart[0] = now;
                        } else if (elapsed > SYNC_PATIENCE_TICKS) {
                            throw new GameTestException("La fenetre du premier portail n'est pas renvoyee.");
                        }
                    }
                    case 3 -> {
                        if (elapsed == 120) {
                            context.assertTrue(PortalViewTracker.watchedChunkCount() == 0,
                                    "Les chunks du Nether sont decharges chez un joueur qui ne regarde plus aucun portail.");
                            // Un joueur mort n'est plus suivi pour la traversee.
                            player.kill();
                        } else if (elapsed == 125) {
                            try {
                                context.assertTrue(!player.isAlive(), "Le joueur est mort.");
                            } finally {
                                GameTestPlayers.disconnect(player);
                                remoteItem[0].discard();
                                portal.discard();
                                farPortal.discard();
                                sibling.discard();
                                twin.discard();
                            }
                            context.complete();
                        }
                    }
                    default -> {
                        // Test termine.
                    }
                }
            });
        }
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 2200, batchId = "portal_sync_crossing")
    public void crossingADimensionPortalMovesThePlayerToTheNetherWithoutResendingItsChunks(TestContext context) {
        ServerWorld nether = nether(context);
        pregenerateArrival(nether, NETHER_EXIT);
        BackroomsPortalEntity portal = netherPortal(context);
        ServerPlayerEntity player = viewer(context, GameMode.CREATIVE);
        Vec3d plane = portal.getPos();
        int[] crossedAt = {-1};

        // Le joueur regarde le portail et traverse des la premiere poignee de chunks du Nether recue : vanilla lui
        // reproposera ensuite ces chunks-la, tous proches de l'arrivee, et n'en renverra aucun.
        for (int tick = 20; tick < 2190; tick++) {
            int now = tick;
            context.runAtTick(tick, () -> {
                if (crossedAt[0] < 0) {
                    if (PortalViewTracker.watchedChunkCount() > 0) {
                        crossedAt[0] = now;
                        player.refreshPositionAndAngles(plane.x, plane.y - 1.0, plane.z + 0.05, 0.0f, 0.0f);
                        context.assertTrue(TeleportExecutor.execute(player, portal), "La traversee a lieu.");
                        context.assertTrue(player.getServerWorld() == nether, "Le joueur est dans le Nether.");
                        Vec3d expected = NETHER_EXIT.add(0.0, -1.0, 0.05 + TeleportExecutor.EXIT_OFFSET);
                        context.assertTrue(player.getPos().isInRange(expected, EPSILON), "Le joueur ressort devant le portail d'arrivee : " + player.getPos() + ".");
                    } else if (now > SYNC_PATIENCE_TICKS) {
                        throw new GameTestException("Aucun chunk du Nether recu avant la traversee.");
                    }
                } else if (now - crossedAt[0] < 60) {
                    // Le gestionnaire reseau du joueur tique : vanilla lui propose les chunks du Nether, sauf ceux qu'il tient deja.
                    GameTestPlayers.tick(player);
                } else if (now - crossedAt[0] == 60) {
                    try {
                        context.assertTrue(player.getServerWorld() == nether && player.isAlive(), "Le joueur reste dans le Nether.");
                    } finally {
                        GameTestPlayers.disconnect(player);
                        portal.discard();
                    }
                    context.complete();
                }
            });
        }
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 100)
    public void theClientAnnouncesItsCrossingAndTheServerChecksIt(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerWorld nether = nether(context);
        BackroomsPortalEntity portal = netherPortal(context);
        BackroomsPortalEntity closed = netherPortal(context, PORTAL.east(2), NETHER_EXIT);
        closed.setTeleportable(false);
        BackroomsPortalEntity broken = BackroomsPortalEntity.builder(world)
                .position(Vec3d.ofCenter(context.getAbsolutePos(PORTAL.east(4)))).facingSource(PortalFacing.NORTH).facingDestination(PortalFacing.SOUTH)
                .destinationDim(nowhere()).destinationPos(NETHER_EXIT).size(1.0f, 2.0f).build();
        world.spawnEntity(broken);
        ServerPlayerEntity player = viewer(context, GameMode.CREATIVE);
        Vec3d plane = portal.getPos();
        Vec3d feet = new Vec3d(plane.x, plane.y - 1.0, plane.z + 0.05);
        BoatEntity boat = EntityType.BOAT.create(world);
        Vec3d boatPos = context.getAbsolute(new Vec3d(8.5, 1.0, 3.5));
        boat.refreshPositionAndAngles(boatPos.x, boatPos.y, boatPos.z, 0.0f, 0.0f);
        world.spawnEntity(boat);
        // Portail du meme monde : une annonce acceptee, puis une seconde aussitot, refusee par le cooldown du serveur de test.
        Vec3d localSource = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(12, 2, 1)));
        BackroomsPortalEntity local = localPortal(context, localSource, Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(16, 2, 6))), true);
        Vec3d localFeet = localSource.add(0.0, -1.0, 0.05);

        context.runAtTick(5, () -> {
            player.refreshPositionAndAngles(localFeet.x, localFeet.y, localFeet.z - 0.3, 0.0f, 0.0f);
            GameTestPlayers.receive(player, new PortalCrossPayload(local.getUuid(), localFeet, Vec3d.ZERO, 0.0f, 0.0f));
            context.assertTrue(player.getX() > localSource.x + 2.0, "L'annonce locale plausible est appliquee : " + player.getPos() + ".");
            GameTestPlayers.receive(player, new PortalCrossPayload(local.getUuid(), player.getPos(), Vec3d.ZERO, 0.0f, 0.0f));
            context.assertTrue(player.getX() > localSource.x + 2.0, "Sous cooldown, la seconde annonce est refusee sans teleporter.");
        });
        context.runAtTick(10, () -> {
            // Cooldown ecoule. Annonces refusees : position invraisemblable, portail inconnu, portail ferme, dimension
            // d'arrivee absente, position hors du cadre, joueur en bateau.
            player.refreshPositionAndAngles(feet.x, feet.y, feet.z - 0.3, 0.0f, 0.0f);
            GameTestPlayers.receive(player, new PortalCrossPayload(portal.getUuid(), feet.add(20.0, 0.0, 0.0), Vec3d.ZERO, 0.0f, 0.0f));
            GameTestPlayers.receive(player, new PortalCrossPayload(UUID.randomUUID(), feet, Vec3d.ZERO, 0.0f, 0.0f));
            Vec3d closedFeet = closed.getPos().add(0.0, -1.0, 0.05);
            player.refreshPositionAndAngles(closedFeet.x, closedFeet.y, closedFeet.z - 0.3, 0.0f, 0.0f);
            GameTestPlayers.receive(player, new PortalCrossPayload(closed.getUuid(), closedFeet, Vec3d.ZERO, 0.0f, 0.0f));
            Vec3d brokenFeet = broken.getPos().add(0.0, -1.0, 0.05);
            player.refreshPositionAndAngles(brokenFeet.x, brokenFeet.y, brokenFeet.z - 0.3, 0.0f, 0.0f);
            GameTestPlayers.receive(player, new PortalCrossPayload(broken.getUuid(), brokenFeet, Vec3d.ZERO, 0.0f, 0.0f));
            player.refreshPositionAndAngles(feet.x, feet.y, feet.z - 0.3, 0.0f, 0.0f);
            GameTestPlayers.receive(player, new PortalCrossPayload(portal.getUuid(), feet.add(2.5, 0.0, 0.0), Vec3d.ZERO, 0.0f, 0.0f));
            context.assertTrue(player.startRiding(boat, true), "Le joueur monte dans le bateau.");
            GameTestPlayers.receive(player, new PortalCrossPayload(portal.getUuid(), feet, Vec3d.ZERO, 0.0f, 0.0f));
            player.stopRiding();
            context.assertTrue(player.getServerWorld() == world, "Aucune annonce refusee ne teleporte.");
            // Annonce plausible : le joueur a franchi le plan de 5 cm. Le serveur s'aligne sur l'etat du client.
            player.refreshPositionAndAngles(feet.x, feet.y, feet.z - 0.3, 0.0f, 0.0f);
            GameTestPlayers.receive(player, new PortalCrossPayload(portal.getUuid(), feet, new Vec3d(0.0, 0.0, 0.2), 0.0f, 0.0f));
            context.assertTrue(player.getServerWorld() == nether, "L'annonce plausible est appliquee : le joueur est dans le Nether.");
        });
        context.runAtTick(11, () -> {
            try {
                // Depuis le Nether, le portail de l'overworld n'existe pas : l'annonce est refusee sans teleporter.
                GameTestPlayers.receive(player, new PortalCrossPayload(portal.getUuid(), player.getPos(), Vec3d.ZERO, 0.0f, 0.0f));
                context.assertTrue(player.getServerWorld() == nether, "Un portail d'un autre monde ne teleporte pas.");
            } finally {
                GameTestPlayers.disconnect(player);
                boat.discard();
                local.discard();
                closed.discard();
                broken.discard();
                portal.discard();
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 100)
    public void theServerCatchesAWalkingPlayerWhenTheClientStaysSilent(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d source = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 2, 1)));
        Vec3d destination = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(6, 2, 6)));
        BackroomsPortalEntity portal = localPortal(context, source, destination, true);
        // Six couches de neige a l'arrivee (collision a cinq huitiemes) : les pieds du joueur y chevaucheraient le sol, il est remonte.
        BlockPos arrivalFloor = BlockPos.ofFloored(destination.x, destination.y - 1.0, destination.z);
        world.setBlockState(arrivalFloor.down(), Blocks.STONE.getDefaultState());
        world.setBlockState(arrivalFloor, Blocks.SNOW.getDefaultState().with(SnowBlock.LAYERS, 6));
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.SURVIVAL,
                new Vec3d(source.x, source.y - 1.0, source.z - 0.6), 0.0f, 0.0f);
        double[] z = {source.z - 0.6};

        // Le joueur marche vers le sud a 0,2 bloc par tick, sans jamais annoncer son franchissement, puis continue a
        // avancer d'un centimetre par tick : le serveur ne reexamine un joueur qu'en mouvement.
        for (int tick = 1; tick <= 20; tick++) {
            context.runAtTick(tick, () -> {
                if (player.getX() < destination.x) {
                    z[0] += z[0] < source.z + 0.5 ? 0.2 : 0.01;
                    player.refreshPositionAndAngles(source.x, source.y - 1.0, z[0], 0.0f, 0.0f);
                } else {
                    // Arrive : il continue vers l'est, loin du portail de sortie, pendant le cooldown de traversee.
                    player.refreshPositionAndAngles(player.getX() + 0.01, player.getY(), player.getZ(), player.getYaw(), player.getPitch());
                }
            });
        }
        context.runAtTick(25, () -> {
            try {
                context.assertTrue(player.getX() > destination.x, "Le serveur a detecte la marche et teleporte le joueur a l'arrivee : " + player.getPos() + ".");
                context.assertTrue(player.getY() > destination.y - 1.0 + 0.1, "Le joueur a ete remonte au-dessus de la neige : " + player.getY() + ".");
            } finally {
                GameTestPlayers.disconnect(player);
                portal.discard();
                world.setBlockState(arrivalFloor, Blocks.AIR.getDefaultState());
                world.setBlockState(arrivalFloor.down(), Blocks.AIR.getDefaultState());
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 60)
    public void aRidingOrJumpingPlayerIsNotCaughtByTheServer(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d source = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 2, 1)));
        Vec3d destination = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(6, 2, 6)));
        BackroomsPortalEntity portal = localPortal(context, source, destination, true);
        BoatEntity boat = EntityType.BOAT.create(world);
        boat.setNoGravity(true);
        boat.refreshPositionAndAngles(source.x, source.y - 1.0, source.z - 0.6, 0.0f, 0.0f);
        world.spawnEntity(boat);
        ServerPlayerEntity rider = GameTestPlayers.connect(context, GameMode.SURVIVAL, new Vec3d(source.x, source.y - 1.0, source.z - 0.6), 0.0f, 0.0f);
        ServerPlayerEntity jumper = GameTestPlayers.connect(context, GameMode.SURVIVAL, new Vec3d(source.x, source.y - 1.0, source.z - 10.0), 0.0f, 0.0f);
        context.runAtTick(1, () -> context.assertTrue(rider.startRiding(boat, true), "Le joueur monte dans le bateau."));
        for (int tick = 2; tick <= 10; tick++) {
            context.runAtTick(tick, () -> {
                // Le bateau et son passager franchissent le plan ; le second joueur saute de dix blocs d'un coup.
                boat.refreshPositionAndAngles(boat.getX(), boat.getY(), boat.getZ() + 0.2, 0.0f, 0.0f);
                rider.refreshPositionAndAngles(boat.getX(), boat.getY(), boat.getZ(), 0.0f, 0.0f);
                jumper.refreshPositionAndAngles(source.x, source.y - 1.0, jumper.getZ() < source.z ? source.z + 0.3 : source.z - 10.0, 0.0f, 0.0f);
            });
        }
        context.runAtTick(20, () -> {
            try {
                context.assertTrue(rider.getX() < destination.x, "En bateau, le joueur n'est pas teleporte par le serveur.");
                context.assertTrue(jumper.getX() < destination.x, "Un saut de position n'est pas une marche : pas de teleportation.");
            } finally {
                rider.stopRiding();
                GameTestPlayers.disconnect(rider);
                GameTestPlayers.disconnect(jumper);
                boat.discard();
                portal.discard();
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 100)
    public void anArrivalInsideAFullBlockIsLeftWhereItIs(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d source = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 2, 1)));
        Vec3d destination = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(6, 2, 6)));
        BackroomsPortalEntity portal = localPortal(context, source, destination, true);
        BlockPos arrivalBlock = BlockPos.ofFloored(destination.x, destination.y - 1.0, destination.z);
        world.setBlockState(arrivalBlock, Blocks.STONE.getDefaultState());
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.CREATIVE,
                new Vec3d(source.x, source.y - 1.0, source.z + 0.05), 0.0f, 0.0f);
        try {
            context.assertTrue(TeleportExecutor.execute(player, portal), "La traversee a lieu.");
            context.assertTrue(Math.abs(player.getY() - (destination.y - 1.0)) < EPSILON,
                    "Un bloc plein ne se contourne pas par une remontee de trente centimetres : " + player.getY() + ".");
        } finally {
            GameTestPlayers.disconnect(player);
            portal.discard();
            world.setBlockState(arrivalBlock, Blocks.AIR.getDefaultState());
        }
        context.complete();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 2200, batchId = "portal_sync_vanilla")
    public void vanillaWorldChangesResetThePlayersSynchronization(TestContext context) {
        ServerWorld nether = nether(context);
        pregenerateArrival(nether, NETHER_EXIT);
        BackroomsPortalEntity portal = netherPortal(context);
        ServerPlayerEntity player = viewer(context, GameMode.CREATIVE);
        ServerPlayerEntity[] respawned = new ServerPlayerEntity[1];
        int[] syncedAt = {-1};

        for (int tick = 20; tick < 2190; tick++) {
            int now = tick;
            context.runAtTick(tick, () -> {
                if (syncedAt[0] < 0) {
                    if (PortalViewTracker.watchedChunkCount() > 0) {
                        syncedAt[0] = now;
                        Entity moved = player.teleportTo(new TeleportTarget(nether, NETHER_EXIT, Vec3d.ZERO, 0.0f, 0.0f, TeleportTarget.NO_OP));
                        context.assertTrue(moved == player && player.getServerWorld() == nether, "Changement de dimension vanilla.");
                    } else if (now > SYNC_PATIENCE_TICKS) {
                        throw new GameTestException("Aucun chunk du Nether envoye au joueur.");
                    }
                } else if (now - syncedAt[0] == 20) {
                    respawned[0] = context.getWorld().getServer().getPlayerManager().respawnPlayer(player, true, Entity.RemovalReason.CHANGED_DIMENSION);
                    context.assertTrue(respawned[0] != null && respawned[0] != player, "Le respawn vanilla cree un nouveau joueur.");
                } else if (now - syncedAt[0] == 40) {
                    try {
                        context.assertTrue(respawned[0].isAlive(), "Le joueur reapparu vit.");
                    } finally {
                        GameTestPlayers.disconnect(respawned[0]);
                        portal.discard();
                    }
                    context.complete();
                }
            });
        }
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 260)
    public void orphanPortalsRemoveThemselvesOnlyWhenTheirDoorIsProvablyGone(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos noDoorHere = context.getAbsolutePos(new BlockPos(3, 1, 3));
        BackroomsPortalEntity doorless = BackroomsPortalEntity.builder(world)
                .position(Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 2, 1))))
                .facingSource(PortalFacing.NORTH).facingDestination(PortalFacing.SOUTH)
                .destinationDim(World.OVERWORLD).destinationPos(Vec3d.ofCenter(noDoorHere))
                .size(1.0f, 2.0f).owner(World.OVERWORLD, noDoorHere).build();
        BackroomsPortalEntity unknownOwner = BackroomsPortalEntity.builder(world)
                .position(Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(3, 2, 1))))
                .facingSource(PortalFacing.NORTH).facingDestination(PortalFacing.SOUTH)
                .destinationDim(World.OVERWORLD).destinationPos(Vec3d.ofCenter(noDoorHere))
                .size(1.0f, 2.0f).owner(nowhere(), noDoorHere).build();
        BackroomsPortalEntity farOwner = BackroomsPortalEntity.builder(world)
                .position(Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(5, 2, 1))))
                .facingSource(PortalFacing.NORTH).facingDestination(PortalFacing.SOUTH)
                .destinationDim(World.OVERWORLD).destinationPos(Vec3d.ofCenter(noDoorHere))
                .size(1.0f, 2.0f).owner(World.OVERWORLD, new BlockPos(1_000_000, 64, 1_000_000)).build();
        world.spawnEntity(doorless);
        world.spawnEntity(unknownOwner);
        world.spawnEntity(farOwner);
        // Un chargement annonce deux fois n'inscrit pas deux fois le portail.
        ServerEntityEvents.ENTITY_LOAD.invoker().onLoad(farOwner, world);
        context.assertTrue(BackroomsPortalEntityRegistry.all().stream().filter(portal -> portal == farOwner).count() == 1,
                "Le registre ne tient chaque portail qu'une fois.");

        context.runAtTick(120, () -> {
            try {
                context.assertTrue(doorless.isRemoved(), "Sans porte a l'emplacement annonce, le portail se retire.");
                context.assertTrue(unknownOwner.isRemoved(), "Dimension de porte inconnue : le portail se retire.");
                context.assertTrue(!farOwner.isRemoved(), "Chunk de la porte non charge : le portail attend.");
            } finally {
                farOwner.discard();
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 60, batchId = "portal_debug_command")
    public void theDebugCommandListsThenClearsLoadedPortals(TestContext context) {
        ServerWorld world = context.getWorld();
        MinecraftServer server = world.getServer();
        BackroomsPortalEntity portal = netherPortal(context);
        BackroomsPortalEntity owned = BackroomsPortalEntity.builder(world)
                .position(Vec3d.ofCenter(context.getAbsolutePos(PORTAL.east(2))))
                .facingSource(PortalFacing.NORTH).facingDestination(PortalFacing.SOUTH)
                .destinationDim(World.NETHER).destinationPos(NETHER_EXIT)
                .size(1.0f, 2.0f).owner(World.OVERWORLD, new BlockPos(1_000_000, 64, 1_000_000)).build();
        world.spawnEntity(owned);
        ServerCommandSource source = server.getCommandSource();
        context.runAtTick(5, () -> {
            context.assertTrue(BackroomsPortalEntityRegistry.all().contains(portal) && BackroomsPortalEntityRegistry.all().contains(owned),
                    "Les deux portails sont charges.");
            server.getCommandManager().executeWithPrefix(source, "backrooms portal list");
            server.getCommandManager().executeWithPrefix(source, "backrooms portal clear");
        });
        context.runAtTick(10, () -> {
            context.assertTrue(portal.isRemoved() && BackroomsPortalEntityRegistry.all().isEmpty(), "Tous les portails sont supprimes.");
            context.complete();
        });
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void remoteDimensionPayloadDescribesTheNether(TestContext context) {
        ServerWorld nether = nether(context);
        RemoteDimensionPayload payload = RemoteDimensionPayload.of(nether);
        context.assertTrue(payload.dimension().equals(World.NETHER.getValue()), "Dimension du Nether.");
        context.assertTrue(payload.dimensionType().equals(Identifier.ofVanilla("the_nether")), "Type de dimension du Nether.");
        context.complete();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 60)
    public void aPlayerWhoNeverLookedAtThePortalCrossesWithoutHeldChunks(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerWorld nether = nether(context);
        // Portail invisible : il ne se regarde pas, aucune synchronisation n'a lieu pour ce joueur avant sa teleportation.
        BackroomsPortalEntity portal = BackroomsPortalEntity.builder(world)
                .position(Vec3d.ofCenter(context.getAbsolutePos(PORTAL))).facingSource(PortalFacing.NORTH).facingDestination(PortalFacing.SOUTH)
                .destinationDim(World.NETHER).destinationPos(NETHER_EXIT).size(1.0f, 2.0f).visible(false).build();
        world.spawnEntity(portal);
        Vec3d plane = portal.getPos();
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.CREATIVE, plane.add(0.0, -1.0, -10.0), 180.0f, 0.0f);
        try {
            player.refreshPositionAndAngles(plane.x, plane.y - 1.0, plane.z + 0.05, 0.0f, 0.0f);
            context.assertTrue(TeleportExecutor.execute(player, portal), "La traversee a lieu.");
            context.assertTrue(player.getServerWorld() == nether, "Le joueur est dans le Nether, sans chunk tenu a epargner.");
        } finally {
            GameTestPlayers.disconnect(player);
            portal.discard();
        }
        context.complete();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 60)
    public void portalsToAnUnknownDimensionTeleportNothing(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d source = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 2, 1)));
        BackroomsPortalEntity broken = BackroomsPortalEntity.builder(world)
                .position(source).facingSource(PortalFacing.NORTH).facingDestination(PortalFacing.SOUTH)
                .destinationDim(nowhere()).destinationPos(new Vec3d(0.5, 65.0, 0.5)).size(1.0f, 2.0f).build();
        world.spawnEntity(broken);
        BackroomsPortalEntity toNether = netherPortal(context);
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.CREATIVE, source.add(0.0, -1.0, 0.05), 0.0f, 0.0f);
        ItemEntity item = new ItemEntity(world, source.x, source.y, source.z + 0.05, new ItemStack(Items.IRON_NUGGET));
        world.spawnEntity(item);
        try {
            context.assertTrue(!TeleportExecutor.execute(player, broken), "Sans dimension d'arrivee, le joueur reste.");
            context.assertTrue(!TeleportExecutor.execute(item, broken), "Sans dimension d'arrivee, l'objet reste.");
            context.assertTrue(player.getServerWorld() == world && item.getWorld() == world, "Rien n'a bouge.");
            // Un objet deja retire ne part nulle part, meme vers une dimension qui existe.
            item.discard();
            context.assertTrue(!TeleportExecutor.execute(item, toNether), "Un objet retire ne se teleporte pas.");
        } finally {
            GameTestPlayers.disconnect(player);
            broken.discard();
            toNether.discard();
        }
        context.complete();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 60)
    public void aCrossingCaughtByTheServerIsDroppedIfThePortalVanishesClosesOrLeadsNowhere(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d destination = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(6, 2, 6)));
        Vec3d[] sources = {
                Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 2, 1))),
                Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 2, 12))),
                Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(12, 2, 1)))};
        BackroomsPortalEntity[] portals = new BackroomsPortalEntity[3];
        ServerPlayerEntity[] walkers = new ServerPlayerEntity[3];
        portals[0] = localPortal(context, sources[0], destination, true);
        portals[1] = localPortal(context, sources[1], destination, true);
        portals[2] = BackroomsPortalEntity.builder(world)
                .position(sources[2]).facingSource(PortalFacing.NORTH).facingDestination(PortalFacing.EAST)
                .destinationDim(nowhere()).destinationPos(destination).size(1.0f, 2.0f).build();
        world.spawnEntity(portals[2]);
        for (int i = 0; i < 3; i++) {
            walkers[i] = GameTestPlayers.connect(context, GameMode.SURVIVAL, new Vec3d(sources[i].x, sources[i].y - 1.0, sources[i].z - 0.3), 0.0f, 0.0f);
        }
        double[] z = {sources[0].z - 0.3, sources[1].z - 0.3, sources[2].z - 0.3};
        for (int tick = 1; tick <= 12; tick++) {
            context.runAtTick(tick, () -> {
                for (int i = 0; i < 3; i++) {
                    z[i] += z[i] < sources[i].z + 0.2 ? 0.2 : 0.01;
                    walkers[i].refreshPositionAndAngles(sources[i].x, sources[i].y - 1.0, z[i], 0.0f, 0.0f);
                }
            });
        }
        // Le serveur a vu les franchissements : avant la fin du delai laisse au client, un portail disparait, un autre se
        // ferme, le troisieme n'a pas de dimension d'arrivee.
        context.runAtTick(4, () -> {
            portals[0].discard();
            portals[1].setTeleportable(false);
        });
        context.runAtTick(20, () -> {
            try {
                for (int i = 0; i < 3; i++) {
                    // Un marcheur ne bouge qu'en Z : teleporte, il aurait rejoint l'abscisse de l'arrivee.
                    context.assertTrue(Math.abs(walkers[i].getX() - sources[i].x) < 1.0, "Le marcheur " + i + " n'a pas ete teleporte.");
                }
            } finally {
                for (int i = 0; i < 3; i++) {
                    GameTestPlayers.disconnect(walkers[i]);
                    portals[i].discard();
                }
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 80)
    public void entitiesThatRideAreRiddenOrWaitDoNotCross(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d source = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(1, 2, 1)));
        Vec3d destination = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(6, 2, 6)));
        BackroomsPortalEntity portal = localPortal(context, source, destination, true);
        // Un objet encore en cooldown de portail, un bateau et son passager, un mouton mort : tous dans la boite de traversee.
        ItemEntity cooling = new ItemEntity(world, source.x, source.y, source.z - 0.3, new ItemStack(Items.DIAMOND));
        cooling.setNoGravity(true);
        cooling.setVelocity(0.0, 0.0, 0.5);
        cooling.setPortalCooldown(200);
        world.spawnEntity(cooling);
        BoatEntity boat = EntityType.BOAT.create(world);
        boat.setNoGravity(true);
        boat.refreshPositionAndAngles(source.x, source.y - 1.0, source.z - 0.3, 0.0f, 0.0f);
        world.spawnEntity(boat);
        SheepEntity passenger = EntityType.SHEEP.create(world);
        passenger.setNoGravity(true);
        passenger.refreshPositionAndAngles(source.x, source.y - 1.0, source.z - 0.3, 0.0f, 0.0f);
        passenger.setAiDisabled(true);
        world.spawnEntity(passenger);
        // A plus d'un bloc du bateau, qui embarque de lui-meme les creatures qui le touchent.
        SheepEntity dead = EntityType.SHEEP.create(world);
        dead.setNoGravity(true);
        dead.refreshPositionAndAngles(source.x + 1.5, source.y - 1.0, source.z - 0.3, 0.0f, 0.0f);
        dead.setAiDisabled(true);
        world.spawnEntity(dead);
        context.runAtTick(1, () -> {
            // Le bateau a pu embarquer le mouton de lui-meme au premier tick : on s'assure seulement qu'il est a bord.
            passenger.startRiding(boat, true);
            context.assertTrue(passenger.getVehicle() == boat, "Le mouton est dans le bateau.");
            dead.kill();
        });
        for (int tick = 2; tick <= 8; tick++) {
            context.runAtTick(tick, () -> {
                boat.refreshPositionAndAngles(boat.getX(), boat.getY(), boat.getZ() + 0.2, 0.0f, 0.0f);
                dead.refreshPositionAndAngles(dead.getX(), dead.getY(), dead.getZ() + 0.2, 0.0f, 0.0f);
            });
        }
        context.runAtTick(12, () -> {
            try {
                context.assertTrue(cooling.getX() < destination.x, "En cooldown, l'objet ne traverse pas.");
                context.assertTrue(boat.getX() < destination.x && passenger.getX() < destination.x, "Bateau et passager restent ensemble, de ce cote.");
                context.assertTrue(dead.getX() < destination.x, "Un mouton mort ne traverse pas.");
            } finally {
                passenger.stopRiding();
                cooling.discard();
                boat.discard();
                passenger.discard();
                dead.discard();
                portal.discard();
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 2200)
    public void approachingAWatchedPortalWidensItsArrivalTicket(TestContext context) {
        ServerWorld nether = nether(context);
        pregenerateArrival(nether, NETHER_EXIT);
        BackroomsPortalEntity portal = netherPortal(context);
        Vec3d plane = portal.getPos();
        // Un objet a l'arrivee : suivi pour le joueur, puis oublie sans paquet quand il se deconnecte en regardant encore le portail.
        ItemEntity watched = new ItemEntity(nether, NETHER_EXIT.x, NETHER_EXIT.y, NETHER_EXIT.z, new ItemStack(Items.QUARTZ));
        watched.setNoGravity(true);
        nether.spawnEntity(watched);
        // Douze blocs au nord : le portail est regarde de loin, son arrivee est chargee sur la fenetre synchronisee.
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.CREATIVE, new Vec3d(plane.x, plane.y - 1.0, plane.z - 12.0), 0.0f, 0.0f);
        int farChunkX = ChunkSectionPos.getSectionCoord(NETHER_EXIT.x) + 4;
        int farChunkZ = ChunkSectionPos.getSectionCoord(NETHER_EXIT.z);
        context.runAtTick(30, () -> {
            // A trois blocs, la traversee est imminente : le ticket s'elargit a la distance de vue du serveur.
            player.refreshPositionAndAngles(plane.x, plane.y - 1.0, plane.z - 3.0, 0.0f, 0.0f);
            player.networkHandler.syncWithPlayerPosition();
        });
        boolean[] done = {false};
        for (int tick = 40; tick < 2190; tick++) {
            int now = tick;
            context.runAtTick(tick, () -> {
                if (done[0]) {
                    return;
                }
                if (nether.getChunkManager().isChunkLoaded(farChunkX, farChunkZ)) {
                    done[0] = true;
                    GameTestPlayers.disconnect(player);
                    watched.discard();
                    portal.discard();
                    context.complete();
                } else if (now > SYNC_PATIENCE_TICKS) {
                    throw new GameTestException("Un chunk a quatre chunks de l'arrivee n'est pas charge par le ticket elargi.");
                }
            });
        }
    }
}
