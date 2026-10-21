package com.gtgpuke.gtgsbackrooms.gametest.core.portal;

import net.minecraft.block.Blocks;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;

import com.gtgpuke.gtgsbackrooms.core.portal.block.BackroomsPortalBlock;
import com.gtgpuke.gtgsbackrooms.core.portal.block.BackroomsPortalBlockEntity;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalArea;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalSettings;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.core.portal.transition.TeleportExecutor;
import com.gtgpuke.gtgsbackrooms.core.registry.ModBlocks;
import com.gtgpuke.gtgsbackrooms.core.registry.ModDataComponents;
import com.gtgpuke.gtgsbackrooms.core.registry.ModItems;
import com.gtgpuke.gtgsbackrooms.gametest.support.GameTestPlayers;

/**
 * Tests en jeu des portails d'une orientation a une autre, les 36 paires : debout, au sol, au plafond, dans tous les
 * sens. Un joueur qui traverse chaque paire file hors du cadre d'arrivee, regarde ou il va, garde sa chute seulement si le
 * bas reste le bas, et ressort du bon cote, dans l'ouverture ; un objet traverse seul chacune des six orientations ; une
 * longue chute renvoyee a l'horizontale ne blesse pas a l'atterrissage, une chute qui continue si ; un portail couche n'a
 * que sa face avant.
 */
public final class PortalOrientationGameTests implements FabricGameTest {

