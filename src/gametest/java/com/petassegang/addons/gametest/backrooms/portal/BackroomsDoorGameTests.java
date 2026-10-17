package com.petassegang.addons.gametest.backrooms.portal;

import java.util.UUID;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.AfterBatch;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;

import com.petassegang.addons.backrooms.BackroomsConstants;
import com.petassegang.addons.backrooms.portal.block.BackroomsDoorBlock;
import com.petassegang.addons.backrooms.portal.block.BackroomsDoorBlockEntity;
import com.petassegang.addons.backrooms.portal.block.BackroomsDoorTargets;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.gametest.support.GameTestPlayers;
import com.petassegang.addons.gametest.support.GameTestServerWorlds;
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.init.ModEntities;
import com.petassegang.addons.init.ModItems;

/**
 * Tests en jeu de la porte des Backrooms : portail d'entree pose avec la destination et ouvert ou ferme avec la
 * porte, portail de retour pose et retire avec elle, pose face au joueur, cibles par dimension, et cas limites (sans
 * destination, dimension absente, portail d'entree perdu, porte tournee, portail de retour disparu ou jamais pose,
 * demi-porte heritee, Level 0 prete au serveur de test).
 */
public final class BackroomsDoorGameTests implements FabricGameTest {

    private static final BlockPos DOOR = new BlockPos(1, 1, 1);
    private static final BlockPos ARRIVAL = new BlockPos(1, 1, 8);
    private static final double EPSILON = 1.0e-3;

