package com.gtgpuke.gtgsbackrooms.gametest.backrooms.portal;

import java.util.List;
import java.util.UUID;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.Registries;
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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;

import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.block.BackroomsPortalBlock;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.block.BackroomsPortalBlockEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.block.PortalArea;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.block.PortalSettings;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.block.PortalTargets;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.PortalSightPayload;
import com.gtgpuke.gtgsbackrooms.config.ModConfig;
import com.gtgpuke.gtgsbackrooms.gametest.support.GameTestPlayers;
import com.gtgpuke.gtgsbackrooms.gametest.support.GameTestServerWorlds;
import com.gtgpuke.gtgsbackrooms.init.ModBlockEntities;
import com.gtgpuke.gtgsbackrooms.init.ModBlocks;
import com.gtgpuke.gtgsbackrooms.init.ModEntities;
import com.gtgpuke.gtgsbackrooms.init.ModItems;

/**
 * Tests en jeu du bloc portail : pose face au joueur avec les reglages de l'item, cadre bloque refuse, deux faces de
 * chaque cote posees et retirees avec le bloc, bloc tourne, bloc copie, reglages envoyes aux clients, traversee par la
 * face arriere, cibles par dimension, et cas
 * limites (sans destination, dimension absente, portail d'entree perdu, retour disparu ou jamais pose, Level 0 prete au
 * serveur de test, portails desactives).
 */
public final class BackroomsPortalBlockGameTests implements FabricGameTest {

    private static final BlockPos PORTAL = new BlockPos(1, 1, 1);
    private static final BlockPos ARRIVAL = new BlockPos(1, 1, 8);
    private static final double EPSILON = 1.0e-3;
    private static final int FRONT = BackroomsPortalBlockEntity.FRONT;
    private static final int BACK = BackroomsPortalBlockEntity.BACK;

    /** Pose un bloc portail, face avant vers {@code front}, aux reglages donnes, et renvoie sa block entity. */
    private static BackroomsPortalBlockEntity placePortal(TestContext context, BlockPos anchor, Direction front, PortalSettings settings) {
        context.setBlockState(anchor, ModBlocks.BACKROOMS_PORTAL.getDefaultState().with(BackroomsPortalBlock.FACING, front));
        BackroomsPortalBlockEntity portal = context.getBlockEntity(anchor);
        portal.readComponents(settings.toStack(new ItemStack(ModItems.BACKROOMS_PORTAL)));
        return portal;
    }

    /** Centre d'un cadre d'un bloc sur deux pose sur {@code anchor}. */
    private static Vec3d frameCenter(TestContext context, BlockPos anchor) {
        return Vec3d.ofBottomCenter(context.getAbsolutePos(anchor)).add(0.0, 1.0, 0.0);
    }

    private static BackroomsPortalEntity portal(ServerWorld world, UUID uuid) {
        return uuid == null ? null : (BackroomsPortalEntity) world.getEntity(uuid);
    }

