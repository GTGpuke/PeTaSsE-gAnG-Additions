package com.gtgpuke.gtgsbackrooms.gametest.backrooms.portal;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.AfterBatch;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.block.BackroomsPortalBlock;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.block.BackroomsPortalBlockEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.block.PortalArea;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.block.PortalSettings;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntityRegistry;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.light.PortalLightTransmission;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.light.VirtualLightSources;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.light.VirtualLightView;
import com.gtgpuke.gtgsbackrooms.gametest.support.GameTestPlayers;
import com.gtgpuke.gtgsbackrooms.gametest.support.GameTestServerWorlds;
import com.gtgpuke.gtgsbackrooms.init.ModBlocks;
import com.gtgpuke.gtgsbackrooms.init.ModItems;

/**
 * Tests en jeu de la lumiere transmise par les portails : seuls les portails ouverts, visibles et regles pour la passer
 * eclairent l'autre cote, d'un niveau de moins ; la plus forte de deux lumieres l'emporte ; une source pas encore
 * appliquee est signalee de nouveau ; le cadre d'un portail qui apparait est reverifie, et une lumiere restee sans sa
 * source s'en va ; et une lumiere retiree s'eteint des deux cotes d'un portail double face.
 * Chaque test a son lot : les sources virtuelles sont celles du monde entier. Les cadres sont loin du coin de l'arene,
 * que la balise d'un test d'un lot precedent peut eclairer, et de toute lumiere qui ne vient pas d'eux.
 */
public final class PortalLightGameTests implements FabricGameTest {

    /** Patience, en ticks, pour que le moteur de lumiere applique une source ou l'eteigne. */
    private static final int LIGHT_PATIENCE_TICKS = 400;
    /** Bloc portail du lot double face, retire apres son lot : il reposerait ses portails. */
    private static @Nullable BlockPos lightBlockAnchor;

    private static int blockLight(ServerWorld world, BlockPos pos) {
        return world.getLightLevel(LightType.BLOCK, pos);
    }