    private static final double EPSILON = 1.0e-3;
    private static final Vec3d ENTRY = new Vec3d(6.5, 4.0, 6.5);
    private static final Vec3d ARRIVAL = new Vec3d(16.5, 4.0, 16.5);
    /** Chute du joueur au moment de traverser, en blocs. */
    private static final float FALL = 12.0f;
    private static final double SPEED = 0.5;
    /** Vitesse des objets qui traversent seuls, en blocs par tick. */
    private static final double ITEM_SPEED = 0.3;
    /** Ticks laisses a un joueur tout juste connecte pour perdre son invulnerabilite d'arrivee (soixante en vanilla). */
    private static final int JOIN_INVULNERABILITY_TICKS = 61;
    /** Une chute de vingt blocs : dix-sept points de degats, sous les vingt points de vie d'un joueur. */
    private static final float LONG_FALL = 20.0f;
    private static final float FULL_HEALTH = 20.0f;

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 40)
    public void everyPairOfOrientationsCarriesAPlayerOutOfTheArrivalFrame(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d entry = context.getAbsolute(ENTRY);
        Vec3d arrival = context.getAbsolute(ARRIVAL);
        ServerPlayerEntity player = GameTestPlayers.connect(context, GameMode.CREATIVE, entry, 0.0f, 0.0f);
        try {
            for (PortalFacing source : PortalFacing.values()) {
                for (PortalFacing exit : PortalFacing.values()) {
                    crossAndCheck(context, player, frame(world, entry, source, exit, arrival), arrival);
                }
            }
        } finally {
            GameTestPlayers.disconnect(player);
        }
        context.complete();
    }

    /**
     * Le joueur vient de faire passer le plan au point qui decide (ses pieds, ses yeux pour un cadre au sol), au centre du
     * cadre, debout les pieds au bas du cadre ; il file a travers, regarde ou il va et a une chute en cours.
     */
    private static void crossAndCheck(TestContext context, ServerPlayerEntity player, BackroomsPortalEntity portal, Vec3d arrival) {
        PortalFacing source = portal.facingSource();
        PortalFacing exit = portal.facingDestination();
        String pair = source + " vers " + exit;
        Vec3d motion = source.normal.multiply(-SPEED);
        Vec3d decision = portal.getPos().subtract(source.normal.multiply(0.05));
        double feetY = source.normal.y == 0.0 ? portal.getY() - 1.0 : decision.y - portal.crossingHeight(player);
        float yaw = (float) Math.toDegrees(Math.atan2(-motion.x, motion.z));
        float pitch = (float) Math.toDegrees(-Math.asin(motion.y / SPEED));
        player.refreshPositionAndAngles(decision.x, feetY, decision.z, yaw, pitch);
        player.setVelocity(motion);
        player.fallDistance = FALL;
        context.assertTrue(TeleportExecutor.execute(player, portal), pair + " : la traversee a lieu.");
        context.assertTrue(player.getVelocity().subtract(exit.normal.multiply(SPEED)).length() < EPSILON,
                pair + " : on file hors du cadre d'arrivee : " + player.getVelocity() + ".");
        context.assertTrue(player.getRotationVector().dotProduct(exit.normal) > 0.999, pair + " : on regarde ou l'on va.");
        boolean keepsFalling = source.normal.y == 0.0 && exit.normal.y == 0.0 || source.normal.y * exit.normal.y < 0.0;
        context.assertTrue(player.fallDistance == (keepsFalling ? FALL : 0.0f),
                pair + " : chute gardee seulement si le bas reste le bas : " + player.fallDistance + ".");
        double feet = player.getY();
        double top = feet + player.getHeight();
        if (exit == PortalFacing.DOWN) {
            context.assertTrue(top <= arrival.y, pair + " : tout le corps pend sous le plafond : " + player.getPos() + ".");
        } else if (exit == PortalFacing.UP) {
            context.assertTrue(feet >= arrival.y, pair + " : tout le corps est au-dessus du sol : " + player.getPos() + ".");
        } else {
            context.assertTrue(player.getPos().subtract(arrival).dotProduct(exit.normal) > 0.0 && feet >= arrival.y - 1.0 - EPSILON
                    && top <= arrival.y + 1.0 + EPSILON, pair + " : debout devant le cadre, dans sa hauteur : " + player.getPos() + ".");
        }
        context.assertTrue(portal.withinArrivalWidth(player.getX(), player.getY(), player.getZ()),
                pair + " : en face de l'ouverture d'arrivee : " + player.getPos() + ".");
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 60)
    public void anObjectCrossesEveryOrientationOnItsOwn(TestContext context) {
        ServerWorld world = context.getWorld();
        PortalFacing[] entries = PortalFacing.values();
        PortalFacing[] exits = {PortalFacing.UP, PortalFacing.DOWN, PortalFacing.WEST, PortalFacing.NORTH, PortalFacing.EAST, PortalFacing.SOUTH};
        BackroomsPortalEntity[] portals = new BackroomsPortalEntity[entries.length];
        ItemEntity[] items = new ItemEntity[entries.length];
        for (int i = 0; i < entries.length; i++) {
            Vec3d entry = context.getAbsolute(new Vec3d(2.5 + 3.5 * i, 4.0, 4.5));
            portals[i] = frame(world, entry, entries[i], exits[i], context.getAbsolute(new Vec3d(2.5 + 3.5 * i, 4.0, 16.5)));
            world.spawnEntity(portals[i]);
            // L'objet part a six dixiemes du plan, son point de traversee au centre du cadre, et file droit dedans.
            Vec3d start = entry.add(entries[i].normal.multiply(0.6));
            items[i] = new ItemEntity(world, start.x, 0.0, start.z, new ItemStack(Items.FEATHER));
            double startY = entries[i].normal.y == 0.0 ? entry.y - 0.5 : start.y - portals[i].crossingHeight(items[i]);
            items[i].setPosition(start.x, startY, start.z);
            items[i].setNoGravity(true);
            items[i].setPickupDelayInfinite();
            items[i].setVelocity(entries[i].normal.multiply(-ITEM_SPEED));
            world.spawnEntity(items[i]);
        }
        context.runAtTick(12, () -> {
            try {
                for (int i = 0; i < entries.length; i++) {
                    BackroomsPortalEntity portal = portals[i];
                    ItemEntity item = items[i];
                    String pair = entries[i] + " vers " + exits[i];
                    Vec3d ahead = item.getPos().subtract(portal.destinationPos());
                    context.assertTrue(item.hasPortalCooldown() && ahead.dotProduct(exits[i].normal) > 0.0,
                            pair + " : l'objet a traverse seul et file devant l'arrivee : " + item.getPos() + ".");
                    // L'air freine un objet de deux pour cent par tick : seule la direction se compare.
                    context.assertTrue(item.getVelocity().normalize().dotProduct(exits[i].normal) > 0.999,
                            pair + " : sa vitesse est transposee : " + item.getVelocity() + ".");
                    context.assertTrue(portal.withinArrivalWidth(item.getX(), item.getY(), item.getZ()), pair + " : en face de l'ouverture d'arrivee.");
                }
            } finally {
                for (int i = 0; i < entries.length; i++) {
                    items[i].discard();
                    portals[i].discard();
                }
            }
            context.complete();
        });
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 200)
    public void aLongFallTurnedSidewaysDoesNotHurtButAFallThatGoesOnDoes(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d entry = context.getAbsolute(ENTRY);
        Vec3d arrival = context.getAbsolute(ARRIVAL);
        BackroomsPortalEntity sideways = frame(world, entry, PortalFacing.UP, PortalFacing.NORTH, arrival);
        BackroomsPortalEntity onwards = frame(world, entry, PortalFacing.UP, PortalFacing.DOWN, arrival.add(-6.0, 0.0, 0.0));
        ServerPlayerEntity turned = GameTestPlayers.connect(context, GameMode.SURVIVAL, entry, 0.0f, 0.0f);
        ServerPlayerEntity falling = GameTestPlayers.connect(context, GameMode.SURVIVAL, entry, 0.0f, 0.0f);
        int[] ticks = {0};
        context.createTimedTaskRunner()
                // Un joueur tout juste connecte ne prend aucun degat pendant soixante ticks : on les laisse passer.
                .createAndAdd(() -> {
                    GameTestPlayers.tick(turned);
                    GameTestPlayers.tick(falling);
                    context.assertTrue(++ticks[0] >= JOIN_INVULNERABILITY_TICKS, "Les joueurs sont encore invulnerables.");
                })
                .createAndAddReported(() -> {
                    try {
                        land(context, turned, sideways);
                        land(context, falling, onwards);
                        context.assertTrue(turned.getHealth() == FULL_HEALTH,
                                "Tombe de vingt blocs dans un sol, sorti d'un mur a l'horizontale : aucun degat a l'atterrissage (" + turned.getHealth() + ").");
                        context.assertTrue(falling.getHealth() < FULL_HEALTH && falling.isAlive(),
                                "Tombe de vingt blocs dans un sol, sorti d'un plafond : il tombe encore, et l'atterrissage blesse (" + falling.getHealth() + ").");
                    } finally {
                        GameTestPlayers.disconnect(turned);
                        GameTestPlayers.disconnect(falling);
                    }
                })
                .completeIfSuccessful();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 60)
    public void aSneakingPlayerDescendsThroughAFloorFrameStandingPlayersStay(TestContext context) {
        ServerWorld world = context.getWorld();
        // Deux cadres au sol, le plan au milieu du bloc au-dessus du sol plein : on s'y tient, les pieds un demi-bloc dessous.
        Vec3d sneakingFrame = context.getAbsolute(new Vec3d(4.5, 1.5, 4.0));
        Vec3d standingFrame = context.getAbsolute(new Vec3d(10.5, 1.5, 4.0));
        Vec3d arrival = context.getAbsolute(ARRIVAL);
        BackroomsPortalEntity descent = frame(world, sneakingFrame, PortalFacing.UP, PortalFacing.SOUTH, arrival);
        BackroomsPortalEntity stay = frame(world, standingFrame, PortalFacing.UP, PortalFacing.SOUTH, arrival.add(-6.0, 0.0, 0.0));
        world.spawnEntity(descent);
        world.spawnEntity(stay);
        ServerPlayerEntity sneaking = GameTestPlayers.connect(context, GameMode.CREATIVE, sneakingFrame.subtract(0.0, 0.5, 0.0), 0.0f, 0.0f);
        ServerPlayerEntity standing = GameTestPlayers.connect(context, GameMode.CREATIVE, standingFrame.subtract(0.0, 0.5, 0.0), 0.0f, 0.0f);
        sneaking.setSneaking(true);
        try {
            context.assertTrue(TeleportExecutor.descendsThrough(descent, sneaking), "Accroupi au sol sur le cadre : il s'y laisse descendre.");
            context.assertTrue(!TeleportExecutor.descendsThrough(stay, standing), "Debout, il reste.");
            sneaking.setOnGround(false);
            context.assertTrue(!TeleportExecutor.descendsThrough(descent, sneaking), "En l'air, il tombera de lui-meme s'il y a de la place.");
            sneaking.setOnGround(true);
            sneaking.setPosition(sneakingFrame.x, sneakingFrame.y + 0.2, sneakingFrame.z);
            context.assertTrue(!TeleportExecutor.descendsThrough(descent, sneaking), "Les pieds au-dessus du plan : il n'est pas dans le cadre.");
            sneaking.setPosition(sneakingFrame.x, sneakingFrame.y - 0.8, sneakingFrame.z);
            context.assertTrue(!TeleportExecutor.descendsThrough(descent, sneaking), "Les pieds trop bas : ce n'est pas le sol du cadre.");
            sneaking.setPosition(sneakingFrame.x, sneakingFrame.y - 0.5, sneakingFrame.z + 1.3);
            context.assertTrue(!TeleportExecutor.descendsThrough(descent, sneaking), "A cote de l'ouverture.");
            BackroomsPortalEntity wall = frame(world, sneakingFrame, PortalFacing.NORTH, PortalFacing.UP, arrival);
            sneaking.setPosition(sneakingFrame.x, sneakingFrame.y - 0.5, sneakingFrame.z);
            context.assertTrue(!TeleportExecutor.descendsThrough(wall, sneaking), "Seul un cadre au sol se descend.");
        } catch (RuntimeException failure) {
            GameTestPlayers.disconnect(sneaking);
            GameTestPlayers.disconnect(standing);
            descent.discard();
            stay.discard();
            throw failure;
        }
        // Le serveur, faute d'annonce du client, fait lui-meme descendre le joueur accroupi, cinq ticks plus tard.
        context.runAtTick(15, () -> {
            try {
                context.assertTrue(sneaking.getPos().squaredDistanceTo(arrival) < 4.0,
                        "Accroupi sur le cadre au sol, le joueur est passe de l'autre cote : " + sneaking.getPos() + ".");
                context.assertTrue(standing.getPos().squaredDistanceTo(standingFrame) < 1.0, "Debout, l'autre est reste : " + standing.getPos() + ".");
            } finally {
                GameTestPlayers.disconnect(sneaking);
                GameTestPlayers.disconnect(standing);
                descent.discard();
                stay.discard();
            }
            context.complete();
        });
    }

    /** Le joueur, en pleine chute de vingt blocs, passe les yeux par le cadre au sol, puis touche terre de l'autre cote. */
    private static void land(TestContext context, ServerPlayerEntity player, BackroomsPortalEntity portal) {
        Vec3d decision = portal.getPos().subtract(0.0, 0.05, 0.0);
        player.refreshPositionAndAngles(decision.x, decision.y - portal.crossingHeight(player), decision.z, 0.0f, 90.0f);
        player.setVelocity(0.0, -1.5, 0.0);
        player.fallDistance = LONG_FALL;
        context.assertTrue(TeleportExecutor.execute(player, portal), "La traversee a lieu.");
        player.handleFall(0.0, -0.1, 0.0, true);
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = 40)
    public void aLyingFrameAtTheEntryOrTheArrivalHasOnlyItsFrontFace(TestContext context) {
        ServerWorld world = context.getWorld();
        int front = BackroomsPortalBlockEntity.FRONT;
        int back = BackroomsPortalBlockEntity.BACK;
        BlockPos floorPos = new BlockPos(3, 4, 3);
        BlockPos wallToFloorPos = new BlockPos(9, 1, 3);
        BlockPos wallPos = new BlockPos(15, 1, 3);
        // Un bloc couche au sol, vers un plafond ; un bloc debout, vers un sol ; un bloc debout, vers un mur. Deux faces reglees.
        BackroomsPortalBlockEntity floor = place(context, floorPos, Direction.UP);
        BackroomsPortalBlockEntity wallToFloor = place(context, wallToFloorPos, Direction.NORTH);
        BackroomsPortalBlockEntity wall = place(context, wallPos, Direction.NORTH);
        floor.configureTarget(World.OVERWORLD, context.getAbsolute(new Vec3d(3.5, 6.5, 15.0)), PortalFacing.DOWN);
        wallToFloor.configureTarget(World.OVERWORLD, context.getAbsolute(new Vec3d(9.5, 1.5, 15.0)), PortalFacing.UP);
        wall.configureTarget(World.OVERWORLD, PortalArea.center(context.getAbsolutePos(new BlockPos(15, 1, 15)), PortalFacing.SOUTH,
                PortalSettings.DEFAULT), PortalFacing.SOUTH);
        try {
            context.assertTrue(floor.entryPortal(front) != null && floor.returnPortal(front) != null
                    && floor.entryPortal(back) == null && floor.returnPortal(back) == null,
                    "Couche au sol : la face avant seule, sans retour arriere (de dos, on viendrait du sol).");
            context.assertTrue(wallToFloor.entryPortal(front) != null && wallToFloor.returnPortal(front) != null
                    && wallToFloor.entryPortal(back) == null && wallToFloor.returnPortal(back) == null,
                    "Debout vers un sol : la face avant seule (la face arriere ressortirait sous le sol).");
            context.assertTrue(wall.entryPortal(back) != null && wall.returnPortal(back) != null,
                    "Debout vers un mur : les deux faces, comme reglees.");
        } finally {
            context.setBlockState(floorPos, Blocks.AIR);
            context.setBlockState(wallToFloorPos, Blocks.AIR);
            context.setBlockState(wallPos, Blocks.AIR);
        }
        context.complete();
    }

    private static BackroomsPortalBlockEntity place(TestContext context, BlockPos pos, Direction front) {
        context.setBlockState(pos, ModBlocks.BACKROOMS_PORTAL.getDefaultState().with(BackroomsPortalBlock.FACING, front));
        BackroomsPortalBlockEntity block = context.getBlockEntity(pos);
        block.readComponents(wand(PortalSettings.DEFAULT));
        return block;
    }

    /** Portail d'un bloc sur deux, pas encore dans le monde, d'une orientation a une autre, dans le monde du test. */
    private static BackroomsPortalEntity frame(ServerWorld world, Vec3d center, PortalFacing source, PortalFacing exit, Vec3d arrival) {
        return BackroomsPortalEntity.builder(world)
                .position(center)
                .facingSource(source)
                .facingDestination(exit)
                .destinationDim(World.OVERWORLD)
                .destinationPos(arrival)
                .size(1.0f, 2.0f)
                .build();
    }

    /** Baguette de portail portant ces reglages. */
    private static ItemStack wand(PortalSettings settings) {
        ItemStack wand = new ItemStack(ModItems.PORTAL_WAND);
        wand.set(ModDataComponents.PORTAL_SETTINGS, settings);
        return wand;
    }
}
