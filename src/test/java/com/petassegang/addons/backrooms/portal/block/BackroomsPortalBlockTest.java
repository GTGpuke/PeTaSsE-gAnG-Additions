package com.petassegang.addons.backrooms.portal.block;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EmptyBlockView;

import com.petassegang.addons.MinecraftTestBootstrap;
import com.petassegang.addons.backrooms.BackroomsConstants;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.init.ModBlockEntities;
import com.petassegang.addons.init.ModBlocks;
import com.petassegang.addons.init.ModDataComponents;
import com.petassegang.addons.init.ModItems;

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
        assertEquals(4, ModBlocks.BACKROOMS_PORTAL.getStateManager().getStates().size(), "Quatre orientations.");
        assertEquals(BlockRenderType.INVISIBLE, state.getRenderType(), "Rien a dessiner : le portail est rendu par ses entities.");
        assertTrue(state.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).isEmpty(), "Aucune collision.");
        assertTrue(state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN, ShapeContext.absent()).isEmpty(),
                "Sans joueur accroupi, le regard passe a travers.");
        assertEquals(Direction.EAST, state.rotate(BlockRotation.CLOCKWISE_90).get(BackroomsPortalBlock.FACING), "Rotation d'un quart de tour.");
        assertEquals(Direction.SOUTH, state.mirror(BlockMirror.LEFT_RIGHT).get(BackroomsPortalBlock.FACING), "Miroir gauche-droite : le nord devient le sud.");
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
    @DisplayName("Le cadre d'entree suit l'orientation du bloc et la taille reglee ; les reglages viennent de l'item")
    void entryFrameFollowsTheBlockAndItsSettings() {
        BlockState east = ModBlocks.BACKROOMS_PORTAL.getDefaultState().with(BackroomsPortalBlock.FACING, Direction.EAST);
        BackroomsPortalBlockEntity portal = new BackroomsPortalBlockEntity(new BlockPos(4, 10, -2), east);
        assertEquals(ModBlockEntities.BACKROOMS_PORTAL, portal.getType(), "Type de block entity enregistre.");
        assertEquals(PortalSettings.DEFAULT, portal.settings(), "Reglages par defaut.");
        PortalSettings wide = new PortalSettings(2, 3, false);
        portal.readComponents(wide.toStack(new ItemStack(ModItems.BACKROOMS_PORTAL)));
        assertEquals(wide, portal.settings(), "Les reglages de l'item sont recopies.");
        assertEquals(wide, portal.createComponentMap().get(ModDataComponents.PORTAL_SETTINGS), "Et rendus a une copie du bloc.");
        assertEquals(PortalFacing.EAST, portal.entryFacing(), "Face avant vers l'est.");
        Vec3d center = portal.entryCenter();
        assertEquals(4.5, center.x, EPSILON, "Plan au milieu du bloc.");
        assertEquals(10.0 + 1.5, center.y, EPSILON, "Centre a mi-hauteur d'un cadre de trois blocs.");
        assertEquals(-2.0 + 0.5 + PortalFacing.EAST.axisW.z * 0.5, center.z, EPSILON, "Largeur paire : decale d'un demi-bloc.");
    }

    @Test
    @DisplayName("Reglages, portails et destination survivent a une sauvegarde ; une copie du bloc ne garde que ses reglages")
    void nbtRoundTripAndCopy() {
        BackroomsPortalBlockEntity portal = new BackroomsPortalBlockEntity(new BlockPos(3, 1, -7), ModBlocks.BACKROOMS_PORTAL.getDefaultState());
        assertFalse(portal.isConfigured(), "Un bloc neuf ne connait pas sa destination.");
        NbtCompound fresh = new NbtCompound();
        portal.writeNbt(fresh, registries);
        assertEquals(Set.of("Width", "Height", "TransmitsLight", "DoubleSided", "OneWay", "FleetingExit"), fresh.getKeys(),
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

        restored.removeFromCopiedStackNbt(again);
        assertTrue(again.isEmpty(), "Une copie ne garde ni portails, ni destination, ni reglages hors composant : " + again + ".");
    }

    @Test
    @DisplayName("Un NBT incomplet ou abime laisse un bloc aux reglages par defaut, sans destination")
    void damagedNbtFallsBackToDefaults() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("DestDim", "petasse_gang_additions:backrooms_level_0");
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
        nbt.remove("DestDim");
        nbt.remove("DestX");
        nbt.remove("DestFacing");
        portal.readNbt(nbt, registries);
        assertFalse(portal.isConfigured(), "Sans destination sauvegardee.");
    }

    @Test
    @DisplayName("L'info-bulle de l'item donne la taille, la lumiere, les faces, le retour et la facon de casser le bloc")
    void tooltipDescribesTheSettings() {
        assertEquals(List.of("size", "light", "double_sided", "round_trip", "break"), tooltipKeys(new ItemStack(ModItems.BACKROOMS_PORTAL)),
                "Reglages par defaut.");
        assertEquals(List.of("size", "no_light", "single_sided", "one_way", "break"),
                tooltipKeys(new PortalSettings(3, 4, false, false, true, true).toStack(new ItemStack(ModItems.BACKROOMS_PORTAL))),
                "Lumiere bloquee, une face, aller simple : sans sortie, rien de fugace.");
        assertEquals(List.of("size", "light", "double_sided", "fleeting_exit", "break"),
                tooltipKeys(new PortalSettings(1, 2, true, true, false, true).toStack(new ItemStack(ModItems.BACKROOMS_PORTAL))),
                "Sortie fugace.");
    }

    private static List<String> tooltipKeys(ItemStack stack) {
        List<Text> tooltip = new ArrayList<>();
        ModItems.BACKROOMS_PORTAL.appendTooltip(stack, Item.TooltipContext.DEFAULT, tooltip, TooltipType.BASIC);
        List<String> keys = new ArrayList<>();
        for (Text line : tooltip) {
            String key = ((TranslatableTextContent) line.getContent()).getKey();
            keys.add(key.substring(key.lastIndexOf('.') + 1));
        }
        return keys;
    }
}