    @GameTest(templateName = GameTestPlayers.ARENA)
    public void arrivalsRestOnTheRealFloor(TestContext context) {
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
            // Un portail du Level 0 pose son cadre sur le sol que voit la heightmap, a l'aplomb du bloc : ici le plafond de
            // barrieres de l'arene. Le centre est a mi-hauteur du cadre.
            int surface = world.getTopY(Heightmap.Type.MOTION_BLOCKING, slab.getX(), slab.getZ());
            PortalTargets.Target target = PortalTargets.resolve(server, BackroomsConstants.LEVEL_ZERO_WORLD, slab, PortalFacing.NORTH,
                    new PortalSettings(1, 4, true));
            context.assertTrue(target != null && Math.abs(target.position().y - (surface + 2.0)) < EPSILON,
                    "Le centre d'arrivee d'un cadre de quatre blocs est deux blocs au-dessus du sol vu par la heightmap.");
            context.assertTrue(target.facing() == PortalFacing.SOUTH, "Traverser ne fait pas tourner : la sortie regarde a l'oppose de l'entree.");
        } finally {
            world.setBlockState(slab, Blocks.AIR.getDefaultState());
            world.setBlockState(carpet, Blocks.AIR.getDefaultState());
            world.setBlockState(water, Blocks.AIR.getDefaultState());
        }
        context.complete();
    }

    private static void assertFloor(TestContext context, ServerWorld world, BlockPos column, int surfaceY, double floor, String message) {
        double actual = PortalTargets.floorHeight(world, column.getX(), surfaceY, column.getZ());
        context.assertTrue(Math.abs(actual - floor) < EPSILON, message + " Attendu " + floor + ", obtenu " + actual + ".");
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 100)
    public void eachSideHoldsBothFacesUntilTheBlockGoes(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos anchor = context.getAbsolutePos(PORTAL);
        Vec3d arrival = frameCenter(context, ARRIVAL);
        BackroomsPortalBlockEntity block = placePortal(context, PORTAL, Direction.NORTH, PortalSettings.DEFAULT);
        block.configureTarget(World.OVERWORLD, arrival, PortalFacing.SOUTH);
        UUID[] portals = new UUID[4];

        context.runAtTick(5, () -> {
            BackroomsPortalEntity entryFront = portal(world, block.entryPortal(FRONT));
            BackroomsPortalEntity entryBack = portal(world, block.entryPortal(BACK));
            BackroomsPortalEntity returnFront = portal(world, block.returnPortal(FRONT));
            BackroomsPortalEntity returnBack = portal(world, block.returnPortal(BACK));
            context.assertTrue(entryFront != null && entryBack != null && returnFront != null && returnBack != null,
                    "Le bloc a pose ses quatre portails : deux faces a l'entree, deux a l'arrivee.");
            context.assertTrue(entryFront.getPos().isInRange(frameCenter(context, PORTAL), EPSILON) && entryBack.getPos().equals(entryFront.getPos()),
                    "Les deux faces d'entree sont dans le plan du milieu du bloc : " + entryFront.getPos() + ".");
            context.assertTrue(entryFront.facingSource() == PortalFacing.NORTH && entryBack.facingSource() == PortalFacing.SOUTH,
                    "La face avant regarde le joueur qui a pose le bloc, la face arriere l'oppose.");
            context.assertTrue(entryFront.facingDestination() == PortalFacing.SOUTH && entryBack.facingDestination() == PortalFacing.NORTH,
                    "Chaque face mene a la face correspondante de l'arrivee.");
            context.assertTrue(returnFront.getPos().isInRange(arrival, EPSILON) && returnBack.getPos().equals(returnFront.getPos()),
                    "Les deux retours sont au point d'arrivee.");
            context.assertTrue(returnFront.facingSource() == PortalFacing.SOUTH && returnFront.facingDestination() == PortalFacing.NORTH
                    && returnBack.facingSource() == PortalFacing.NORTH && returnBack.facingDestination() == PortalFacing.SOUTH,
                    "Les retours sont l'inverse exact des entrees.");
            context.assertTrue(returnFront.destinationPos().equals(entryFront.getPos()), "Le retour ramene dans le plan meme du bloc.");
            Vec3d point = entryFront.getPos().add(0.3, -0.4, 1.7);
            context.assertTrue(entryFront.toDestination(point).isInRange(entryBack.toDestination(point), EPSILON),
                    "Les deux faces transposent l'espace de la meme facon : un trou qui se traverse dans les deux sens.");
            context.assertTrue(entryFront.width() == 1.0f && entryFront.height() == 2.0f && entryFront.transmitsLight(),
                    "Les portails ont la taille et la lumiere reglees.");
            context.assertTrue(anchor.equals(entryFront.owner().pos()) && anchor.equals(returnBack.owner().pos()), "Les portails appartiennent au bloc.");
            NbtCompound saved = block.createNbt(world.getRegistryManager());
            context.assertTrue(saved.containsUuid("EntryFront") && saved.containsUuid("ReturnBack"), "Le bloc sauve ses quatre portails.");
            portals[0] = block.entryPortal(FRONT);
            portals[1] = block.entryPortal(BACK);
            portals[2] = block.returnPortal(FRONT);
            portals[3] = block.returnPortal(BACK);
            // Bloc tourne vers l'est : meme bloc, meme block entity, portails reposes.
            context.setBlockState(PORTAL, context.getBlockState(PORTAL).with(BackroomsPortalBlock.FACING, Direction.EAST));
            context.assertTrue(context.getBlockEntity(PORTAL) == block, "Tourner le bloc garde sa block entity.");
        });
        context.runAtTick(8, () -> {
            for (UUID old : portals) {
                context.assertTrue(world.getEntity(old) == null, "Les portails de l'ancienne orientation sont retires.");
            }
            context.assertTrue(portal(world, block.entryPortal(FRONT)) instanceof BackroomsPortalEntity front && front.facingSource() == PortalFacing.EAST
                    && portal(world, block.entryPortal(BACK)).facingSource() == PortalFacing.WEST, "Les faces d'entree suivent le bloc tourne.");
            context.assertTrue(portal(world, block.returnPortal(FRONT)) instanceof BackroomsPortalEntity back && back.facingDestination() == PortalFacing.EAST,
                    "Les retours sont reposes : ils ramenent dans la nouvelle orientation.");
            context.setBlockState(PORTAL, Blocks.AIR);
        });
        context.runAtTick(12, () -> {
            context.assertTrue(world.getEntitiesByType(ModEntities.BACKROOMS_PORTAL, new Box(anchor).expand(3.0), entity -> true).isEmpty()
                    && world.getEntitiesByType(ModEntities.BACKROOMS_PORTAL, new Box(BlockPos.ofFloored(arrival)).expand(3.0), entity -> true).isEmpty(),
                    "Casser le bloc retire ses quatre portails, a l'entree comme a l'arrivee.");
            // Un bloc dont la block entity a disparu se retire sans rien casser : une exception ferait echouer la tache.
            context.setBlockState(PORTAL, ModBlocks.BACKROOMS_PORTAL.getDefaultState());
            world.removeBlockEntity(anchor);
            context.setBlockState(PORTAL, Blocks.AIR);
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 60)
    public void aBlockRereadWithOtherSettingsOrDestinationReplacesItsPortals(TestContext context) {
        ServerWorld world = context.getWorld();
        BackroomsPortalBlockEntity block = placePortal(context, PORTAL, Direction.NORTH, PortalSettings.DEFAULT);
        // Relu avant d'avoir une destination : il n'a rien a recaler.
        block.read(block.createNbt(world.getRegistryManager()), world.getRegistryManager());
        block.configureTarget(World.OVERWORLD, frameCenter(context, ARRIVAL), PortalFacing.SOUTH);
        UUID[] entry = new UUID[1];
        UUID[] returns = new UUID[2];
        double[] arrivalY = new double[1];
        // Chaque relecture change un seul reglage ou un seul element de la destination : les portails sont reposes.
        String[] steps = {"Width", "TransmitsLight", "DestX", "DestDim", "Height"};
        for (int step = 0; step < steps.length; step++) {
            String key = steps[step];
            context.runAtTick(3 + step * 3, () -> {
                entry[0] = block.entryPortal(FRONT);
                returns[0] = block.returnPortal(FRONT);
                returns[1] = block.returnPortal(BACK);
                context.assertTrue(portal(world, entry[0]) != null, "La face avant existe avant de relire " + key + ".");
                arrivalY[0] = portal(world, entry[0]).destinationPos().y;
                NbtCompound nbt = block.createNbt(world.getRegistryManager());
                switch (key) {
                    case "Width" -> nbt.putInt(key, 3);
                    case "TransmitsLight" -> nbt.putBoolean(key, false);
                    case "DestX" -> nbt.putDouble(key, nbt.getDouble(key) + 2.0);
                    case "DestDim" -> nbt.putString(key, World.NETHER.getValue().toString());
                    default -> nbt.putInt(key, 3);
                }
                block.read(nbt, world.getRegistryManager());
            });
            context.runAtTick(5 + step * 3, () -> {
                context.assertTrue(world.getEntity(entry[0]) == null && portal(world, block.entryPortal(FRONT)) != null,
                        "Relu avec un autre " + key + ", le bloc repose sa face avant.");
                if ("DestDim".equals(key)) {
                    // Les anciens retours restent dans l'overworld, hors de la nouvelle destination, jusqu'a ce que le bloc
                    // en pose de nouveaux et que le registre les retire : retires ici a la main.
                    GameTestServerWorlds.discardEverywhere(world, world.getEntity(returns[0]));
                    GameTestServerWorlds.discardEverywhere(world, world.getEntity(returns[1]));
                } else if ("Height".equals(key)) {
                    context.assertTrue(Math.abs(portal(world, block.entryPortal(FRONT)).destinationPos().y - arrivalY[0] - 0.5) < EPSILON,
                            "Relu a trois de haut, le cadre d'arrivee garde son pied : son centre monte d'un demi-bloc.");
                } else {
                    context.assertTrue(world.getEntity(returns[0]) == null && portal(world, block.returnPortal(FRONT)) != null,
                            "Et son retour avant, qui ne correspond plus.");
                }
            });
        }
        context.runAtTick(3 + steps.length * 3, () -> {
            context.setBlockState(PORTAL, Blocks.AIR);
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 60)
    public void aCopiedBlockKeepsTheDestinationAndPlacesItsOwnPortals(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos copyPos = new BlockPos(6, 1, 1);
        PortalSettings wide = new PortalSettings(2, 3, true);
        BackroomsPortalBlockEntity original = placePortal(context, PORTAL, Direction.NORTH, wide);
        Vec3d destination = PortalArea.center(Vec3d.ofBottomCenter(context.getAbsolutePos(ARRIVAL)), PortalFacing.SOUTH, wide);
        original.configureTarget(World.OVERWORLD, destination, PortalFacing.SOUTH);
        UUID[] originals = {original.entryPortal(FRONT), original.entryPortal(BACK), original.returnPortal(FRONT), original.returnPortal(BACK)};
        // Une copie (clone, structure posee) : un bloc neuf, deja dans le monde, relit la sauvegarde de l'original.
        BackroomsPortalBlockEntity copy = placePortal(context, copyPos, Direction.NORTH, PortalSettings.DEFAULT);
        copy.read(original.createNbt(world.getRegistryManager()), world.getRegistryManager());
        context.assertTrue(copy.settings().equals(wide) && copy.entryPortal(FRONT) == null && copy.returnPortal(FRONT) == null,
                "La copie a les reglages de l'original, pas ses portails.");
        NbtCompound sent = ((BlockEntityUpdateS2CPacket) original.toUpdatePacket()).getNbt();
        context.assertTrue(sent.getInt("Width") == 2 && sent.getInt("Height") == 3 && !sent.contains("DestDim") && !sent.containsUuid("EntryFront"),
                "Les clients recoivent les reglages du bloc, ni sa destination ni ses portails : " + sent + ".");
        context.runAtTick(3, () -> {
            try {
                for (UUID uuid : originals) {
                    context.assertTrue(world.getEntity(uuid) != null, "Les portails de l'original restent.");
                }
                BackroomsPortalEntity entry = portal(world, copy.entryPortal(FRONT));
                BackroomsPortalEntity back = portal(world, copy.returnPortal(BACK));
                context.assertTrue(entry != null && back != null && !copy.entryPortal(FRONT).equals(originals[0]),
                        "Des son premier tick, la copie pose ses propres faces et ses propres retours.");
                context.assertTrue(entry.destinationPos().equals(destination) && back.getPos().equals(destination),
                        "Elle garde la destination de l'original, sans la recaler.");
            } finally {
                context.setBlockState(PORTAL, Blocks.AIR);
                context.setBlockState(copyPos, Blocks.AIR);
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 60)
    public void anEntityCrossingTheBackFaceComesOutOfTheBackOfTheArrival(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d arrival = frameCenter(context, new BlockPos(12, 1, 12));
        BackroomsPortalBlockEntity block = placePortal(context, PORTAL, Direction.NORTH, PortalSettings.DEFAULT);
        block.configureTarget(World.OVERWORLD, arrival, PortalFacing.SOUTH);
        Vec3d plane = frameCenter(context, PORTAL);
        // Lance du sud vers le nord : il entre par la face arriere du cadre.
        ItemEntity thrown = new ItemEntity(world, plane.x, plane.y - 0.5, plane.z + 0.4, new ItemStack(Items.GOLD_NUGGET));
        thrown.setNoGravity(true);
        thrown.setVelocity(0.0, 0.0, -0.5);
        world.spawnEntity(thrown);
        context.runAtTick(10, () -> {
            try {
                context.assertTrue(Math.abs(thrown.getX() - arrival.x) < 0.5 && thrown.getZ() < arrival.z,
                        "La pepite ressort par la face arriere de l'arrivee, du cote nord, et continue vers le nord : " + thrown.getPos() + ".");
            } finally {
                thrown.discard();
                context.setBlockState(PORTAL, Blocks.AIR);
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 60)
    public void aSingleSidedPortalHasNothingBehindAndAOneWayPortalNoReturn(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos anchor = new BlockPos(6, 1, 6);
        Vec3d arrival = frameCenter(context, new BlockPos(16, 1, 16));
        BackroomsPortalBlockEntity block = placePortal(context, anchor, Direction.NORTH, PortalSettings.DEFAULT);
        block.configureTarget(World.OVERWORLD, arrival, PortalFacing.SOUTH);
        Vec3d plane = frameCenter(context, anchor);
        UUID[] removed = new UUID[2];
        ItemEntity[] thrown = new ItemEntity[1];
        context.runAtTick(3, () -> {
            removed[0] = block.entryPortal(BACK);
            removed[1] = block.returnPortal(BACK);
            context.assertTrue(portal(world, removed[0]) != null && portal(world, removed[1]) != null, "Deux faces au depart.");
            NbtCompound nbt = block.createNbt(world.getRegistryManager());
            nbt.putBoolean("DoubleSided", false);
            block.read(nbt, world.getRegistryManager());
        });
        context.runAtTick(5, () -> {
            context.assertTrue(world.getEntity(removed[0]) == null && world.getEntity(removed[1]) == null
                    && block.entryPortal(BACK) == null && block.returnPortal(BACK) == null, "Relu a une face : la face arriere et son retour sont retires.");
            context.assertTrue(portal(world, block.entryPortal(FRONT)) != null && portal(world, block.returnPortal(FRONT)) != null,
                    "La face avant et son retour restent.");
            // Lancee du sud vers le nord, par l'arriere : rien de ce cote, elle passe au travers du plan.
            thrown[0] = new ItemEntity(world, plane.x, plane.y - 0.5, plane.z + 0.4, new ItemStack(Items.GOLD_NUGGET));
            thrown[0].setNoGravity(true);
            thrown[0].setVelocity(0.0, 0.0, -0.5);
            world.spawnEntity(thrown[0]);
        });
        context.runAtTick(15, () -> {
            try {
                context.assertTrue(thrown[0].getZ() < plane.z - 1.0 && Math.abs(thrown[0].getX() - plane.x) < 0.5,
                        "Par l'arriere, la pepite traverse le plan sans etre teleportee : " + thrown[0].getPos() + ".");
            } finally {
                thrown[0].discard();
            }
            removed[0] = block.returnPortal(FRONT);
            NbtCompound nbt = block.createNbt(world.getRegistryManager());
            nbt.putBoolean("OneWay", true);
            block.read(nbt, world.getRegistryManager());
        });
        context.runAtTick(17, () -> {
            context.assertTrue(world.getEntity(removed[0]) == null && block.returnPortal(FRONT) == null, "Relu en aller simple : le retour est retire.");
            context.assertTrue(portal(world, block.entryPortal(FRONT)) != null, "L'entree reste.");
            context.setBlockState(anchor, Blocks.AIR);
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 40)
    public void aSingleSidedOneWayPortalPlacesItsFrontFaceOnly(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d arrival = frameCenter(context, ARRIVAL);
        BackroomsPortalBlockEntity block = placePortal(context, PORTAL, Direction.NORTH, new PortalSettings(1, 2, true, false, true, false));
        block.configureTarget(World.OVERWORLD, arrival, PortalFacing.SOUTH);
        context.runAtTick(5, () -> {
            context.assertTrue(portal(world, block.entryPortal(FRONT)) != null && block.entryPortal(BACK) == null, "Une seule face, a l'avant.");
            context.assertTrue(block.returnPortal(FRONT) == null && block.returnPortal(BACK) == null
                    && world.getEntitiesByType(ModEntities.BACKROOMS_PORTAL, new Box(BlockPos.ofFloored(arrival)).expand(2.0), entity -> true).isEmpty(),
                    "Aller simple : rien a l'arrivee pour revenir.");
            context.setBlockState(PORTAL, Blocks.AIR);
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 800, batchId = "portal_fleeting_exit")
    public void aFleetingExitHoldsOnlyWhileSomeoneSeesIt(TestContext context) {
        ServerWorld world = context.getWorld();
        // Le test compte les joueurs qui regardent : aucun joueur d'un autre test ne doit etre la.
        GameTestPlayers.disconnectAll();
        PortalSettings fleeting = new PortalSettings(1, 2, true, false, false, true);
        BackroomsPortalBlockEntity watched = placePortal(context, new BlockPos(1, 1, 1), Direction.NORTH, fleeting);
        watched.configureTarget(World.OVERWORLD, frameCenter(context, new BlockPos(1, 1, 12)), PortalFacing.NORTH);
        // Un second bloc, son arrivee a vingt blocs de la premiere : hors de portee du joueur qui regarde celle-ci.
        BackroomsPortalBlockEntity other = placePortal(context, new BlockPos(20, 1, 1), Direction.NORTH, fleeting);
        other.configureTarget(World.OVERWORLD, frameCenter(context, new BlockPos(20, 1, 12)), PortalFacing.NORTH);
        // Pres de la sortie du premier ; son client rapporte aussitot ne rien voir, avant meme que la sortie n'existe chez lui.
        ServerPlayerEntity watcher = GameTestPlayers.connect(context, GameMode.CREATIVE, context.getAbsolute(new Vec3d(1.5, 1.0, 7.5)), 0.0f, 0.0f);
        sees(watcher);
        Vec3d nearOther = context.getAbsolute(new Vec3d(20.5, 1.0, 7.5));
        Vec3d farFromOther = context.getAbsolute(new Vec3d(20.5, 1.0, 40.5));
        BlockPos copyPos = new BlockPos(10, 1, 1);
        BackroomsPortalBlockEntity[] copy = new BackroomsPortalBlockEntity[1];
        ServerPlayerEntity[] silent = new ServerPlayerEntity[1];
        long[] since = new long[1];
        long[] lost = new long[1];
        context.createTimedTaskRunner()
                .createAndAdd(() -> context.assertTrue(portal(world, watched.returnPortal(FRONT)) != null, "La sortie n'est pas encore posee."))
                .createAndAddReported(() -> since[0] = context.getTick())
                // Plus que le delai sans regard, moins que la grace d'une sortie qui vient d'etre posee.
                .createAndAdd(() -> waitTicks(context, since[0], BackroomsPortalBlockEntity.EXIT_UNSEEN_TICKS + 12))
                .createAndAddReported(() -> {
                    context.assertTrue(watched.returnPortal(FRONT) != null,
                            "Juste posee, la sortie laisse au client le temps de la voir : un rapport vide ne la perd pas encore.");
                    sees(watcher, watched.returnPortal(FRONT));
                    since[0] = context.getTick();
                })
                // Au-dela du delai de grace : seul le regard la tient.
                .createAndAdd(() -> waitTicks(context, since[0], 40))
                .createAndAddReported(() -> {
                    context.assertTrue(watched.returnPortal(FRONT) != null, "Son client la voit : elle reste.");
                    // Une image sans la sortie, pendant une traversee : un rapport vide isole.
                    sees(watcher);
                    since[0] = context.getTick();
                })
                .createAndAdd(() -> waitTicks(context, since[0], 8))
                .createAndAddReported(() -> {
                    context.assertTrue(watched.returnPortal(FRONT) != null, "Un rapport vide isole ne perd pas la sortie.");
                    sees(watcher, watched.returnPortal(FRONT));
                    // Un rapport fait dans le Nether, arrive alors que le serveur le tient deja ici : il ne dit rien de ce monde.
                    GameTestPlayers.receive(watcher, new PortalSightPayload(World.NETHER.getValue(), List.of()));
                    since[0] = context.getTick();
                })
                .createAndAdd(() -> waitTicks(context, since[0], BackroomsPortalBlockEntity.EXIT_UNSEEN_TICKS + 8))
                .createAndAddReported(() -> {
                    context.assertTrue(watched.returnPortal(FRONT) != null,
                            "Revue a temps, la sortie reste ; un rapport fait dans un autre monde est ignore.");
                    // Il se retourne : son client ne la voit plus.
                    sees(watcher);
                    since[0] = context.getTick();
                })
                .createAndAdd(() -> context.assertTrue(watched.returnPortal(FRONT) == null, "La sortie perdue de vue n'a pas encore disparu."))
                .createAndAddReported(() -> {
                    context.assertTrue(context.getTick() - since[0] >= BackroomsPortalBlockEntity.EXIT_UNSEEN_TICKS,
                            "La sortie ne disparait qu'apres une seconde sans regard.");
                    lost[0] = context.getTick();
                    NbtCompound saved = watched.createNbt(world.getRegistryManager());
                    context.assertTrue(saved.getBoolean("ExitLost"), "Perdue pour de bon : c'est sauve.");
                    // Une copie du bloc (clone, structure) a sa propre sortie, toute neuve.
                    context.setBlockState(copyPos, ModBlocks.BACKROOMS_PORTAL.getDefaultState().with(BackroomsPortalBlock.FACING, Direction.NORTH));
                    copy[0] = context.getBlockEntity(copyPos);
                    copy[0].read(saved, world.getRegistryManager());
                    // Le joueur part dans le Nether et y rapporte ne rien voir ; pres de la seconde sortie, un joueur muet.
                    watcher.teleportTo(new TeleportTarget(GameTestServerWorlds.nether(context), new Vec3d(0.5, 120.0, 0.5), Vec3d.ZERO, 0.0f, 0.0f,
                            TeleportTarget.NO_OP));
                    sees(watcher);
                    silent[0] = GameTestPlayers.connect(context, GameMode.CREATIVE, nearOther, 0.0f, 0.0f);
                    since[0] = context.getTick();
                })
                .createAndAdd(() -> waitTicks(context, since[0], BackroomsPortalBlockEntity.EXIT_UNSEEN_TICKS + 12))
                .createAndAddReported(() -> {
                    context.assertTrue(other.returnPortal(FRONT) != null, "Un joueur dont le client ne rapporte rien est tenu pour la voir : elle reste.");
                    GameTestPlayers.disconnect(silent[0]);
                    // Revenu pres d'elle, son dernier rapport vient du Nether.
                    watcher.teleportTo(new TeleportTarget(world, nearOther, Vec3d.ZERO, 0.0f, 0.0f, TeleportTarget.NO_OP));
                    since[0] = context.getTick();
                })
                .createAndAdd(() -> waitTicks(context, since[0], BackroomsPortalBlockEntity.EXIT_UNSEEN_TICKS + 12))
                .createAndAddReported(() -> {
                    context.assertTrue(watched.returnPortal(FRONT) == null, "La premiere sortie ne revient pas.");
                    context.assertTrue(portal(world, copy[0].returnPortal(FRONT)) != null, "La copie a sa propre sortie.");
                    context.assertTrue(other.returnPortal(FRONT) != null, "Un rapport d'un autre monde ne compte pas : la sortie reste.");
                    // Loin de la sortie, il ne voit rien ; pres d'elle, un spectateur qui ne la voit pas non plus.
                    watcher.refreshPositionAndAngles(farFromOther.x, farFromOther.y, farFromOther.z, 0.0f, 0.0f);
                    sees(watcher);
                    silent[0] = GameTestPlayers.connect(context, GameMode.SPECTATOR, nearOther, 0.0f, 0.0f);
                    sees(silent[0]);
                    since[0] = context.getTick();
                })
                .createAndAdd(() -> waitTicks(context, since[0], 50))
                .createAndAddReported(() -> {
                    context.assertTrue(other.returnPortal(FRONT) != null, "Personne pres de la sortie, un spectateur ne compte pas : elle reste.");
                    GameTestPlayers.disconnect(silent[0]);
                    // Revenu pres d'elle, son rapport a trop vieilli pour compter : il est tenu pour la voir.
                    watcher.refreshPositionAndAngles(nearOther.x, nearOther.y, nearOther.z, 0.0f, 0.0f);
                    since[0] = context.getTick();
                })
                .createAndAdd(() -> waitTicks(context, since[0], BackroomsPortalBlockEntity.EXIT_UNSEEN_TICKS + 12))
                .createAndAddReported(() -> {
                    context.assertTrue(other.returnPortal(FRONT) != null, "Un rapport trop vieux ne compte plus : la sortie reste.");
                    sees(watcher);
                })
                .createAndAdd(() -> context.assertTrue(other.returnPortal(FRONT) == null, "La seconde sortie n'a pas encore disparu."))
                // Assez de verifications des retours pour qu'un retour manquant soit repose, si le bloc oubliait la perte.
                .createAndAdd(() -> waitTicks(context, lost[0],
                        BackroomsPortalBlockEntity.RETURN_CHECK_INTERVAL_TICKS * BackroomsPortalBlockEntity.RETURN_MISSING_CHECKS + 10))
                .createAndAddReported(() -> {
                    try {
                        context.assertTrue(watched.returnPortal(FRONT) == null, "Perdue pour de bon : les verifications des retours ne la reposent pas.");
                    } finally {
                        GameTestPlayers.disconnect(watcher);
                        context.setBlockState(new BlockPos(1, 1, 1), Blocks.AIR);
                        context.setBlockState(new BlockPos(20, 1, 1), Blocks.AIR);
                        context.setBlockState(copyPos, Blocks.AIR);
                    }
                })
                .completeIfSuccessful();
    }

    /** Le client du joueur rapporte ce qu'il voit : ces portails, ou rien. */
    private static void sees(ServerPlayerEntity player, UUID... portals) {
        GameTestPlayers.receive(player, new PortalSightPayload(player.getServerWorld().getRegistryKey().getValue(), List.of(portals)));
    }

    /** Leve tant que ce nombre de ticks n'est pas passe depuis le tick donne. */
    private static void waitTicks(TestContext context, long since, int ticks) {
        if (context.getTick() < since + ticks) {
            throw new GameTestException("Pas encore " + ticks + " ticks.");
        }
    }

    /** Apres le lot de la sortie fugace, reussi ou non : plus de joueur. */
    @AfterBatch(batchId = "portal_fleeting_exit")
    public void afterFleetingExit(ServerWorld world) {
        GameTestPlayers.disconnectAll();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 300)
    public void aPortalItsBlockNoLongerHoldsIsRemoved(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d arrival = frameCenter(context, ARRIVAL);
        BackroomsPortalBlockEntity block = placePortal(context, PORTAL, Direction.NORTH, PortalSettings.DEFAULT);
        block.configureTarget(World.OVERWORLD, arrival, PortalFacing.SOUTH);
        // Se reclame du bloc sans que le bloc le tienne pour sien : une face retiree quand son chunk etait decharge.
        BackroomsPortalEntity stray = BackroomsPortalEntity.builder(world)
                .position(frameCenter(context, new BlockPos(6, 1, 1)))
                .facingSource(PortalFacing.NORTH)
                .facingDestination(PortalFacing.SOUTH)
                .destinationDim(World.OVERWORLD)
                .destinationPos(arrival)
                .size(1.0f, 2.0f)
                .owner(World.OVERWORLD, context.getAbsolutePos(PORTAL))
                .build();
        world.spawnEntity(stray);
        context.createTimedTaskRunner()
                .createAndAdd(() -> {
                    if (!stray.isRemoved()) {
                        throw new GameTestException("Le registre n'est pas encore passe sur les portails.");
                    }
                })
                .createAndAddReported(() -> {
                    try {
                        context.assertTrue(portal(world, block.entryPortal(FRONT)) != null && portal(world, block.returnPortal(BACK)) != null,
                                "Les portails que le bloc tient pour siens restent.");
                    } finally {
                        context.setBlockState(PORTAL, Blocks.AIR);
                    }
                })
                .completeIfSuccessful();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 150)
    public void placementFollowsThePlayerAndNeedsAFreeFrame(TestContext context) {
        ServerWorld world = context.getWorld();
        BlockPos floor = context.getAbsolutePos(new BlockPos(2, 0, 2));
        BlockPos blocked = context.getAbsolutePos(new BlockPos(6, 0, 2));
        BlockPos anchor = floor.up();
        world.setBlockState(floor, Blocks.STONE.getDefaultState());
        world.setBlockState(blocked, Blocks.STONE.getDefaultState());
        // Une pierre dans le cadre d'un portail de trois blocs pose sur la seconde pierre.
        world.setBlockState(blocked.up().east(), Blocks.STONE.getDefaultState());
        PortalSettings wide = new PortalSettings(2, 3, true);
        // Le joueur regarde le nord : la face avant du portail se tourne vers lui, au sud.
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.CREATIVE, context.getAbsolute(new Vec3d(2.5, 1.0, 5.5)), 180.0f, 0.0f);
        try {
            context.assertTrue(place(player, floor, wide), "Le bloc portail se pose.");
            BlockState state = world.getBlockState(anchor);
            context.assertTrue(state.isOf(ModBlocks.BACKROOMS_PORTAL) && state.get(BackroomsPortalBlock.FACING) == Direction.SOUTH,
                    "La face avant regarde le joueur.");
            context.assertTrue(world.getBlockEntity(anchor) instanceof BackroomsPortalBlockEntity placed && placed.settings().equals(wide),
                    "Le bloc garde les reglages de l'item.");
            context.assertTrue(!place(player, blocked, new PortalSettings(3, 2, true)) && world.getBlockState(blocked.up()).isAir(),
                    "Un cadre qui traverserait un bloc est refuse.");

            // Le regard ne s'arrete sur le bloc que si le joueur est accroupi : pour le casser.
            VoxelShape standing = state.getOutlineShape(world, anchor, ShapeContext.of(player));
            player.setSneaking(true);
            VoxelShape sneaking = state.getOutlineShape(world, anchor, ShapeContext.of(player));
            VoxelShape sneakingEast = state.with(BackroomsPortalBlock.FACING, Direction.EAST).getOutlineShape(world, anchor, ShapeContext.of(player));
            context.assertTrue(standing.isEmpty() && !sneaking.isEmpty(), "Debout, le regard passe ; accroupi, il vise le bloc.");
            context.assertTrue(sneaking.getMax(Direction.Axis.X) == 1.0 && sneakingEast.getMax(Direction.Axis.Z) == 1.0
                    && sneaking.getMax(Direction.Axis.Z) < 1.0 && sneakingEast.getMax(Direction.Axis.X) < 1.0,
                    "Le bloc visable est le plan du portail, dans son orientation.");

            // Un portail du Level 0 mene a la surface de l'overworld ; un portail de l'overworld cherche le Level 0, absent ici.
            PortalTargets.Target surface = PortalTargets.resolve(world.getServer(), BackroomsConstants.LEVEL_ZERO_WORLD, anchor, PortalFacing.SOUTH,
                    PortalSettings.DEFAULT);
            context.assertTrue(surface != null && surface.dimension().equals(World.OVERWORLD) && surface.facing() == PortalFacing.NORTH,
                    "Depuis le Level 0, la cible est la surface de l'overworld, sans demi-tour.");
            context.assertTrue(surface.position().x == anchor.getX() + 0.5 && surface.position().z == anchor.getZ() + 0.5,
                    "La cible est a l'aplomb du bloc.");
            context.assertTrue(PortalTargets.resolve(world, anchor, PortalFacing.SOUTH, PortalSettings.DEFAULT) == null,
                    "Sans Level 0 sur ce serveur, un portail de l'overworld n'a pas de cible.");
        } finally {
            GameTestPlayers.disconnect(player);
        }
        // Le bloc pose reste sans destination : il reessaie periodiquement, sans portail.
        context.runAtTick(BackroomsPortalBlockEntity.TARGET_RETRY_INTERVAL_TICKS + 10, () -> {
            BackroomsPortalBlockEntity block = (BackroomsPortalBlockEntity) world.getBlockEntity(anchor);
            context.assertTrue(block != null && !block.isConfigured() && block.entryPortal(FRONT) == null,
                    "Sans destination, pas de portail.");
            world.setBlockState(anchor, Blocks.AIR.getDefaultState());
            world.setBlockState(floor, Blocks.AIR.getDefaultState());
            world.setBlockState(blocked, Blocks.AIR.getDefaultState());
            world.setBlockState(blocked.up().east(), Blocks.AIR.getDefaultState());
            context.complete();
        });
    }

    /** Le joueur pose le bloc portail aux reglages donnes sur le dessus d'un bloc ; {@code true} si la pose est acceptee. */
    private static boolean place(ServerPlayerEntity player, BlockPos support, PortalSettings settings) {
        ItemStack stack = settings.toStack(new ItemStack(ModItems.BACKROOMS_PORTAL));
        player.setStackInHand(Hand.MAIN_HAND, stack);
        ItemPlacementContext placement = new ItemPlacementContext(player, Hand.MAIN_HAND, stack,
                new BlockHitResult(Vec3d.ofCenter(support).add(0.0, 0.5, 0.0), Direction.UP, support, false));
        return ModItems.BACKROOMS_PORTAL.place(placement).isAccepted();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 3000)
    public void aPortalToAMissingDimensionOnlyKeepsItsEntryFaces(TestContext context) {
        ServerWorld world = context.getWorld();
        // Sortie fugace vers une dimension absente : personne a y regarder, rien ne casse.
        BackroomsPortalBlockEntity block = placePortal(context, PORTAL, Direction.NORTH, new PortalSettings(1, 2, true, true, false, true));
        block.configureTarget(GameTestServerWorlds.nowhere(), new Vec3d(0.5, 65.0, 0.5), PortalFacing.SOUTH);
        // Un second bloc dont l'arrivee est a cent mille blocs, jamais generee : son chunk se charge sans bloquer le serveur,
        // et ses retours sont poses quand il est la. Le chunk ne tique pas ensuite, la verification ne tranche pas. Les
        // retours restent dans ce chunk decharge, hors d'atteinte : ils se retireront seuls, bloc disparu, a son prochain
        // chargement.
        BlockPos farPos = new BlockPos(4, 1, 1);
        BackroomsPortalBlockEntity far = placePortal(context, farPos, Direction.NORTH, PortalSettings.DEFAULT);
        far.configureTarget(World.OVERWORLD, new Vec3d(100_000.5, 70.0, 100_000.5), PortalFacing.SOUTH);
        context.assertTrue(far.returnPortal(FRONT) == null, "Arrivee pas encore chargee : le retour attend son chunk, sans bloquer.");
        UUID[] farReturn = new UUID[1];
        long[] posedAt = new long[1];
        int checks = BackroomsPortalBlockEntity.RETURN_CHECK_INTERVAL_TICKS * BackroomsPortalBlockEntity.RETURN_MISSING_CHECKS + 5;
        context.createTimedTaskRunner()
                .createAndAdd(() -> {
                    farReturn[0] = far.returnPortal(FRONT);
                    if (farReturn[0] == null) {
                        throw new GameTestException("Le chunk d'arrivee lointain n'est pas encore charge.");
                    }
                })
                .createAndAddReported(() -> {
                    context.assertTrue(portal(world, block.entryPortal(FRONT)) != null && portal(world, block.entryPortal(BACK)) != null,
                            "Les faces d'entree existent.");
                    context.assertTrue(block.returnPortal(FRONT) == null && block.returnPortal(BACK) == null, "Sans dimension d'arrivee, pas de retour.");
                    posedAt[0] = context.getTick();
                })
                .createAndAdd(() -> {
                    // Assez de verifications pour qu'un retour manquant soit repose, si le bloc en concluait quelque chose.
                    if (context.getTick() < posedAt[0] + checks) {
                        throw new GameTestException("Les verifications periodiques n'ont pas encore eu lieu.");
                    }
                })
                .createAndAddReported(() -> {
                    context.assertTrue(block.returnPortal(FRONT) == null, "Les verifications periodiques ne posent rien sans dimension d'arrivee.");
                    context.assertTrue(farReturn[0].equals(far.returnPortal(FRONT)), "Chunk d'arrivee inactif : le bloc n'en conclut rien.");
                    // Relu en aller simple avec un retour sauve vers cette dimension absente : il est oublie sans rien casser.
                    NbtCompound nbt = block.createNbt(world.getRegistryManager());
                    nbt.putBoolean("OneWay", true);
                    nbt.putUuid("ReturnFront", UUID.randomUUID());
                    block.read(nbt, world.getRegistryManager());
                })
                .createAndAdd(() -> {
                    if (block.returnPortal(FRONT) != null) {
                        throw new GameTestException("Le bloc relu n'a pas encore tique.");
                    }
                })
                .createAndAddReported(() -> {
                    context.setBlockState(PORTAL, Blocks.AIR);
                    context.setBlockState(farPos, Blocks.AIR);
                    context.assertTrue(world.getEntitiesByType(ModEntities.BACKROOMS_PORTAL, new Box(context.getAbsolutePos(PORTAL)).expand(3.0),
                            entity -> true).isEmpty(), "Le bloc retire emporte ses faces d'entree.");
                })
                .completeIfSuccessful();
    }

    @GameTest(templateName = GameTestPlayers.ARENA,
            tickLimit = BackroomsPortalBlockEntity.RETURN_CHECK_INTERVAL_TICKS * BackroomsPortalBlockEntity.RETURN_MISSING_CHECKS + 50)
    public void blockWaitsForItsChunkBeforeReplacingALostEntryFace(TestContext context) {
        ServerWorld world = context.getWorld();
        // Bloc recharge avec son chunk, une face avant qui n'existe plus (sauvegarde interrompue), sans face arriere ni retours
        // connus : comme au chargement, la block entity lit sa sauvegarde avant d'avoir un monde.
        UUID lost = UUID.randomUUID();
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", Registries.BLOCK_ENTITY_TYPE.getId(ModBlockEntities.BACKROOMS_PORTAL).toString());
        nbt.putUuid("EntryFront", lost);
        nbt.putString("DestDim", World.OVERWORLD.getValue().toString());
        Vec3d arrival = frameCenter(context, new BlockPos(6, 1, 6));
        nbt.putDouble("DestX", arrival.x);
        nbt.putDouble("DestY", arrival.y);
        nbt.putDouble("DestZ", arrival.z);
        nbt.putString("DestFacing", "SOUTH");
        context.setBlockState(PORTAL, ModBlocks.BACKROOMS_PORTAL.getDefaultState().with(BackroomsPortalBlock.FACING, Direction.NORTH));
        BlockPos anchor = context.getAbsolutePos(PORTAL);
        BackroomsPortalBlockEntity block = (BackroomsPortalBlockEntity) BlockEntity.createFromNbt(anchor, world.getBlockState(anchor), nbt,
                world.getRegistryManager());
        world.addBlockEntity(block);

        context.runAtTick(BackroomsPortalBlockEntity.PORTAL_GRACE_TICKS / 2, () -> {
            context.assertTrue(lost.equals(block.entryPortal(FRONT)), "Pendant le delai de grace, le bloc attend que son chunk livre ses entities.");
            context.assertTrue(portal(world, block.entryPortal(BACK)) != null, "La face arriere, jamais posee, l'est aussitot.");
        });
        context.runAtTick(BackroomsPortalBlockEntity.PORTAL_GRACE_TICKS + 20, () -> context.assertTrue(
                !lost.equals(block.entryPortal(FRONT)) && portal(world, block.entryPortal(FRONT)) != null,
                "Le delai passe, une nouvelle face avant est posee."));
        // Sans retour connu, le bloc en repose un apres plusieurs verifications espacees.
        context.runAtTick(BackroomsPortalBlockEntity.RETURN_CHECK_INTERVAL_TICKS + 5,
                () -> context.assertTrue(block.returnPortal(FRONT) == null, "Une seule verification sans retour ne suffit pas."));
        context.runAtTick(BackroomsPortalBlockEntity.RETURN_CHECK_INTERVAL_TICKS * BackroomsPortalBlockEntity.RETURN_MISSING_CHECKS + 5, () -> {
            try {
                context.assertTrue(portal(world, block.returnPortal(FRONT)) != null && portal(world, block.returnPortal(BACK)) != null,
                        "Les retours manquants sont reposes.");
            } finally {
                context.setBlockState(PORTAL, Blocks.AIR);
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA,
            tickLimit = BackroomsPortalBlockEntity.RETURN_CHECK_INTERVAL_TICKS * (BackroomsPortalBlockEntity.RETURN_MISSING_CHECKS + 1) + 100)
    public void blockReplacesAReturnFaceThatDisappeared(TestContext context) {
        ServerWorld world = context.getWorld();
        BackroomsPortalBlockEntity block = placePortal(context, PORTAL, Direction.NORTH, PortalSettings.DEFAULT);
        block.configureTarget(World.OVERWORLD, frameCenter(context, new BlockPos(6, 1, 6)), PortalFacing.SOUTH);
        UUID[] original = new UUID[1];
        context.runAtTick(5, () -> {
            original[0] = block.returnPortal(BACK);
            context.assertTrue(portal(world, original[0]) != null, "La face arriere du retour est posee.");
        });
        context.runAtTick(BackroomsPortalBlockEntity.RETURN_CHECK_INTERVAL_TICKS + 5, () -> {
            context.assertTrue(original[0].equals(block.returnPortal(BACK)), "Presente a la premiere verification, elle est gardee.");
            world.getEntity(original[0]).discard();
        });
        context.runAtTick(BackroomsPortalBlockEntity.RETURN_CHECK_INTERVAL_TICKS * (BackroomsPortalBlockEntity.RETURN_MISSING_CHECKS + 1) + 50, () -> {
            try {
                UUID replaced = block.returnPortal(BACK);
                context.assertTrue(replaced != null && !replaced.equals(original[0]) && world.getEntity(replaced) != null,
                        "Apres assez de verifications sans elle, le bloc la repose.");
            } finally {
                context.setBlockState(PORTAL, Blocks.AIR);
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 400, batchId = "portal_level_zero_stand_in")
    public void blockResolvesItsLevelZeroTargetWhenTheDimensionExists(TestContext context) {
        ServerWorld world = context.getWorld();
        MinecraftServer server = world.getServer();
        ServerWorld standIn = GameTestServerWorlds.nether(context);
        GameTestServerWorlds.registerStandInLevelZero(server, standIn);
        // L'arrivee se calcule d'avance : son chunk est force, les retours y sont suivis et partent avec le bloc.
        PortalTargets.Target target = PortalTargets.resolve(world, context.getAbsolutePos(PORTAL), PortalFacing.NORTH, PortalSettings.DEFAULT);
        if (target == null) {
            throw new GameTestException("Le Level 0 prete n'offre aucune arrivee.");
        }
        Vec3d arrival = target.position();
        GameTestServerWorlds.forceChunk(standIn, arrival, true);
        BackroomsPortalBlockEntity[] block = new BackroomsPortalBlockEntity[1];
        UUID[] returnPortal = new UUID[1];
        context.createTimedTaskRunner()
                .createAndAdd(() -> {
                    if (!standIn.shouldTickEntity(BlockPos.ofFloored(arrival))) {
                        throw new GameTestException("Le chunk d'arrivee n'est pas encore actif.");
                    }
                    block[0] = placePortal(context, PORTAL, Direction.NORTH, PortalSettings.DEFAULT);
                })
                .createAndAdd(() -> {
                    context.assertTrue(block[0].isConfigured(), "Le bloc a resolu sa destination dans le Level 0.");
                    context.assertTrue(portal(world, block[0].entryPortal(FRONT)) != null, "La face avant d'entree est posee.");
                    returnPortal[0] = block[0].returnPortal(FRONT);
                    context.assertTrue(returnPortal[0] != null && standIn.getEntity(returnPortal[0]) != null,
                            "Le retour est pose et suivi dans la dimension d'arrivee.");
                    context.setBlockState(PORTAL, Blocks.AIR);
                })
                .createAndAddReported(() -> context.assertTrue(standIn.getEntity(returnPortal[0]) == null, "Le bloc casse emporte ses retours."))
                .completeIfSuccessful();
    }

    /** Apres le lot du Level 0 prete, reussi ou non : plus de Level 0, plus de chunk force dans le Nether. */
    @AfterBatch(batchId = "portal_level_zero_stand_in")
    public void afterLevelZeroStandIn(ServerWorld world) {
        GameTestServerWorlds.removeStandInLevelZero(world.getServer());
        GameTestServerWorlds.releaseForcedChunks(world.getServer().getWorld(World.NETHER));
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 40, batchId = "portal_disabled")
    public void withPortalsDisabledTheBlockStaysInert(TestContext context) {
        MinecraftServer server = context.getWorld().getServer();
        // Avec un Level 0, un bloc actif resoudrait sa destination des son premier tick.
        GameTestServerWorlds.registerStandInLevelZero(server, GameTestServerWorlds.nether(context));
        BackroomsPortalBlockEntity.setPortalsEnabled(false);
        BackroomsPortalBlockEntity block = placePortal(context, PORTAL, Direction.NORTH, PortalSettings.DEFAULT);
        context.runAtTick(5, () -> {
            try {
                context.assertTrue(!block.isConfigured() && block.entryPortal(FRONT) == null && block.returnPortal(FRONT) == null,
                        "Portails desactives : le bloc ne cherche pas de destination et ne pose aucun portail.");
            } finally {
                context.setBlockState(PORTAL, Blocks.AIR);
            }
            context.complete();
        });
    }

    /** Apres le lot sans portails, reussi ou non : portails rendus a leur configuration, plus de Level 0 prete. */
    @AfterBatch(batchId = "portal_disabled")
    public void afterPortalsDisabled(ServerWorld world) {
        BackroomsPortalBlockEntity.setPortalsEnabled(ModConfig.ENABLE_PORTAL_FEATURE);
        GameTestServerWorlds.removeStandInLevelZero(world.getServer());
    }
}