    /** Portail d'un bloc sur deux, face au nord, vers une arrivee face au sud ; la lumiere passe sauf reglage contraire. */
    private static BackroomsPortalEntity.Builder northPortal(ServerWorld world, Vec3d center, RegistryKey<World> destinationDim, Vec3d destination) {
        return BackroomsPortalEntity.builder(world)
                .position(center)
                .facingSource(PortalFacing.NORTH)
                .facingDestination(PortalFacing.SOUTH)
                .destinationDim(destinationDim)
                .destinationPos(destination)
                .size(1.0f, 2.0f)
                .transmitsLight(true);
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = LIGHT_PATIENCE_TICKS * 2, batchId = "portal_light")
    public void onlyOpenVisiblePortalsSetToTransmitLightPassIt(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d entry = context.getAbsolute(new Vec3d(8.5, 2.0, 8.5));
        BlockPos entryLow = context.getAbsolutePos(new BlockPos(8, 1, 8));
        BlockPos entryHigh = entryLow.up();
        // Deux arrivees eclairees : la plus proche de sa lumiere, la plus forte, l'emporte.
        Vec3d dim = context.getAbsolute(new Vec3d(20.5, 2.0, 20.5));
        Vec3d bright = context.getAbsolute(new Vec3d(16.5, 2.0, 20.5));
        BlockPos dimLow = BlockPos.ofFloored(dim).down();
        BlockPos brightLow = BlockPos.ofFloored(bright).down();
        BlockPos dimLight = dimLow.south(2);
        BlockPos brightLight = brightLow.south();
        world.setBlockState(dimLight, Blocks.GLOWSTONE.getDefaultState());
        world.setBlockState(brightLight, Blocks.GLOWSTONE.getDefaultState());
        Vec3d[] shut = new Vec3d[5];
        for (int i = 0; i < shut.length; i++) {
            shut[i] = context.getAbsolute(new Vec3d(21.5 + (i % 2) * 2.0, 2.0, 2.5 + i));
        }
        List<BackroomsPortalEntity> portals = new ArrayList<>();
        int[] expected = new int[2];
        context.createTimedTaskRunner()
                .createAndAdd(() -> {
                    if (blockLight(world, brightLow) < 14 || blockLight(world, dimLow) < 13) {
                        throw new GameTestException("Les arrivees ne sont pas encore eclairees.");
                    }
                    // Faible, forte, faible : la plus forte remplace la premiere et resiste a la derniere, quel que soit l'ordre.
                    portals.add(northPortal(world, entry, World.OVERWORLD, dim).build());
                    portals.add(northPortal(world, entry, World.OVERWORLD, bright).build());
                    portals.add(northPortal(world, entry, World.OVERWORLD, dim).build());
                    // Des portails qui ne laissent rien passer, vers l'arrivee la plus eclairee, hors de portee de toute lumiere.
                    portals.add(northPortal(world, shut[0], World.OVERWORLD, bright).teleportable(false).build());
                    portals.add(northPortal(world, shut[1], World.OVERWORLD, bright).visible(false).build());
                    portals.add(northPortal(world, shut[2], World.OVERWORLD, bright).transmitsLight(false).build());
                    portals.add(northPortal(world, shut[3], GameTestServerWorlds.nowhere(), bright).build());
                    // Vers un coin sans lumiere : rien a transmettre.
                    portals.add(northPortal(world, shut[4], World.OVERWORLD, context.getAbsolute(new Vec3d(23.5, 2.0, 11.5))).build());
                    portals.forEach(world::spawnEntity);
                    // Deja comptes par une passe, leurs cadres ne sont plus reverifies : seul le re-signalement des sources
                    // pas encore appliquees peut eclairer celui-ci.
                    portals.forEach(BackroomsPortalEntity::takeLightRefresh);
                    expected[0] = Math.max(blockLight(world, dimLow), blockLight(world, brightLow)) - 1;
                    expected[1] = Math.max(blockLight(world, dimLow.up()), blockLight(world, brightLow.up())) - 1;
                    // Les sources sont publiees telles que la passe les calculera, sans etre signalees au moteur : la passe
                    // les trouve inchangees mais pas appliquees, et les lui signale.
                    ((VirtualLightView) world).backrooms$setVirtualLight(new VirtualLightSources(
                            new long[] {entryLow.asLong(), entryHigh.asLong()}, new byte[] {(byte) expected[0], (byte) expected[1]}, 2));
                    PortalLightTransmission.SERVER.update(world, BackroomsPortalEntityRegistry.all());
                })
                .createAndAdd(() -> {
                    if (blockLight(world, entryLow) != expected[0] || blockLight(world, entryHigh) != expected[1]) {
                        throw new GameTestException("Le cadre n'est pas encore eclaire par l'autre cote : " + blockLight(world, entryLow) + ".");
                    }
                })
                .createAndAddReported(() -> {
                    context.assertTrue(blockLight(world, entryLow.north()) == expected[0] - 1, "La lumiere se propage devant le cadre.");
                    for (Vec3d center : shut) {
                        BlockPos cell = BlockPos.ofFloored(center).down();
                        context.assertTrue(blockLight(world, cell) == 0,
                                "Ferme, invisible, regle sans lumiere, sans arrivee ou vers le noir : rien ne passe en " + cell + ".");
                    }
                    world.setBlockState(dimLight, Blocks.AIR.getDefaultState());
                    world.setBlockState(brightLight, Blocks.AIR.getDefaultState());
                })
                .createAndAdd(() -> {
                    if (blockLight(world, entryLow) != 0 || blockLight(world, entryHigh) != 0) {
                        throw new GameTestException("Le cadre n'est pas encore eteint.");
                    }
                })
                .createAndAddReported(() -> context.assertTrue(((VirtualLightView) world).backrooms$virtualLight().size() == 0,
                        "Plus aucune source virtuelle."))
                .completeIfSuccessful();
    }

    /** Apres le lot, reussi ou non : ses portails ne transmettent plus rien au lot suivant, qui compte les sources du monde. */
    @AfterBatch(batchId = "portal_light")
    public void afterLight(ServerWorld world) {
        discardPortals();
    }

    /** Apres le lot du bloc double face, reussi ou non : de meme, et le bloc, qui les reposerait, est retire. */
    @AfterBatch(batchId = "portal_light_block")
    public void afterLightBlock(ServerWorld world) {
        if (lightBlockAnchor != null) {
            world.setBlockState(lightBlockAnchor, Blocks.AIR.getDefaultState());
            lightBlockAnchor = null;
        }
        discardPortals();
    }

    /** Apres le lot du cadre reverifie, reussi ou non : de meme. */
    @AfterBatch(batchId = "portal_light_refresh")
    public void afterLightRefresh(ServerWorld world) {
        discardPortals();
    }

