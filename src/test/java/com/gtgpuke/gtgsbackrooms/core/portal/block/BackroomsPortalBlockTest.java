package com.gtgpuke.gtgsbackrooms.core.portal.block;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants;
import com.gtgpuke.gtgsbackrooms.core.ModConfig;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.core.registry.ModBlockEntities;
import com.gtgpuke.gtgsbackrooms.core.registry.ModBlocks;
import com.gtgpuke.gtgsbackrooms.core.registry.ModDataComponents;
import com.gtgpuke.gtgsbackrooms.core.registry.ModItems;

@DisplayName("Bloc portail")
class BackroomsPortalBlockTest {

    private static final double EPSILON = 1.0e-9;
    private static RegistryWrapper.WrapperLookup registries;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
        registries = BuiltinRegistries.createWrapperLookup();
    }

    @Test
    @DisplayName("Le bloc ne se voit pas, ne bloque rien et ne se vise pas sans joueur accroupi ; il tourne avec sa structure")
    void blockIsInvisibleAndRotates() {
        BlockState state = ModBlocks.BACKROOMS_PORTAL.getDefaultState();
        assertEquals(Direction.NORTH, state.get(BackroomsPortalBlock.FACING), "Orientation par defaut.");
        assertEquals(6, ModBlocks.BACKROOMS_PORTAL.getStateManager().getStates().size(), "Six orientations : quatre debout, au sol, au plafond.");
        assertEquals(BlockRenderType.INVISIBLE, state.getRenderType(), "Rien a dessiner : le portail est rendu par ses entities.");
        assertTrue(state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).isEmpty(), "Aucune collision.");
        assertTrue(state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, ShapeContext.absent()).isEmpty(),
                "Sans joueur accroupi, le regard passe a travers.");
        assertEquals(Direction.EAST, state.rotate(BlockRotation.CLOCKWISE_90).get(BackroomsPortalBlock.FACING), "Rotation d'un quart de tour.");
        assertEquals(Direction.SOUTH, state.mirror(BlockMirror.LEFT_RIGHT).get(BackroomsPortalBlock.FACING), "Miroir gauche-droite : le nord devient le sud.");
        BlockState lying = state.with(BackroomsPortalBlock.FACING, Direction.UP);
        assertEquals(Direction.UP, lying.rotate(BlockRotation.CLOCKWISE_90).get(BackroomsPortalBlock.FACING), "Un cadre au sol tourne sur lui-meme.");
        assertEquals(Direction.UP, lying.mirror(BlockMirror.FRONT_BACK).get(BackroomsPortalBlock.FACING), "Et reste au sol au miroir.");
        assertTrue(ModBlocks.BACKROOMS_PORTAL.createBlockEntity(BlockPos.ORIGIN, state) instanceof BackroomsPortalBlockEntity,
                "Le bloc cree sa block entity.");
        assertEquals(BackroomsPortalBlock.CODEC, ModBlocks.BACKROOMS_PORTAL.getCodec(), "Codec du bloc.");
    }

    @Test
    @DisplayName("Hors d'un monde serveur, le bloc ne cherche rien, portails actives ou non")
    void serverTickNeedsAServerWorld() {
        BackroomsPortalBlockEntity portal = new BackroomsPortalBlockEntity(BlockPos.ORIGIN, ModBlocks.BACKROOMS_PORTAL.getDefaultState());
        try {
            BackroomsPortalBlockEntity.setPortalsEnabled(true);
            assertDoesNotThrow(portal::serverTick, "Portails actifs, sans monde : le tick s'arrete avant toute recherche.");
            BackroomsPortalBlockEntity.setPortalsEnabled(false);
            assertDoesNotThrow(portal::serverTick, "Portails coupes : le tick ne fait rien.");
        } finally {
            BackroomsPortalBlockEntity.setPortalsEnabled(ModConfig.ENABLE_PORTAL_FEATURE);
        }
        assertFalse(portal.isConfigured(), "Sans monde serveur, aucune destination n'est cherchee.");
    }

    @Test
    @DisplayName("Le cadre d'entree suit l'orientation du bloc et la taille reglee ; les reglages viennent de la baguette")
    void entryFrameFollowsTheBlockAndItsSettings() {
        BlockState east = ModBlocks.BACKROOMS_PORTAL.getDefaultState().with(BackroomsPortalBlock.FACING, Direction.EAST);
        BackroomsPortalBlockEntity portal = new BackroomsPortalBlockEntity(new BlockPos(4, 10, -2), east);
        assertEquals(ModBlockEntities.BACKROOMS_PORTAL, portal.getType(), "Type de block entity enregistre.");
        assertEquals(PortalSettings.DEFAULT, portal.settings(), "Reglages par defaut.");
        PortalSettings wide = new PortalSettings(2, 3, false);
        portal.readComponents(wand(wide));
        assertEquals(wide, portal.settings(), "Les reglages de la baguette sont recopies.");
        assertEquals(PortalFacing.EAST, portal.entryFacing(), "Face avant vers l'est.");
        Vec3d center = portal.entryCenter();
        assertEquals(4.5, center.x, EPSILON, "Plan au milieu du bloc.");
        assertEquals(10.0 + 1.5, center.y, EPSILON, "Centre a mi-hauteur d'un cadre de trois blocs.");
        assertEquals(-2.0 + 0.5 + PortalFacing.EAST.axisW.z * 0.5, center.z, EPSILON, "Largeur paire : decale d'un demi-bloc.");
        // Plan au bord du bloc : sur sa face arriere, a l'ouest pour une face avant a l'est ; garde a la sauvegarde, comme une
        // arrivee tete en bas, la pose, la destination et la sortie fixee.
        PortalSettings edge = new PortalSettings(1, 2, true, true, false, false, PortalExit.STRAIGHT, 0, true, true, PortalPlacement.FLOOR,
                Optional.of(World.NETHER), Optional.of(new PortalMark(World.END, new BlockPos(100, 50, -20), Direction.WEST)));
        portal.readComponents(wand(edge));
        assertEquals(4.0, portal.entryCenter().x, EPSILON, "Plan sur la face ouest du bloc.");
        NbtCompound nbt = new NbtCompound();
        portal.writeNbt(nbt, registries);
        assertTrue(nbt.getBoolean("Flush"), "Le plan au bord du bloc se sauve.");
        assertTrue(nbt.getBoolean("UpsideDown"), "L'arrivee tete en bas se sauve.");
        assertEquals("floor", nbt.getString("Placement"), "La pose se sauve.");
        assertEquals("minecraft:the_nether", nbt.getString("Destination"), "La destination se sauve.");
        assertTrue(nbt.contains("Mark"), "La sortie fixee se sauve.");
        BackroomsPortalBlockEntity restored = new BackroomsPortalBlockEntity(new BlockPos(4, 10, -2), east);
        restored.readNbt(nbt, registries);
        assertEquals(edge, restored.settings(), "Et se relit.");
        // Une destination ou une sortie fixee illisibles sont oubliees : destination automatique, sans sortie fixee.
        nbt.putString("Destination", "pas une destination");
        nbt.put("Mark", new NbtCompound());
        restored.readNbt(nbt, registries);
        assertTrue(restored.settings().destination().isEmpty() && restored.settings().mark().isEmpty(), "Illisibles, elles sont oubliees.");
    }

    @Test
    @DisplayName("Reglages, portails et destination survivent a une sauvegarde")
    void nbtRoundTrip() {
        BackroomsPortalBlockEntity portal = new BackroomsPortalBlockEntity(new BlockPos(3, 1, -7), ModBlocks.BACKROOMS_PORTAL.getDefaultState());
        assertFalse(portal.isConfigured(), "Un bloc neuf ne connait pas sa destination.");
        NbtCompound fresh = new NbtCompound();
        portal.writeNbt(fresh, registries);
        assertEquals(Set.of("Width", "Height", "TransmitsLight", "DoubleSided", "OneWay", "FleetingExit", "Exit", "ExitScatter", "Flush", "UpsideDown",
                "Placement"),
                fresh.getKeys(),
                "Un bloc neuf ne sauve que ses reglages.");
        portal.configureTarget(BackroomsConstants.LEVEL_ZERO_WORLD, new Vec3d(1.5, 2.0, 1.5), PortalFacing.SOUTH);
        assertTrue(portal.isConfigured(), "Le bloc connait sa destination.");
        assertNull(portal.entryPortal(BackroomsPortalBlockEntity.FRONT), "Sans monde serveur, aucun portail n'est pose.");
        NbtCompound nbt = new NbtCompound();
        portal.writeNbt(nbt, registries);
        UUID[] uuids = {UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()};
        nbt.putUuid("EntryFront", uuids[0]);
        nbt.putUuid("EntryBack", uuids[1]);
        nbt.putUuid("ReturnFront", uuids[2]);
        nbt.putUuid("ReturnBack", uuids[3]);
        // Une sortie fugace perdue de vue : sauvee seulement quand elle l'est.
        nbt.putBoolean("FleetingExit", true);
        nbt.putBoolean("ExitLost", true);

        BackroomsPortalBlockEntity restored = new BackroomsPortalBlockEntity(new BlockPos(3, 1, -7), ModBlocks.BACKROOMS_PORTAL.getDefaultState());
        restored.readNbt(nbt, registries);
        assertTrue(restored.isConfigured(), "La destination est relue.");
        assertEquals(uuids[0], restored.entryPortal(BackroomsPortalBlockEntity.FRONT), "Portail d'entree avant relu.");
        assertEquals(uuids[1], restored.entryPortal(BackroomsPortalBlockEntity.BACK), "Portail d'entree arriere relu.");
        assertEquals(uuids[2], restored.returnPortal(BackroomsPortalBlockEntity.FRONT), "Retour avant relu.");
        assertEquals(uuids[3], restored.returnPortal(BackroomsPortalBlockEntity.BACK), "Retour arriere relu.");
        for (UUID uuid : uuids) {
            assertTrue(restored.owns(uuid), "Le bloc tient ses quatre portails pour siens.");
        }
        assertFalse(restored.owns(UUID.randomUUID()), "Un autre portail ne lui appartient pas.");
        NbtCompound again = new NbtCompound();
        restored.writeNbt(again, registries);
        assertEquals(nbt, again, "Le NBT est stable apres un aller-retour.");
    }

    @Test
    @DisplayName("Seule une sortie fugace se perd de vue : sans elle, une perte sauvee est oubliee a la relecture")
    void onlyAFleetingExitIsLost() {
        NbtCompound nbt = new NbtCompound();
        nbt.putBoolean("ExitLost", true);
        BackroomsPortalBlockEntity portal = new BackroomsPortalBlockEntity(new BlockPos(0, 1, 0), ModBlocks.BACKROOMS_PORTAL.getDefaultState());
        portal.readNbt(nbt, registries);
        NbtCompound saved = new NbtCompound();
        portal.writeNbt(saved, registries);
        assertFalse(saved.contains("ExitLost"), "Sans sortie fugace, la perte n'est pas retenue.");
        nbt.putBoolean("FleetingExit", true);
        portal.readNbt(nbt, registries);
        NbtCompound fleeting = new NbtCompound();
        portal.writeNbt(fleeting, registries);
        assertTrue(fleeting.getBoolean("ExitLost"), "Une sortie fugace perdue de vue le reste a la relecture.");
    }

    @Test
    @DisplayName("Un NBT incomplet ou abime laisse un bloc aux reglages par defaut, sans destination")
    void damagedNbtFallsBackToDefaults() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("DestDim", "gtgs_backrooms:backrooms_level_0");
        nbt.putDouble("DestX", 1.0);
        nbt.putString("DestFacing", "NULLE_PART");
        BackroomsPortalBlockEntity portal = new BackroomsPortalBlockEntity(BlockPos.ORIGIN, ModBlocks.BACKROOMS_PORTAL.getDefaultState());
        portal.readNbt(nbt, registries);
        assertEquals(PortalSettings.DEFAULT, portal.settings(), "Reglages absents : ceux par defaut.");
        assertFalse(portal.isConfigured(), "Orientation illisible : pas de destination.");
        nbt.putString("DestFacing", "SOUTH");
        nbt.remove("DestX");
        portal.readNbt(nbt, registries);
        assertFalse(portal.isConfigured(), "Dimension sans position : pas de destination.");
        nbt.putInt("Width", 99);
        nbt.putInt("Height", 0);
        nbt.putBoolean("TransmitsLight", false);
        portal.readNbt(nbt, registries);
        assertEquals(new PortalSettings(PortalSettings.MAX_SIZE, PortalSettings.MIN_SIZE, false), portal.settings(), "Taille ramenee dans les bornes.");
        nbt.putBoolean("DoubleSided", false);
        nbt.putBoolean("OneWay", true);
        portal.readNbt(nbt, registries);
        assertEquals(new PortalSettings(PortalSettings.MAX_SIZE, PortalSettings.MIN_SIZE, false, false, true, false), portal.settings(),
                "Une face, aller simple : relus.");
        nbt.putBoolean("FleetingExit", true);
        portal.readNbt(nbt, registries);
        assertTrue(portal.settings().fleetingExit(), "Sortie fugace : relue.");
        nbt.putString("Exit", "east");
        nbt.putInt("ExitScatter", 12);
        portal.readNbt(nbt, registries);
        assertEquals(PortalExit.EAST, portal.settings().exit(), "Sens de sortie : relu.");
        assertEquals(12, portal.settings().exitScatter(), "Rayon de tirage : relu.");
        nbt.putString("Exit", "diagonale");
        portal.readNbt(nbt, registries);
        assertEquals(PortalExit.STRAIGHT, portal.settings().exit(), "Sens inconnu : tout droit.");
        nbt.remove("DestDim");
        nbt.remove("DestX");
        nbt.remove("DestFacing");
        portal.readNbt(nbt, registries);
        assertFalse(portal.isConfigured(), "Sans destination sauvegardee.");
    }

    /** Baguette de portail portant ces reglages. */
    private static ItemStack wand(PortalSettings settings) {
        ItemStack wand = new ItemStack(ModItems.PORTAL_WAND);
        wand.set(ModDataComponents.PORTAL_SETTINGS, settings);
        return wand;
    }
}