    /** Pose une porte complete (deux moities, gond a gauche) sur une pierre et renvoie la block entity de sa moitie basse. */
    private static BackroomsDoorBlockEntity placeDoor(TestContext context, BlockPos lower, Direction facing, boolean open) {
        context.setBlockState(lower.down(), Blocks.STONE.getDefaultState());
        BlockState state = ModBlocks.BACKROOMS_DOOR.getDefaultState().with(DoorBlock.FACING, facing).with(DoorBlock.OPEN, open);
        context.setBlockState(lower, state.with(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        context.setBlockState(lower.up(), state.with(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        return context.getBlockEntity(lower);
    }

    /** Retire les deux moities de la porte et la pierre sous elle. */
    private static void removeDoor(TestContext context, BlockPos lower) {
        context.setBlockState(lower, Blocks.AIR.getDefaultState());
        context.setBlockState(lower.up(), Blocks.AIR.getDefaultState());
        context.setBlockState(lower.down(), Blocks.AIR.getDefaultState());
    }

    /** Ouvre ou ferme la porte comme un clic du joueur : l'autre moitie suit. */
    private static void setOpen(TestContext context, BlockPos lower, boolean open) {
        ModBlocks.BACKROOMS_DOOR.setOpen(null, context.getWorld(), context.getBlockState(lower), context.getAbsolutePos(lower), open);
    }

    private static Vec3d doorwayCenter(TestContext context, BlockPos lower) {
        return Vec3d.ofCenter(context.getAbsolutePos(lower)).add(0.0, 0.5, 0.0);
    }

    @GameTest(templateName = GameTestPlayers.ARENA)
    public void overworldArrivalsRestOnTheRealFloor(TestContext context) {
        ServerWorld world = context.getWorld();
        MinecraftServer server = world.getServer();
        BlockPos slab = context.getAbsolutePos(new BlockPos(5, 0, 5));
        BlockPos carpet = context.getAbsolutePos(new BlockPos(7, 0, 5));
        BlockPos water = context.getAbsolutePos(new BlockPos(9, 0, 5));
        BlockPos plain = context.getAbsolutePos(new BlockPos(11, 0, 5));
        world.setBlockState(slab, Blocks.STONE_SLAB.getDefaultState());
        world.setBlockState(carpet, Blocks.WHITE_CARPET.getDefaultState());
        world.setBlockState(water, Blocks.WATER.getDefaultState());
        try {
            // L'arene de test a un plafond de barrieres que la heightmap voit : on donne au calcul le premier Y libre de chaque colonne.
            assertFloor(context, world, slab, slab.getY() + 1, slab.getY() + 0.5, "Sur une dalle, le cadre se pose sur la dalle, au demi-bloc.");
            assertFloor(context, world, carpet, carpet.getY() + 1, carpet.getY() + 1.0 / 16.0, "Sur un tapis compte dans la heightmap, le cadre se pose dessus.");
            assertFloor(context, world, carpet, carpet.getY(), carpet.getY() + 1.0 / 16.0, "Sur un tapis absent de la heightmap aussi.");
            assertFloor(context, world, water, water.getY() + 1, water.getY() + 1.0, "Sur l'eau, rien a fouler : le cadre se pose a sa surface.");
            assertFloor(context, world, plain, plain.getY(), plain.getY(), "Sur un bloc plein, le cadre se pose sur le bloc.");
            // Une porte du Level 0 pose le centre du cadre un bloc au-dessus du sol que voit la heightmap, a l'aplomb de la
            // colonne : ici le plafond de barrieres de l'arene, un bloc plein.
            BackroomsDoorTargets.Target target = BackroomsDoorTargets.resolve(server, BackroomsConstants.LEVEL_ZERO_WORLD, slab, Direction.NORTH);
            int surface = world.getTopY(Heightmap.Type.MOTION_BLOCKING, slab.getX(), slab.getZ());
            context.assertTrue(target != null && Math.abs(target.position().y - (surface + 1.0)) < EPSILON,
                    "Le centre d'arrivee est un bloc au-dessus du sol vu par la heightmap.");
        } finally {
            world.setBlockState(slab, Blocks.AIR.getDefaultState());
            world.setBlockState(carpet, Blocks.AIR.getDefaultState());
            world.setBlockState(water, Blocks.AIR.getDefaultState());
        }
        context.complete();
    }

    private static void assertFloor(TestContext context, ServerWorld world, BlockPos column, int surfaceY, double floor, String message) {
        double actual = BackroomsDoorTargets.floorHeight(world, column.getX(), surfaceY, column.getZ());
        context.assertTrue(Math.abs(actual - floor) < EPSILON, message + " Attendu " + floor + ", obtenu " + actual + ".");
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 100)
    public void doorKeepsItsEntryPortalAndOnlyOpensItWithTheDoor(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos doorPos = context.getAbsolutePos(DOOR);
        Vec3d arrival = Vec3d.ofCenter(context.getAbsolutePos(ARRIVAL)).add(0.0, 0.5, 0.0);
        BackroomsDoorBlockEntity door = placeDoor(context, DOOR, Direction.NORTH, false);
        door.configureTarget(World.OVERWORLD, arrival, PortalFacing.SOUTH);
        UUID[] portals = new UUID[2];

        context.runAtTick(5, () -> {
            portals[0] = door.attachedPortalUuid();
            portals[1] = door.returnPortalUuid();
            context.assertTrue(portals[0] != null && portals[1] != null, "La porte a pose ses deux portails, fermee ou non.");
            BackroomsPortalEntity entry = (BackroomsPortalEntity) world.getEntity(portals[0]);
            BackroomsPortalEntity back = (BackroomsPortalEntity) world.getEntity(portals[1]);
            context.assertTrue(entry != null && back != null, "Les deux portails sont charges.");
            context.assertTrue(!entry.teleportable() && entry.visible(), "Porte fermee : le portail d'entree ne se traverse pas mais reste synchronise.");
            context.assertTrue(!back.teleportable(), "Porte fermee : le portail de retour ne ramene personne dans le battant.");
            context.assertTrue(!entry.isShown() && back.isShown(),
                    "Porte fermee : le battant cache le portail d'entree ; le retour, sans porte a son emplacement, reste visible.");
            context.assertTrue(entry.getPos().isInRange(doorwayCenter(context, DOOR), EPSILON), "Le portail d'entree remplit l'encadrement : " + entry.getPos() + ".");
            context.assertTrue(entry.facingSource() == PortalFacing.SOUTH && entry.facingDestination() == PortalFacing.SOUTH,
                    "On entre par le cote d'ou la porte a ete posee et on sort par le portail de retour.");
            context.assertTrue(back.facingSource() == PortalFacing.SOUTH && back.facingDestination() == PortalFacing.SOUTH,
                    "Le portail de retour ramene devant la porte, du cote de l'entree.");
            context.assertTrue(back.getPos().isInRange(arrival, EPSILON), "Le portail de retour est au point d'arrivee.");
            context.assertTrue(entry.destinationPos().isInRange(back.getPos(), EPSILON), "Le portail d'entree mene au portail de retour.");
            context.assertTrue(back.destinationPos().equals(entry.getPos()),
                    "Le retour ramene dans le plan meme de la porte, inverse exact de l'entree : " + back.destinationPos() + ".");
            // Un retour qui ne mene pas au plan de la porte y est ramene au tick suivant.
            back.setDestinationPos(entry.getPos().add(0.0, 0.0, 0.55));
            context.assertTrue(doorPos.equals(entry.owner().pos()) && doorPos.equals(back.owner().pos()), "Les deux portails appartiennent a la porte.");
            context.assertTrue(entry.signedDistance(Vec3d.ofCenter(doorPos.south(2))) > 0.0, "Le cote d'entree du portail est devant la porte.");
            setOpen(context, DOOR, true);
        });
        context.runAtTick(10, () -> {
            context.assertTrue(world.getEntity(portals[0]) instanceof BackroomsPortalEntity entry && entry.teleportable() && entry.isShown(),
                    "Porte ouverte : le portail d'entree se voit et se traverse.");
            context.assertTrue(world.getEntity(portals[1]) instanceof BackroomsPortalEntity back && back.teleportable()
                    && back.destinationPos().equals(world.getEntity(portals[0]).getPos()),
                    "Le retour se traverse aussi, et un retour decale est ramene sur le plan de la porte.");
            setOpen(context, DOOR, false);
        });
        context.runAtTick(15, () -> {
            context.assertTrue(world.getEntity(portals[0]) instanceof BackroomsPortalEntity entry && !entry.teleportable(),
                    "Porte refermee : le portail d'entree se referme sans disparaitre.");
            context.assertTrue(world.getEntity(portals[1]) instanceof BackroomsPortalEntity back && !back.teleportable(),
                    "Le portail de retour reste aussi, referme.");
            // Porte tournee vers l'est : on y entre desormais par l'ouest.
            context.setBlockState(DOOR, context.getBlockState(DOOR).with(DoorBlock.FACING, Direction.EAST));
        });
        context.runAtTick(18, () -> {
            context.assertTrue(world.getEntity(portals[0]) == null && world.getEntity(portals[1]) == null,
                    "Les portails de l'ancienne orientation sont retires.");
            portals[0] = door.attachedPortalUuid();
            portals[1] = door.returnPortalUuid();
            context.assertTrue(world.getEntity(portals[0]) instanceof BackroomsPortalEntity entry && entry.facingSource() == PortalFacing.WEST
                    && entry.getPos().isInRange(doorwayCenter(context, DOOR), EPSILON),
                    "Le portail d'entree est repose dans l'encadrement, entree par l'ouest.");
            context.assertTrue(world.getEntity(portals[1]) instanceof BackroomsPortalEntity back && back.facingDestination() == PortalFacing.WEST,
                    "Le retour est repose : il ramene devant la porte, du nouveau cote d'entree.");
            context.setBlockState(DOOR, Blocks.AIR);
        });
        context.runAtTick(22, () -> {
            try {
                context.assertTrue(world.getEntity(portals[0]) == null && world.getEntity(portals[1]) == null, "Casser la porte retire ses deux portails.");
                context.assertTrue(world.getBlockState(doorPos.up()).isAir(), "La moitie haute tombe avec la moitie basse.");
                // Boites serrees : les autres tests de portail tournent a quelques blocs de la, dans le meme monde.
                context.assertTrue(world.getEntitiesByType(ModEntities.BACKROOMS_PORTAL, new Box(doorPos).expand(3.0), entity -> true).isEmpty()
                        && world.getEntitiesByType(ModEntities.BACKROOMS_PORTAL, new Box(BlockPos.ofFloored(arrival)).expand(3.0), entity -> true).isEmpty(),
                        "Aucun portail ne survit a la porte, ni dans l'encadrement ni a l'arrivee.");
            } finally {
                removeDoor(context, DOOR);
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 150)
    public void doorPlacementFollowsThePlayerAndTargetsResolveByDimension(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos floor = context.getAbsolutePos(new BlockPos(2, 0, 2));
        BlockPos doorPos = floor.up();
        world.setBlockState(floor, Blocks.STONE.getDefaultState());
        // Le joueur regarde le nord : la porte s'oriente comme son regard et on y entre par le sud, de son cote.
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.CREATIVE, context.getAbsolute(new Vec3d(2.5, 1.0, 5.5)), 180.0f, 0.0f);
        try {
            ItemStack stack = new ItemStack(ModItems.BACKROOMS_DOOR);
            player.setStackInHand(Hand.MAIN_HAND, stack);
            ItemPlacementContext placement = new ItemPlacementContext(player, Hand.MAIN_HAND, stack,
                    new BlockHitResult(Vec3d.ofCenter(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false));
            context.assertTrue(ModItems.BACKROOMS_DOOR.place(placement).isAccepted(), "La porte se pose.");
            BlockState lower = world.getBlockState(doorPos);
            BlockState upper = world.getBlockState(doorPos.up());
            context.assertTrue(lower.isOf(ModBlocks.BACKROOMS_DOOR) && lower.get(DoorBlock.FACING) == Direction.NORTH
                    && lower.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER && !lower.get(DoorBlock.OPEN), "La moitie basse regarde le nord, fermee.");
            context.assertTrue(upper.isOf(ModBlocks.BACKROOMS_DOOR) && upper.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER, "La moitie haute est posee avec elle.");
            context.assertTrue(BackroomsDoorBlock.entrySide(lower) == Direction.SOUTH, "On entre par le cote du joueur.");
            context.assertTrue(world.getBlockEntity(doorPos) instanceof BackroomsDoorBlockEntity && world.getBlockEntity(doorPos.up()) == null,
                    "Seule la moitie basse porte la block entity.");

            // Une porte du Level 0 mene a la surface de l'overworld ; une porte de l'overworld cherche le Level 0, absent ici.
            BackroomsDoorTargets.Target surface = BackroomsDoorTargets.resolve(world.getServer(), BackroomsConstants.LEVEL_ZERO_WORLD, doorPos, Direction.NORTH);
            context.assertTrue(surface != null && surface.dimension().equals(World.OVERWORLD) && surface.facing() == PortalFacing.NORTH,
                    "Depuis le Level 0, la cible est la surface de l'overworld.");
            context.assertTrue(surface.position().x == doorPos.getX() + 0.5 && surface.position().z == doorPos.getZ() + 0.5,
                    "La cible est a l'aplomb de la porte.");
            context.assertTrue(BackroomsDoorTargets.resolve(world.getServer(), World.OVERWORLD, doorPos, Direction.NORTH) == null,
                    "Sans Level 0 sur ce serveur, une porte de l'overworld n'a pas de cible.");
        } finally {
            GameTestPlayers.disconnect(player);
        }
        // La porte posee reste sans destination : elle reessaie periodiquement, sans portail, meme ouverte.
        context.runAtTick(2, () -> ModBlocks.BACKROOMS_DOOR.setOpen(null, world, world.getBlockState(doorPos), doorPos, true));
        context.runAtTick(BackroomsDoorBlockEntity.TARGET_RETRY_INTERVAL_TICKS + 10, () -> {
            BackroomsDoorBlockEntity entity = (BackroomsDoorBlockEntity) world.getBlockEntity(doorPos);
            context.assertTrue(entity != null && entity.isOpen() && !entity.isConfigured() && entity.attachedPortalUuid() == null,
                    "Sans destination, pas de portail, meme porte ouverte.");
            world.setBlockState(doorPos, Blocks.AIR.getDefaultState());
            world.setBlockState(doorPos.up(), Blocks.AIR.getDefaultState());
            world.setBlockState(floor, Blocks.AIR.getDefaultState());
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA,
            tickLimit = BackroomsDoorBlockEntity.RETURN_CHECK_INTERVAL_TICKS * BackroomsDoorBlockEntity.RETURN_MISSING_CHECKS + 60)
    public void doorWithAMissingDestinationDimensionOnlyKeepsItsEntryPortal(TestContext context) {
        ServerWorld world = context.getWorld();
        BackroomsDoorBlockEntity door = placeDoor(context, DOOR, Direction.NORTH, true);
        door.configureTarget(GameTestServerWorlds.nowhere(), new Vec3d(0.5, 65.0, 0.5), PortalFacing.SOUTH);
        // Une seconde porte dont l'arrivee est a cent mille blocs : son chunk ne tique pas, la verification ne tranche pas.
        // Son portail de retour reste dans ce chunk decharge, hors d'atteinte : il se retirera seul, porte disparue, au
        // prochain chargement du chunk.
        BlockPos farDoorPos = new BlockPos(4, 1, 1);
        BackroomsDoorBlockEntity farDoor = placeDoor(context, farDoorPos, Direction.NORTH, true);
        farDoor.configureTarget(World.OVERWORLD, new Vec3d(100_000.5, 70.0, 100_000.5), PortalFacing.SOUTH);
        UUID[] farReturn = new UUID[1];
        context.runAtTick(5, () -> {
            context.assertTrue(door.attachedPortalUuid() != null && world.getEntity(door.attachedPortalUuid()) != null, "Le portail d'entree existe.");
            context.assertTrue(door.returnPortalUuid() == null, "Sans dimension d'arrivee, pas de portail de retour.");
            farReturn[0] = farDoor.returnPortalUuid();
            context.assertTrue(farReturn[0] != null, "Le portail de retour lointain a ete pose dans son chunk charge a la demande.");
        });
        // Assez de verifications pour qu'un retour manquant soit repose, si la porte en concluait quelque chose.
        int checked = BackroomsDoorBlockEntity.RETURN_CHECK_INTERVAL_TICKS * BackroomsDoorBlockEntity.RETURN_MISSING_CHECKS + 5;
        context.runAtTick(checked, () -> {
            context.assertTrue(door.returnPortalUuid() == null, "Les verifications periodiques ne posent rien sans dimension d'arrivee.");
            context.assertTrue(farReturn[0].equals(farDoor.returnPortalUuid()), "Chunk d'arrivee inactif : la porte n'en conclut rien.");
            removeDoor(context, DOOR);
            removeDoor(context, farDoorPos);
        });
        context.runAtTick(checked + 5, () -> {
            context.assertTrue(world.getEntitiesByType(ModEntities.BACKROOMS_PORTAL, new Box(context.getAbsolutePos(DOOR)).expand(3.0), entity -> true).isEmpty(),
                    "La porte retiree emporte son portail d'entree.");
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA)
    public void entryPortalFillsTheDoorwayWhateverTheOrientation(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d arrival = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(6, 1, 6))).add(0.0, 0.5, 0.0);
        BlockPos north = new BlockPos(1, 1, 3);
        BlockPos east = new BlockPos(5, 1, 3);
        BackroomsDoorBlockEntity northDoor = placeDoor(context, north, Direction.NORTH, true);
        BackroomsDoorBlockEntity eastDoor = placeDoor(context, east, Direction.EAST, true);
        northDoor.configureTarget(World.OVERWORLD, arrival, PortalFacing.SOUTH);
        eastDoor.configureTarget(World.OVERWORLD, arrival.add(0.0, 0.0, 3.0), PortalFacing.SOUTH);

        context.runAtTick(5, () -> {
            BackroomsPortalEntity northPortal = (BackroomsPortalEntity) world.getEntity(northDoor.attachedPortalUuid());
            BackroomsPortalEntity eastPortal = (BackroomsPortalEntity) world.getEntity(eastDoor.attachedPortalUuid());
            try {
                context.assertTrue(northPortal != null && northPortal.getPos().isInRange(doorwayCenter(context, north), EPSILON),
                        "Le portail d'une porte au nord est au milieu de son encadrement.");
                context.assertTrue(northPortal.width() == BackroomsDoorBlockEntity.PORTAL_WIDTH && northPortal.height() == BackroomsDoorBlockEntity.PORTAL_HEIGHT,
                        "Le portail fait la taille de la porte : un bloc de large, deux de haut.");
                context.assertTrue(eastPortal != null && eastPortal.getPos().isInRange(doorwayCenter(context, east), EPSILON)
                        && eastPortal.facingSource() == PortalFacing.WEST, "Une porte vers l'est s'entre par l'ouest, au milieu de son encadrement.");
            } finally {
                removeDoor(context, north);
                removeDoor(context, east);
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA,
            tickLimit = BackroomsDoorBlockEntity.RETURN_CHECK_INTERVAL_TICKS * BackroomsDoorBlockEntity.RETURN_MISSING_CHECKS + 50)
    public void doorWaitsForItsChunkBeforeReplacingALostEntryPortal(TestContext context) {
        ServerWorld world = context.getWorld();
        BackroomsDoorBlockEntity door = placeDoor(context, DOOR, Direction.NORTH, true);
        // Porte rechargee avec un portail d'entree qui n'existe plus (sauvegarde interrompue).
        UUID lost = UUID.randomUUID();
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("AttachedPortal", lost);
        nbt.putString("DestDim", World.OVERWORLD.getValue().toString());
        Vec3d arrival = Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(6, 1, 6))).add(0.0, 0.5, 0.0);
        nbt.putDouble("DestX", arrival.x);
        nbt.putDouble("DestY", arrival.y);
        nbt.putDouble("DestZ", arrival.z);
        nbt.putString("DestFacing", "SOUTH");
        door.read(nbt, world.getRegistryManager());
        UUID[] entry = new UUID[1];

        context.runAtTick(BackroomsDoorBlockEntity.PORTAL_GRACE_TICKS / 2, () -> context.assertTrue(lost.equals(door.attachedPortalUuid()),
                "Pendant le delai de grace, la porte attend que son chunk livre ses entities."));
        context.runAtTick(BackroomsDoorBlockEntity.PORTAL_GRACE_TICKS + 20, () -> {
            context.assertTrue(!lost.equals(door.attachedPortalUuid()) && world.getEntity(door.attachedPortalUuid()) != null,
                    "Le delai passe, un nouveau portail d'entree est pose.");
            // Un changement d'etat du meme bloc (rotation) garde la porte et sa block entity ; l'autre moitie suit.
            entry[0] = door.attachedPortalUuid();
            context.setBlockState(DOOR, context.getBlockState(DOOR).with(DoorBlock.FACING, Direction.EAST));
            context.assertTrue(context.getBlockEntity(DOOR) == door, "Tourner la porte garde sa block entity.");
            context.assertTrue(context.getBlockState(DOOR.up()).get(DoorBlock.FACING) == Direction.EAST, "La moitie haute suit la rotation.");
        });
        context.runAtTick(BackroomsDoorBlockEntity.PORTAL_GRACE_TICKS + 25, () -> context.assertTrue(world.getEntity(entry[0]) == null
                && world.getEntity(door.attachedPortalUuid()) instanceof BackroomsPortalEntity turned && turned.facingSource() == PortalFacing.WEST,
                "Le portail d'entree suit la porte tournee : il est repose, entree par l'ouest."));
        // Sans portail de retour connu, la porte en repose un apres plusieurs verifications espacees.
        context.runAtTick(BackroomsDoorBlockEntity.RETURN_CHECK_INTERVAL_TICKS + 5,
                () -> context.assertTrue(door.returnPortalUuid() == null, "Une seule verification sans portail de retour ne suffit pas."));
        context.runAtTick(BackroomsDoorBlockEntity.RETURN_CHECK_INTERVAL_TICKS * BackroomsDoorBlockEntity.RETURN_MISSING_CHECKS + 5, () -> {
            try {
                context.assertTrue(door.returnPortalUuid() != null && world.getEntity(door.returnPortalUuid()) != null, "Le portail de retour manquant est repose.");
            } finally {
                removeDoor(context, DOOR);
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA,
            tickLimit = BackroomsDoorBlockEntity.RETURN_CHECK_INTERVAL_TICKS * (BackroomsDoorBlockEntity.RETURN_MISSING_CHECKS + 1) + 100)
    public void doorReplacesAReturnPortalThatDisappeared(TestContext context) {
        ServerWorld world = context.getWorld();
        BackroomsDoorBlockEntity door = placeDoor(context, DOOR, Direction.NORTH, false);
        door.configureTarget(World.OVERWORLD, Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(6, 1, 6))).add(0.0, 0.5, 0.0), PortalFacing.SOUTH);
        UUID[] original = new UUID[1];
        context.runAtTick(5, () -> {
            original[0] = door.returnPortalUuid();
            context.assertTrue(original[0] != null && world.getEntity(original[0]) != null, "Le portail de retour est pose.");
        });
        context.runAtTick(BackroomsDoorBlockEntity.RETURN_CHECK_INTERVAL_TICKS + 5, () -> {
            context.assertTrue(original[0].equals(door.returnPortalUuid()), "Present a la premiere verification, le portail de retour est garde.");
            world.getEntity(original[0]).discard();
        });
        context.runAtTick(BackroomsDoorBlockEntity.RETURN_CHECK_INTERVAL_TICKS * (BackroomsDoorBlockEntity.RETURN_MISSING_CHECKS + 1) + 50, () -> {
            try {
                UUID replaced = door.returnPortalUuid();
                context.assertTrue(replaced != null && !replaced.equals(original[0]) && world.getEntity(replaced) != null,
                        "Apres assez de verifications sans portail de retour, la porte en repose un.");
            } finally {
                removeDoor(context, DOOR);
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 400, batchId = "portal_level_zero_stand_in")
    public void doorResolvesItsLevelZeroTargetWhenTheDimensionExists(TestContext context) {
        ServerWorld world = context.getWorld();
        MinecraftServer server = world.getServer();
        ServerWorld standIn = GameTestServerWorlds.nether(context);
        GameTestServerWorlds.registerStandInLevelZero(server, standIn);
        // L'arrivee se calcule d'avance : son chunk est force, le portail de retour y est suivi et part avec la porte.
        BackroomsDoorTargets.Target target = BackroomsDoorTargets.resolve(world, context.getAbsolutePos(DOOR), Direction.SOUTH);
        if (target == null) {
            throw new GameTestException("Le Level 0 prete n'offre aucune arrivee.");
        }
        Vec3d arrival = target.position();
        GameTestServerWorlds.forceChunk(standIn, arrival, true);
        BackroomsDoorBlockEntity[] door = new BackroomsDoorBlockEntity[1];
        UUID[] returnPortal = new UUID[1];
        context.createTimedTaskRunner()
                .createAndAdd(() -> {
                    if (!standIn.shouldTickEntity(BlockPos.ofFloored(arrival))) {
                        throw new GameTestException("Le chunk d'arrivee n'est pas encore actif.");
                    }
                    door[0] = placeDoor(context, DOOR, Direction.NORTH, true);
                })
                .createAndAdd(() -> {
                    context.assertTrue(door[0].isConfigured(), "La porte a resolu sa destination dans le Level 0.");
                    context.assertTrue(door[0].attachedPortalUuid() != null && world.getEntity(door[0].attachedPortalUuid()) != null,
                            "Le portail d'entree est pose.");
                    returnPortal[0] = door[0].returnPortalUuid();
                    context.assertTrue(returnPortal[0] != null && standIn.getEntity(returnPortal[0]) != null,
                            "Le portail de retour est pose et suivi dans la dimension d'arrivee.");
                    removeDoor(context, DOOR);
                })
                .createAndAddReported(() -> context.assertTrue(standIn.getEntity(returnPortal[0]) == null,
                        "La porte cassee emporte son portail de retour."))
                .completeIfSuccessful();
    }

    /** Apres le lot du Level 0 prete, reussi ou non : plus de Level 0, plus de chunk force dans le Nether. */
    @AfterBatch(batchId = "portal_level_zero_stand_in")
    public void afterLevelZeroStandIn(ServerWorld world) {
        GameTestServerWorlds.removeStandInLevelZero(world.getServer());
        GameTestServerWorlds.releaseForcedChunks(world.getServer().getWorld(World.NETHER));
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 40, batchId = "portal_disabled")
    public void withPortalsDisabledTheDoorStaysAnOrdinaryDoor(TestContext context) {
        MinecraftServer server = context.getWorld().getServer();
        // Avec un Level 0, une porte active resoudrait sa destination des son premier tick.
        GameTestServerWorlds.registerStandInLevelZero(server, GameTestServerWorlds.nether(context));
        BackroomsDoorBlockEntity.setPortalsEnabled(false);
        BackroomsDoorBlockEntity door = placeDoor(context, DOOR, Direction.NORTH, true);
        context.runAtTick(5, () -> {
            try {
                context.assertTrue(!door.isConfigured() && door.attachedPortalUuid() == null && door.returnPortalUuid() == null,
                        "Portails desactives : la porte ne cherche pas de destination et ne pose aucun portail.");
            } finally {
                removeDoor(context, DOOR);
            }
            context.complete();
        });
    }

    /** Apres le lot sans portails, reussi ou non : portails rendus a leur configuration, plus de Level 0 prete. */
    @AfterBatch(batchId = "portal_disabled")
    public void afterPortalsDisabled(ServerWorld world) {
        BackroomsDoorBlockEntity.setPortalsEnabled(ModConfig.ENABLE_PORTAL_FEATURE);
        GameTestServerWorlds.removeStandInLevelZero(world.getServer());
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 40)
    public void aDoorSavedAsASingleBlockKeepsItsEntrySideAndRegrowsItsUpperHalf(TestContext context) {
        // Portes chargees sans moitie haute, sauvegardees quand elles tenaient en un bloc oriente vers leur entree (le nord) :
        // l'une a l'air libre, l'autre sous de la pierre, posee avant elle (une moitie basse seule tomberait a la mise a
        // jour de son voisin du dessus).
        BlockPos underAir = new BlockPos(3, 1, 3);
        BlockPos underStone = new BlockPos(6, 1, 3);
        context.setBlockState(underStone.up(), Blocks.STONE.getDefaultState());
        for (BlockPos doorPos : new BlockPos[] {underAir, underStone}) {
            context.setBlockState(doorPos.down(), Blocks.STONE.getDefaultState());
            context.setBlockState(doorPos, ModBlocks.BACKROOMS_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH));
        }
        context.runAtTick(5, () -> {
            try {
                BlockState lower = context.getBlockState(underAir);
                BlockState upper = context.getBlockState(underAir.up());
                context.assertTrue(BackroomsDoorBlock.entrySide(lower) == Direction.NORTH,
                        "La porte est retournee pour garder son cote d'entree : " + lower.get(DoorBlock.FACING) + ".");
                context.assertTrue(upper.isOf(ModBlocks.BACKROOMS_DOOR) && upper.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER
                        && upper.get(DoorBlock.FACING) == Direction.SOUTH && !upper.get(DoorBlock.OPEN),
                        "La moitie haute est reposee, fermee et orientee comme la basse.");
                context.assertTrue(BackroomsDoorBlock.entrySide(context.getBlockState(underStone)) == Direction.NORTH
                        && context.getBlockState(underStone.up()).isOf(Blocks.STONE),
                        "Sous un bloc plein, la porte garde aussi son cote d'entree, sans moitie haute.");
            } finally {
                removeDoor(context, underAir);
                removeDoor(context, underStone);
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 40)
    public void aDoorSavedAsASingleBlockGetsPortalsInItsDoorway(TestContext context) {
        ServerWorld world = context.getWorld();
        // Porte d'un bloc ouverte, orientee vers son entree (le nord) ; son portail d'entree ressortait a 0,55 bloc devant elle,
        // son retour y ramenait.
        context.setBlockState(DOOR.down(), Blocks.STONE.getDefaultState());
        context.setBlockState(DOOR, ModBlocks.BACKROOMS_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH).with(DoorBlock.OPEN, true));
        BackroomsDoorBlockEntity door = context.getBlockEntity(DOOR);
        Vec3d arrival = Vec3d.ofCenter(context.getAbsolutePos(ARRIVAL)).add(0.0, 0.5, 0.0);
        Vec3d oldEntry = doorwayCenter(context, DOOR).add(0.0, 0.0, -0.55);
        BackroomsPortalEntity entry = BackroomsPortalEntity.builder(world).position(oldEntry)
                .facingSource(PortalFacing.NORTH).facingDestination(PortalFacing.SOUTH).destinationDim(World.OVERWORLD).destinationPos(arrival)
                .size(1.0f, 2.0f).owner(World.OVERWORLD, context.getAbsolutePos(DOOR)).build();
        BackroomsPortalEntity back = BackroomsPortalEntity.builder(world).position(arrival)
                .facingSource(PortalFacing.SOUTH).facingDestination(PortalFacing.NORTH).destinationDim(World.OVERWORLD).destinationPos(oldEntry)
                .size(1.0f, 2.0f).owner(World.OVERWORLD, context.getAbsolutePos(DOOR)).build();
        world.spawnEntity(entry);
        world.spawnEntity(back);
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("AttachedPortal", entry.getUuid());
        nbt.putUuid("ReturnPortal", back.getUuid());
        nbt.putString("DestDim", World.OVERWORLD.getValue().toString());
        nbt.putDouble("DestX", arrival.x);
        nbt.putDouble("DestY", arrival.y);
        nbt.putDouble("DestZ", arrival.z);
        nbt.putString("DestFacing", "SOUTH");
        door.read(nbt, world.getRegistryManager());
        context.runAtTick(5, () -> {
            try {
                context.assertTrue(entry.isRemoved(), "L'ancien portail d'entree, hors de l'encadrement, est retire.");
                context.assertTrue(world.getEntity(door.attachedPortalUuid()) instanceof BackroomsPortalEntity fresh
                        && fresh.getPos().isInRange(doorwayCenter(context, DOOR), EPSILON) && fresh.facingSource() == PortalFacing.NORTH,
                        "Un portail d'entree remplit l'encadrement, toujours entre par le nord.");
                context.assertTrue(!back.isRemoved() && back.destinationPos().equals(world.getEntity(door.attachedPortalUuid()).getPos()),
                        "Le retour, deja dans la bonne orientation, est garde et ramene dans le plan de la porte.");
            } finally {
                removeDoor(context, DOOR);
            }
            context.complete();
        });
    }
}
