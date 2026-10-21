package com.gtgpuke.gtgsbackrooms.gametest.core.portal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.HorseEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.AfterBatch;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;

import com.gtgpuke.gtgsbackrooms.core.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.core.portal.sync.PortalTeleportPayload;
import com.gtgpuke.gtgsbackrooms.gametest.support.GameTestPlayers;
import com.gtgpuke.gtgsbackrooms.gametest.support.GameTestServerWorlds;

/**
 * Tests en jeu des attelages qui traversent un portail : un bateau emmene ses animaux et ses joueurs (vers le Nether,
 * dimension distante du serveur de test, ou dans le meme monde), un cheval monte dans un bateau garde son cavalier,
 * et rien ne part vers une dimension absente. Les bateaux menes par un joueur sont deplaces a la main entre deux
 * ticks, comme les paquets d'un client le font ; ceux sans joueur avancent par leur propre vitesse. Les tests d'un
 * meme lot tournent en meme temps : chacun a son arrivee dans le Nether et ne touche pas aux joueurs des autres.
 */
public final class PortalRidingGameTests implements FabricGameTest {

    private static final String BATCH = "portal_riding";
    private static final double EPSILON = 1.0e-3;
    /** Pas d'un bateau mene par un joueur, par tick. */
    private static final double RIDDEN_STEP = 0.25;
    /** Vitesse donnee a chaque tick a un bateau sans joueur (le sol en retient la moitie). */
    private static final double FREE_SPEED = 0.3;
    private static final Vec3d PORTAL = new Vec3d(6.5, 2.0, 6.5);
    private static final Vec3d LOCAL_EXIT = new Vec3d(16.5, 2.0, 6.5);
    /** Arrivees dans le Nether, au-dessus de son toit (rien n'y gene un bateau sans gravite), une par test, a quatre blocs l'une de l'autre. */
    private static final Vec3d ANIMAL_EXIT = new Vec3d(8.5, 200.0, 8.5);
    private static final Vec3d RIDER_EXIT = new Vec3d(12.5, 200.0, 8.5);
    private static final Vec3d HORSE_EXIT = new Vec3d(4.5, 200.0, 8.5);
    private static final int BIG_JUMP = 10;
    /** Ce que les tests laissent derriere eux si une attente expire : retire apres le lot, reussi ou non. */
    private static final List<Entity> LEFTOVERS = new ArrayList<>();

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 200, batchId = BATCH)
    public void aBoatCarriesItsAnimalThroughThePortal(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerWorld nether = prepareNether(context, ANIMAL_EXIT);
        Vec3d source = context.getAbsolute(PORTAL);
        BackroomsPortalEntity portal = GameTestServerWorlds.spawnNorthPortal(world, source, World.NETHER, ANIMAL_EXIT);
        BoatEntity boat = boat(world, source.add(0.0, -1.0, -1.5));
        CowEntity cow = cow(world, boat);
        LEFTOVERS.add(portal);
        context.createTimedTaskRunner()
                .createAndAdd(() -> context.assertTrue(nether.shouldTickEntity(BlockPos.ofFloored(ANIMAL_EXIT)), "Le chunk d'arrivee ne suit pas encore ses entities."))
                .createAndAdd(() -> {
                    if (boat.getWorld() == world && !boat.isRemoved()) {
                        boat.setVelocity(0.0, 0.0, FREE_SPEED);
                    }
                    context.assertTrue(nether.getEntity(boat.getUuid()) != null, "Le bateau n'est pas encore passe : " + boat.getPos() + ".");
                })
                .createAndAddReported(() -> {
                    Entity copy = nether.getEntity(boat.getUuid());
                    Entity cowCopy = nether.getEntity(cow.getUuid());
                    LEFTOVERS.add(copy);
                    LEFTOVERS.add(cowCopy);
                    context.assertTrue(boat.isRemoved() && cow.isRemoved(), "De ce cote, le bateau et la vache sont partis.");
                    context.assertTrue(copy instanceof BoatEntity && cowCopy != null && cowCopy.getVehicle() == copy && copy.getFirstPassenger() == cowCopy,
                            "Dans le Nether, la vache est toujours dans le bateau.");
                    context.assertTrue(Math.abs(copy.getX() - ANIMAL_EXIT.x) < 0.5 && Math.abs(copy.getY() - (ANIMAL_EXIT.y - 1.0)) < EPSILON
                            && copy.getZ() > ANIMAL_EXIT.z && copy.getZ() < ANIMAL_EXIT.z + 1.0, "Le bateau ressort devant l'arrivee : " + copy.getPos() + ".");
                    context.assertTrue(copy.hasPortalCooldown() && copy.getVelocity().z > 0.0, "Cooldown pose, vitesse transposee vers le sud.");
                })
                .completeIfSuccessful();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 300, batchId = BATCH)
    public void aRiddenBoatCrossesWithItsRiderAndItsCow(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerWorld nether = prepareNether(context, RIDER_EXIT);
        Vec3d source = context.getAbsolute(PORTAL);
        BackroomsPortalEntity portal = GameTestServerWorlds.spawnNorthPortal(world, source, World.NETHER, RIDER_EXIT);
        BoatEntity boat = boat(world, source.add(0.0, -1.0, -1.5));
        CowEntity cow = cow(world, boat);
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.CREATIVE, boat.getPos(), 0.0f, 0.0f);
        context.assertTrue(player.startRiding(boat, true), "Le joueur monte dans le bateau.");
        LEFTOVERS.add(portal);
        boolean[] mounted = {false};
        int[] moved = {0};
        context.createTimedTaskRunner()
                .createAndAdd(() -> context.assertTrue(nether.shouldTickEntity(BlockPos.ofFloored(RIDER_EXIT)), "Le chunk d'arrivee ne suit pas encore ses entities."))
                // Deux ticks sans bouger (premiere position, puis position inchangee), puis le bateau avance comme le ferait son client.
                .createAndAdd(() -> {
                    if (moved[0]++ >= 2 && player.getServerWorld() == world) {
                        boat.setPosition(boat.getX(), boat.getY(), boat.getZ() + RIDDEN_STEP);
                    }
                    context.assertTrue(player.getServerWorld() == nether, "Le joueur n'est pas encore passe : " + boat.getPos() + ".");
                })
                .createAndAddReported(() -> {
                    Entity copy = nether.getEntity(boat.getUuid());
                    Entity cowCopy = nether.getEntity(cow.getUuid());
                    LEFTOVERS.add(copy);
                    LEFTOVERS.add(cowCopy);
                    context.assertTrue(copy instanceof BoatEntity && player.getVehicle() == copy, "Dans le Nether, le joueur est toujours dans le bateau.");
                    context.assertTrue(cowCopy != null && cowCopy.getVehicle() == copy, "La vache aussi.");
                    context.assertTrue(!GameTestPlayers.sent(player, PortalTeleportPayload.class).isEmpty(), "Le serveur a mene la bascule du joueur.");
                })
                // Le faux client recoit ses chunks, suit le bateau, et apprend qu'il en est passager ; le bateau avance encore
                // sous le cooldown du joueur, sans rien declencher.
                .createAndAdd(() -> {
                    GameTestPlayers.tick(player);
                    nether.getChunkManager().updatePosition(player);
                    Entity copy = player.getVehicle();
                    // Une exception autre qu'un echec de test ferait planter tout le serveur de test.
                    context.assertTrue(copy != null, "Le joueur n'est plus dans le bateau.");
                    copy.setPosition(copy.getX(), copy.getY(), copy.getZ() + RIDDEN_STEP);
                    for (BundleS2CPacket bundle : GameTestPlayers.sent(player, BundleS2CPacket.class)) {
                        for (Packet<?> packet : bundle.getPackets()) {
                            mounted[0] |= packet instanceof EntityPassengersSetS2CPacket passengers && passengers.getEntityId() == copy.getId()
                                    && contains(passengers.getPassengerIds(), player.getId());
                        }
                    }
                    context.assertTrue(mounted[0], "Le joueur n'a pas encore recu sa place dans le bateau.");
                })
                .createAndAddReported(() -> context.assertTrue(player.getServerWorld() == nether && player.getVehicle() != null, "Le joueur navigue toujours dans le Nether."))
                .completeIfSuccessful();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 200, batchId = BATCH)
    public void aRiddenBoatCrossesWithinTheWorld(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d source = context.getAbsolute(PORTAL);
        Vec3d exit = context.getAbsolute(LOCAL_EXIT);
        BackroomsPortalEntity portal = GameTestServerWorlds.spawnNorthPortal(world, source, World.OVERWORLD, exit);
        BoatEntity boat = boat(world, source.add(0.0, -1.0, -1.5));
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.CREATIVE, boat.getPos(), 0.0f, 0.0f);
        context.assertTrue(player.startRiding(boat, true), "Le joueur monte dans le bateau.");
        LEFTOVERS.add(portal);
        context.createTimedTaskRunner()
                // Tant que le bateau n'a pas saute a l'arrivee, ce que la montee du depart a envoye au client est oublie ; les
                // paquets de la traversee, eux, partent au bout du tick ou le bateau atteint le plan et attendent la verification.
                .createAndAdd(() -> {
                    boolean crossed = boat.getX() > source.x + 5.0;
                    if (!crossed) {
                        GameTestPlayers.sent(player, EntityPassengersSetS2CPacket.class);
                        GameTestPlayers.sent(player, VehicleMoveS2CPacket.class);
                        boat.setPosition(boat.getX(), boat.getY(), boat.getZ() + RIDDEN_STEP);
                    }
                    context.assertTrue(crossed || boat.getZ() > source.z + 0.5, "Le bateau n'est pas encore passe : " + boat.getPos() + ".");
                })
                .createAndAddReported(() -> {
                    context.assertTrue(player.getVehicle() == boat && !boat.isRemoved() && player.getServerWorld() == world,
                            "Dans le meme monde, le joueur reste dans le meme bateau.");
                    context.assertTrue(Math.abs(boat.getX() - exit.x) < EPSILON && boat.getZ() < exit.z + 1.0, "Le bateau ressort devant l'arrivee : " + boat.getPos() + ".");
                    context.assertTrue(GameTestPlayers.sent(player, EntityPassengersSetS2CPacket.class).stream()
                                    .anyMatch(packet -> packet.getEntityId() == boat.getId() && contains(packet.getPassengerIds(), player.getId())),
                            "Le client du joueur, descendu par le serveur, recoit sa place dans le bateau.");
                    context.assertTrue(GameTestPlayers.sent(player, VehicleMoveS2CPacket.class).stream()
                                    .anyMatch(packet -> Math.abs(packet.getX() - boat.getX()) < EPSILON && Math.abs(packet.getZ() - boat.getZ()) < EPSILON),
                            "Et la position de son bateau de l'autre cote.");
                })
                .completeIfSuccessful();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 300, batchId = BATCH)
    public void aRiderOnAHorseInABoatKeepsTheHorse(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerWorld nether = prepareNether(context, HORSE_EXIT);
        Vec3d source = context.getAbsolute(PORTAL);
        BackroomsPortalEntity portal = GameTestServerWorlds.spawnNorthPortal(world, source, World.NETHER, HORSE_EXIT);
        BoatEntity boat = boat(world, source.add(0.0, -1.0, -1.5));
        HorseEntity horse = EntityType.HORSE.create(world);
        horse.refreshPositionAndAngles(boat.getX(), boat.getY(), boat.getZ(), 0.0f, 0.0f);
        horse.setAiDisabled(true);
        world.spawnEntity(horse);
        LEFTOVERS.add(horse);
        context.assertTrue(horse.startRiding(boat, true), "Le cheval monte dans le bateau.");
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.CREATIVE, boat.getPos(), 0.0f, 0.0f);
        context.assertTrue(player.startRiding(horse, true), "Le joueur monte sur le cheval.");
        LEFTOVERS.add(portal);
        context.createTimedTaskRunner()
                .createAndAdd(() -> context.assertTrue(nether.shouldTickEntity(BlockPos.ofFloored(HORSE_EXIT)), "Le chunk d'arrivee ne suit pas encore ses entities."))
                .createAndAdd(() -> {
                    if (player.getServerWorld() == world) {
                        boat.setPosition(boat.getX(), boat.getY(), boat.getZ() + RIDDEN_STEP);
                    }
                    context.assertTrue(player.getServerWorld() == nether, "Le joueur n'est pas encore passe : " + boat.getPos() + ".");
                })
                .createAndAddReported(() -> {
                    Entity boatCopy = nether.getEntity(boat.getUuid());
                    Entity horseCopy = nether.getEntity(horse.getUuid());
                    LEFTOVERS.add(boatCopy);
                    LEFTOVERS.add(horseCopy);
                    context.assertTrue(horseCopy instanceof HorseEntity && player.getVehicle() == horseCopy, "Dans le Nether, le joueur est toujours sur son cheval.");
                    context.assertTrue(boatCopy instanceof BoatEntity && horseCopy.getVehicle() == boatCopy, "Et le cheval toujours dans le bateau.");
                })
                .completeIfSuccessful();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 200, batchId = BATCH)
    public void aRiddenBoatTowardsAnUnknownDimensionStaysWithItsRider(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d source = context.getAbsolute(PORTAL);
        BackroomsPortalEntity broken = GameTestServerWorlds.spawnNorthPortal(world, source, GameTestServerWorlds.nowhere(), ANIMAL_EXIT);
        Vec3d start = source.add(0.0, -1.0, -1.5);
        BoatEntity first = boat(world, start.add(3.0, 0.0, 0.0));
        BoatEntity boat = boat(world, start);
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.CREATIVE, first.getPos(), 0.0f, 0.0f);
        context.assertTrue(player.startRiding(first, true), "Le joueur monte dans le premier bateau.");
        LEFTOVERS.add(broken);
        int[] tick = {0};
        context.createTimedTaskRunner()
                // Un changement de bateau, un saut de position aller puis retour, un tick immobile : aucun n'est un trajet.
                .createAndAdd(() -> {
                    switch (tick[0]++) {
                        case 1 -> context.assertTrue(player.startRiding(boat, true), "Le joueur change de bateau.");
                        case 2 -> boat.setPosition(start.x, start.y, start.z - BIG_JUMP);
                        case 3 -> boat.setPosition(start.x, start.y, start.z);
                        case 4 -> { }
                        default -> boat.setPosition(boat.getX(), boat.getY(), boat.getZ() + RIDDEN_STEP);
                    }
                    context.assertTrue(boat.getZ() > source.z + 0.5, "Le bateau n'a pas encore passe le plan : " + boat.getPos() + ".");
                })
                .createAndAddReported(() -> context.assertTrue(player.getVehicle() == boat && player.getServerWorld() == world && !boat.isRemoved()
                                && Math.abs(boat.getX() - start.x) < EPSILON,
                        "Sans dimension d'arrivee, le bateau passe le plan comme de l'air et le joueur y reste assis : " + boat.getPos() + "."))
                .completeIfSuccessful();
    }

    /** Apres le lot, reussi ou non : joueurs deconnectes, bateaux, animaux et portails retires des deux mondes, chunks liberes. */
    @AfterBatch(batchId = BATCH)
    public void afterRiding(ServerWorld world) {
        GameTestPlayers.disconnectAll();
        ServerWorld nether = world.getServer().getWorld(World.NETHER);
        for (Entity entity : LEFTOVERS) {
            if (entity != null) {
                GameTestServerWorlds.discardEverywhere(nether, entity);
                GameTestServerWorlds.discardEverywhere(world, entity);
            }
        }
        LEFTOVERS.clear();
        GameTestServerWorlds.releaseForcedChunks(nether);
    }

    /** Le Nether genere d'avance autour de l'arrivee, son chunk force : il tient ses entities sans joueur. */
    private static ServerWorld prepareNether(TestContext context, Vec3d exit) {
        ServerWorld nether = GameTestServerWorlds.nether(context);
        GameTestServerWorlds.pregenerateArrival(nether, exit);
        GameTestServerWorlds.forceChunk(nether, exit, true);
        return nether;
    }

    /** Un bateau sans gravite (il flotte au-dessus du toit du Nether), pose a cet endroit. */
    private static BoatEntity boat(ServerWorld world, Vec3d position) {
        BoatEntity boat = new BoatEntity(world, position.x, position.y, position.z);
        boat.setNoGravity(true);
        world.spawnEntity(boat);
        LEFTOVERS.add(boat);
        return boat;
    }

    /** Une vache sans cervelle, assise dans ce bateau. */
    private static CowEntity cow(ServerWorld world, BoatEntity boat) {
        CowEntity cow = EntityType.COW.create(world);
        cow.refreshPositionAndAngles(boat.getX(), boat.getY(), boat.getZ(), 0.0f, 0.0f);
        cow.setAiDisabled(true);
        world.spawnEntity(cow);
        LEFTOVERS.add(cow);
        cow.startRiding(boat, true);
        return cow;
    }

    private static boolean contains(int[] ids, int id) {
        for (int candidate : ids) {
            if (candidate == id) {
                return true;
            }
        }
        return false;
    }
}