    /** Retire les portails encore charges ; la passe suivante de la lumiere retire leurs sources. */
    private static void discardPortals() {
        new ArrayList<>(BackroomsPortalEntityRegistry.all()).forEach(BackroomsPortalEntity::discard);
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = LIGHT_PATIENCE_TICKS * 2, batchId = "portal_light_block")
    public void aDoubleFacedPortalLightsBothSidesAndFadesWithItsLight(TestContext context) {
        ServerWorld world = context.getWorld();
        PortalSettings settings = new PortalSettings(4, 4, true);
        BlockPos anchor = new BlockPos(10, 1, 8);
        context.setBlockState(anchor, ModBlocks.BACKROOMS_PORTAL.getDefaultState().with(BackroomsPortalBlock.FACING, Direction.NORTH));
        lightBlockAnchor = context.getAbsolutePos(anchor);
        BackroomsPortalBlockEntity block = context.getBlockEntity(anchor);
        block.readComponents(settings.toStack(new ItemStack(ModItems.BACKROOMS_PORTAL)));
        BlockPos arrivalAnchor = context.getAbsolutePos(new BlockPos(18, 1, 20));
        block.configureTarget(World.OVERWORLD, PortalArea.center(Vec3d.ofBottomCenter(arrivalAnchor), PortalFacing.SOUTH, settings), PortalFacing.SOUTH);
        // Deux lumieres derriere le cadre d'arrivee : chacun de ses seize blocs recoit au moins dix niveaux, ceux de l'entree neuf.
        BlockPos[] lights = {arrivalAnchor.add(0, 1, 1), arrivalAnchor.add(1, 2, 1)};
        for (BlockPos light : lights) {
            world.setBlockState(light, Blocks.GLOWSTONE.getDefaultState());
        }
        // Le cadre d'entree s'etend vers l'ouest (axe de largeur du nord), celui d'arrivee vers l'est.
        BlockPos entryCorner = context.getAbsolutePos(anchor).add(-2, 0, 0);
        BlockPos arrivalCorner = arrivalAnchor.add(-1, 0, 0);
        context.createTimedTaskRunner()
                .createAndAdd(() -> {
                    for (int x = 0; x < 4; x++) {
                        for (int y = 0; y < 4; y++) {
                            if (blockLight(world, entryCorner.add(x, y, 0)) < 9) {
                                throw new GameTestException("Le cadre d'entree n'est pas encore eclaire en " + entryCorner.add(x, y, 0) + ".");
                            }
                        }
                    }
                    // Les seize blocs de chaque cote deviennent des sources : l'arrivee recoit a son tour la lumiere de l'entree.
                    int sources = ((VirtualLightView) world).backrooms$virtualLight().size();
                    if (sources != 32) {
                        throw new GameTestException("L'arrivee n'est pas encore eclairee par l'entree : " + sources + " sources.");
                    }
                    for (BlockPos light : lights) {
                        world.setBlockState(light, Blocks.AIR.getDefaultState());
                    }
                })
                .createAndAdd(() -> {
                    for (int x = 0; x < 4; x++) {
                        for (int y = 0; y < 4; y++) {
                            if (blockLight(world, entryCorner.add(x, y, 0)) != 0 || blockLight(world, arrivalCorner.add(x, y, 0)) != 0) {
                                throw new GameTestException("La lumiere ne s'est pas encore eteinte des deux cotes.");
                            }
                        }
                    }
                })
                .completeIfSuccessful();
    }

    @GameTest(templateName = GameTestPlayers.ARENA, tickLimit = LIGHT_PATIENCE_TICKS * 3, batchId = "portal_light_refresh")
    public void aPortalThatAppearsRefreshesTheLightOfItsFrame(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d entry = context.getAbsolute(new Vec3d(8.5, 2.0, 8.5));
        BlockPos cell = context.getAbsolutePos(new BlockPos(8, 1, 8));
        Vec3d bright = context.getAbsolute(new Vec3d(16.5, 2.0, 20.5));
        BlockPos light = BlockPos.ofFloored(bright).down().south();
        world.setBlockState(light, Blocks.GLOWSTONE.getDefaultState());
        BackroomsPortalEntity lit = northPortal(world, entry, World.OVERWORLD, bright).build();
        world.spawnEntity(lit);
        context.createTimedTaskRunner()
                .createAndAdd(() -> {
                    int source = ((VirtualLightView) world).backrooms$virtualLight().luminance(cell.asLong());
                    if (source == 0 || blockLight(world, cell) < source) {
                        throw new GameTestException("Le cadre n'est pas encore eclaire par l'autre cote.");
                    }
                })
                .createAndAddReported(() -> {
                    // Le portail part et ses sources avec lui, sans que le moteur le sache : la lumiere reste, comme celle
                    // sauvee avec un chunk d'une source eteinte depuis.
                    lit.discard();
                    ((VirtualLightView) world).backrooms$setVirtualLight(VirtualLightSources.EMPTY);
                    world.setBlockState(light, Blocks.AIR.getDefaultState());
                    context.assertTrue(blockLight(world, cell) > 0, "La lumiere reste sans sa source.");
                    // Un portail apparait sur le meme cadre, vers un coin sans lumiere : rien a transmettre, rien de change.
                    world.spawnEntity(northPortal(world, entry, World.OVERWORLD, context.getAbsolute(new Vec3d(23.5, 2.0, 11.5))).build());
                })
                .createAndAdd(() -> {
                    if (blockLight(world, cell) != 0) {
                        throw new GameTestException("Le cadre du portail apparu n'est pas encore reverifie : " + blockLight(world, cell) + ".");
                    }
                })
                .completeIfSuccessful();
    }
}
