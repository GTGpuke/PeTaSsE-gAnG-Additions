package com.petassegang.addons.gametest.backrooms.portal;

import java.util.UUID;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;

import com.petassegang.addons.backrooms.BackroomsConstants;
import com.petassegang.addons.backrooms.portal.block.BackroomsDoorBlock;
import com.petassegang.addons.backrooms.portal.block.BackroomsDoorBlockEntity;
import com.petassegang.addons.backrooms.portal.block.BackroomsDoorTargets;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntityType;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.gametest.support.GameTestPlayers;
import com.petassegang.addons.gametest.support.GameTestServerWorlds;
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.init.ModItems;

/**
 * Tests en jeu de la porte des Backrooms : portails lies poses et retires avec elle, pose face au joueur, cibles
 * par dimension, et cas limites (sans destination, dimension absente, sol irregulier, portail d'entree perdu, porte
 * tournee, portail de retour disparu ou jamais pose, Level 0 prete au serveur de test).
 */
public final class BackroomsDoorGameTests implements FabricGameTest {

    private static final BlockPos DOOR = new BlockPos(1, 1, 1);
    private static final BlockPos ARRIVAL = new BlockPos(1, 1, 8);
    private static final double EPSILON = 1.0e-3;

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 100)
    public void doorSpawnsLinkedPortalsAndRemovesThemWithIt(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos doorPos = context.getAbsolutePos(DOOR);
        Vec3d arrival = Vec3d.ofCenter(context.getAbsolutePos(ARRIVAL)).add(0.0, 0.5, 0.0);
        context.setBlockState(DOOR, ModBlocks.BACKROOMS_DOOR.getDefaultState().with(BackroomsDoorBlock.FACING, Direction.NORTH));
        BackroomsDoorBlockEntity door = context.getBlockEntity(DOOR);
        door.configureTarget(World.OVERWORLD, arrival, PortalFacing.SOUTH);
        UUID[] portals = new UUID[2];

        context.runAtTick(5, () -> {
            portals[0] = door.attachedPortalUuid();
            portals[1] = door.returnPortalUuid();
            context.assertTrue(portals[0] != null && portals[1] != null, "La porte a pose ses deux portails.");
            BackroomsPortalEntity entry = (BackroomsPortalEntity) world.getEntity(portals[0]);
            BackroomsPortalEntity back = (BackroomsPortalEntity) world.getEntity(portals[1]);
            context.assertTrue(entry != null && back != null, "Les deux portails sont charges.");
            context.assertTrue(entry.facingSource() == PortalFacing.NORTH && entry.facingDestination() == PortalFacing.SOUTH,
                    "Le portail d'entree regarde comme la porte et sort par le portail de retour.");
            context.assertTrue(back.facingSource() == PortalFacing.SOUTH && back.facingDestination() == PortalFacing.NORTH,
                    "Le portail de retour est le miroir exact du portail d'entree.");
            context.assertTrue(back.getPos().isInRange(arrival, EPSILON), "Le portail de retour est au point d'arrivee.");
            context.assertTrue(entry.destinationPos().isInRange(back.getPos(), EPSILON)
                    && back.destinationPos().isInRange(entry.getPos(), EPSILON), "Chaque portail mene a l'autre.");
            context.assertTrue(doorPos.equals(entry.ownerPos()) && doorPos.equals(back.ownerPos()),
                    "Les deux portails appartiennent a la porte.");
            context.assertTrue(entry.signedDistance(Vec3d.ofCenter(doorPos.north(2))) > 0.0,
                    "Le cote d'entree du portail est devant la porte.");
            context.setBlockState(DOOR, Blocks.AIR);
        });
        context.runAtTick(10, () -> {
            context.assertTrue(world.getEntity(portals[0]) == null && world.getEntity(portals[1]) == null,
                    "Casser la porte retire ses deux portails.");
            // Boites serrees : les autres tests de portail tournent a quelques blocs de la, dans le meme monde.
            context.assertTrue(world.getEntitiesByType(BackroomsPortalEntityType.INSTANCE, new Box(doorPos).expand(3.0), entity -> true).isEmpty()
                    && world.getEntitiesByType(BackroomsPortalEntityType.INSTANCE, new Box(BlockPos.ofFloored(arrival)).expand(3.0), entity -> true).isEmpty(),
                    "Aucun portail ne survit a la porte, ni devant elle ni a l'arrivee.");
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 150)
    public void doorPlacementFacesThePlayerAndTargetsResolveByDimension(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos floor = context.getAbsolutePos(new BlockPos(2, 0, 2));
        BlockPos doorPos = floor.up();
        world.setBlockState(floor, Blocks.STONE.getDefaultState());
        // Le joueur regarde le nord : la porte posee lui fait face, donc regarde le sud.
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.CREATIVE, context.getAbsolute(new Vec3d(2.5, 1.0, 5.5)), 180.0f, 0.0f);
        try {
            ItemStack stack = new ItemStack(ModItems.BACKROOMS_DOOR);
            player.setStackInHand(Hand.MAIN_HAND, stack);
            ItemPlacementContext placement = new ItemPlacementContext(player, Hand.MAIN_HAND, stack,
                    new BlockHitResult(Vec3d.ofCenter(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false));
            context.assertTrue(ModItems.BACKROOMS_DOOR.place(placement).isAccepted(), "La porte se pose.");
            BlockState door = world.getBlockState(doorPos);
            context.assertTrue(door.isOf(ModBlocks.BACKROOMS_DOOR) && door.get(BackroomsDoorBlock.FACING) == Direction.SOUTH,
                    "La porte regarde le joueur qui l'a posee.");

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
        // La porte posee reste sans destination : elle reessaie periodiquement, sans portail.
        context.runAtTick(110, () -> {
            BackroomsDoorBlockEntity entity = (BackroomsDoorBlockEntity) world.getBlockEntity(doorPos);
            context.assertTrue(entity != null && !entity.isConfigured() && entity.attachedPortalUuid() == null,
                    "Sans destination, pas de portail.");
            world.setBlockState(doorPos, Blocks.AIR.getDefaultState());
            world.setBlockState(floor, Blocks.AIR.getDefaultState());
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 260)
    public void doorWithAMissingDestinationDimensionOnlyKeepsItsEntryPortal(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos doorPos = new BlockPos(1, 1, 1);
        context.setBlockState(doorPos, ModBlocks.BACKROOMS_DOOR.getDefaultState().with(BackroomsDoorBlock.FACING, Direction.NORTH));
        BackroomsDoorBlockEntity door = context.getBlockEntity(doorPos);
        RegistryKey<World> nowhere = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(ModConstants.MOD_ID, "nulle_part"));
        door.configureTarget(nowhere, new Vec3d(0.5, 65.0, 0.5), PortalFacing.SOUTH);
        // Une seconde porte dont l'arrivee est a cent mille blocs : son chunk ne tique pas, la verification ne tranche pas.
        BlockPos farDoorPos = new BlockPos(4, 1, 1);
        context.setBlockState(farDoorPos, ModBlocks.BACKROOMS_DOOR.getDefaultState().with(BackroomsDoorBlock.FACING, Direction.NORTH));
        BackroomsDoorBlockEntity farDoor = context.getBlockEntity(farDoorPos);
        farDoor.configureTarget(World.OVERWORLD, new Vec3d(100_000.5, 70.0, 100_000.5), PortalFacing.SOUTH);
        UUID[] farReturn = new UUID[1];
        context.runAtTick(5, () -> {
            context.assertTrue(door.attachedPortalUuid() != null && world.getEntity(door.attachedPortalUuid()) != null, "Le portail d'entree existe.");
            context.assertTrue(door.returnPortalUuid() == null, "Sans dimension d'arrivee, pas de portail de retour.");
            farReturn[0] = farDoor.returnPortalUuid();
            context.assertTrue(farReturn[0] != null, "Le portail de retour lointain a ete pose dans son chunk charge a la demande.");
        });
        context.runAtTick(205, () -> {
            context.assertTrue(door.returnPortalUuid() == null, "La verification periodique ne pose rien sans dimension d'arrivee.");
            context.assertTrue(farReturn[0].equals(farDoor.returnPortalUuid()), "Chunk d'arrivee inactif : la porte n'en conclut rien.");
            context.setBlockState(doorPos, Blocks.AIR.getDefaultState());
            context.setBlockState(farDoorPos, Blocks.AIR.getDefaultState());
        });
        context.runAtTick(210, () -> {
            context.assertTrue(world.getEntitiesByType(BackroomsPortalEntityType.INSTANCE, new Box(context.getAbsolutePos(doorPos)).expand(3.0), entity -> true).isEmpty(),
                    "La porte retiree emporte son portail d'entree.");
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA)
    public void entryPortalRestsOnTheFloorFoundInFrontOfTheDoor(TestContext context) {
        ServerWorld world = context.getWorld();
        // Porte flottante : rien devant elle jusqu'a deux blocs plus bas, le cadre part du niveau de la porte.
        BlockPos floating = new BlockPos(1, 3, 3);
        context.setBlockState(floating, ModBlocks.BACKROOMS_DOOR.getDefaultState().with(BackroomsDoorBlock.FACING, Direction.NORTH));
        BackroomsDoorBlockEntity floatingDoor = context.getBlockEntity(floating);
        floatingDoor.configureTarget(World.OVERWORLD, Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(6, 1, 6))).add(0.0, 0.5, 0.0), PortalFacing.SOUTH);
        // Porte sur une marche : le sol devant elle est un bloc plus bas.
        BlockPos stepped = new BlockPos(5, 3, 3);
        context.setBlockState(stepped.north().down(2), Blocks.STONE.getDefaultState());
        context.setBlockState(stepped, ModBlocks.BACKROOMS_DOOR.getDefaultState().with(BackroomsDoorBlock.FACING, Direction.NORTH));
        BackroomsDoorBlockEntity steppedDoor = context.getBlockEntity(stepped);
        steppedDoor.configureTarget(World.OVERWORLD, Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(6, 1, 1))).add(0.0, 0.5, 0.0), PortalFacing.SOUTH);

        context.runAtTick(5, () -> {
            BackroomsPortalEntity floatingPortal = (BackroomsPortalEntity) world.getEntity(floatingDoor.attachedPortalUuid());
            BackroomsPortalEntity steppedPortal = (BackroomsPortalEntity) world.getEntity(steppedDoor.attachedPortalUuid());
            try {
                context.assertTrue(floatingPortal != null && Math.abs(floatingPortal.getY() - (context.getAbsolutePos(floating).getY() + 1.0)) < EPSILON,
                        "Sans sol, le cadre part du bas de la porte.");
                context.assertTrue(steppedPortal != null && Math.abs(steppedPortal.getY() - (context.getAbsolutePos(stepped).getY() - 1.0 + 1.0)) < EPSILON,
                        "Sur une marche, le cadre repose sur le sol trouve un bloc plus bas.");
            } finally {
                context.setBlockState(floating, Blocks.AIR.getDefaultState());
                context.setBlockState(stepped, Blocks.AIR.getDefaultState());
                context.setBlockState(stepped.north().down(2), Blocks.AIR.getDefaultState());
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 450)
    public void doorWaitsForItsChunkBeforeReplacingALostEntryPortal(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos doorPos = new BlockPos(1, 1, 1);
        context.setBlockState(doorPos, ModBlocks.BACKROOMS_DOOR.getDefaultState().with(BackroomsDoorBlock.FACING, Direction.NORTH));
        BackroomsDoorBlockEntity door = context.getBlockEntity(doorPos);
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

        context.runAtTick(20, () -> context.assertTrue(lost.equals(door.attachedPortalUuid()),
                "Pendant le delai de grace, la porte attend que son chunk livre ses entities."));
        context.runAtTick(60, () -> {
            context.assertTrue(!lost.equals(door.attachedPortalUuid()) && world.getEntity(door.attachedPortalUuid()) != null,
                    "Le delai passe, un nouveau portail d'entree est pose.");
            // Un changement d'etat du meme bloc (rotation) garde la porte et son portail.
            UUID entry = door.attachedPortalUuid();
            context.setBlockState(doorPos, context.getBlockState(doorPos).with(BackroomsDoorBlock.FACING, Direction.EAST));
            context.assertTrue(context.getBlockEntity(doorPos) == door && world.getEntity(entry) != null, "Tourner la porte ne retire ni la porte ni son portail.");
        });
        // Sans portail de retour connu, la porte en repose un apres deux verifications espacees de deux cents ticks.
        context.runAtTick(205, () -> context.assertTrue(door.returnPortalUuid() == null, "Une seule verification sans portail de retour ne suffit pas."));
        context.runAtTick(405, () -> {
            try {
                context.assertTrue(door.returnPortalUuid() != null && world.getEntity(door.returnPortalUuid()) != null, "Le portail de retour manquant est repose.");
            } finally {
                context.setBlockState(doorPos, Blocks.AIR.getDefaultState());
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 700)
    public void doorReplacesAReturnPortalThatDisappeared(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos doorPos = new BlockPos(1, 1, 1);
        context.setBlockState(doorPos, ModBlocks.BACKROOMS_DOOR.getDefaultState().with(BackroomsDoorBlock.FACING, Direction.NORTH));
        BackroomsDoorBlockEntity door = context.getBlockEntity(doorPos);
        door.configureTarget(World.OVERWORLD, Vec3d.ofCenter(context.getAbsolutePos(new BlockPos(6, 1, 6))).add(0.0, 0.5, 0.0), PortalFacing.SOUTH);
        UUID[] original = new UUID[1];
        context.runAtTick(5, () -> {
            original[0] = door.returnPortalUuid();
            context.assertTrue(original[0] != null && world.getEntity(original[0]) != null, "Le portail de retour est pose.");
        });
        context.runAtTick(205, () -> {
            context.assertTrue(original[0].equals(door.returnPortalUuid()), "Present a la premiere verification, le portail de retour est garde.");
            world.getEntity(original[0]).discard();
        });
        context.runAtTick(650, () -> {
            try {
                UUID replaced = door.returnPortalUuid();
                context.assertTrue(replaced != null && !replaced.equals(original[0]) && world.getEntity(replaced) != null,
                        "Apres deux verifications sans portail de retour, la porte en repose un.");
            } finally {
                context.setBlockState(doorPos, Blocks.AIR.getDefaultState());
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 160, batchId = "portal_level_zero_stand_in")
    public void doorResolvesItsLevelZeroTargetWhenTheDimensionExists(TestContext context) {
        ServerWorld world = context.getWorld();
        MinecraftServer server = world.getServer();
        ServerWorld standIn = server.getWorld(World.NETHER);
        GameTestServerWorlds.registerStandInLevelZero(server, standIn);
        BlockPos doorPos = new BlockPos(1, 1, 1);
        context.setBlockState(doorPos, ModBlocks.BACKROOMS_DOOR.getDefaultState().with(BackroomsDoorBlock.FACING, Direction.NORTH));
        BackroomsDoorBlockEntity door = context.getBlockEntity(doorPos);
        context.runAtTick(110, () -> {
            try {
                context.assertTrue(door.isConfigured(), "La porte a resolu sa destination dans le Level 0.");
                context.assertTrue(door.attachedPortalUuid() != null && world.getEntity(door.attachedPortalUuid()) != null, "Le portail d'entree est pose.");
                // Le chunk d'arrivee est charge a la demande mais ne tique pas : son entity n'est pas encore dans l'index du monde.
                context.assertTrue(door.returnPortalUuid() != null, "Le portail de retour est pose dans la dimension d'arrivee.");
            } finally {
                context.setBlockState(doorPos, Blocks.AIR.getDefaultState());
                GameTestServerWorlds.removeStandInLevelZero(server);
            }
            context.complete();
        });
    }
}
