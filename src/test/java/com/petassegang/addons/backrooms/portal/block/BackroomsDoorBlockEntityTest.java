package com.petassegang.addons.backrooms.portal.block;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import com.petassegang.addons.MinecraftTestBootstrap;
import com.petassegang.addons.backrooms.BackroomsConstants;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.init.ModBlockEntities;
import com.petassegang.addons.init.ModBlocks;

@DisplayName("Porte Backrooms")
class BackroomsDoorBlockEntityTest {

    private static RegistryWrapper.WrapperLookup registries;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
        registries = BuiltinRegistries.createWrapperLookup();
    }

    @Test
    @DisplayName("Le bloc porte est une vraie porte : deux moities, orientee comme le regard, entree du cote oppose")
    void doorBlock() {
        BlockState state = ModBlocks.BACKROOMS_DOOR.getDefaultState();
        assertEquals(Direction.NORTH, state.get(DoorBlock.FACING), "Orientation par defaut.");
        assertEquals(Direction.SOUTH, BackroomsDoorBlock.entrySide(state), "On entre par le cote oppose a l'orientation.");
        assertFalse(state.get(DoorBlock.OPEN), "Fermee par defaut.");
        assertEquals(64, ModBlocks.BACKROOMS_DOOR.getStateManager().getStates().size(),
                "Quatre orientations, deux moities, deux gonds, ouverte ou fermee, alimentee ou non.");
        assertTrue(ModBlocks.BACKROOMS_DOOR.createBlockEntity(BlockPos.ORIGIN, state) instanceof BackroomsDoorBlockEntity,
                "La moitie basse cree sa block entity.");
        assertNull(ModBlocks.BACKROOMS_DOOR.createBlockEntity(BlockPos.ORIGIN, state.with(DoorBlock.HALF, DoubleBlockHalf.UPPER)),
                "La moitie haute n'en a pas.");
        assertEquals(ModBlockEntities.BACKROOMS_DOOR, new BackroomsDoorBlockEntity(BlockPos.ORIGIN, state).getType(),
                "Type de block entity enregistre.");
        assertFalse(new BackroomsDoorBlockEntity(BlockPos.ORIGIN, state).isOpen(), "La block entity lit la porte fermee.");
        assertTrue(new BackroomsDoorBlockEntity(BlockPos.ORIGIN, state.with(DoorBlock.OPEN, true)).isOpen(), "Et la porte ouverte.");
    }

    @Test
    @DisplayName("La cible configuree survit a une sauvegarde NBT")
    void nbtRoundTrip() {
        BackroomsDoorBlockEntity door = new BackroomsDoorBlockEntity(new BlockPos(3, 1, -7), ModBlocks.BACKROOMS_DOOR.getDefaultState());
        assertFalse(door.isConfigured(), "Une porte neuve ne connait pas sa destination.");
        door.configureTarget(BackroomsConstants.LEVEL_ZERO_WORLD, new Vec3d(1.5, 2.0, 1.5), PortalFacing.SOUTH);
        assertTrue(door.isConfigured(), "La porte connait sa destination.");
        assertNull(door.attachedPortalUuid(), "Sans monde serveur, aucun portail n'est pose.");
        NbtCompound nbt = new NbtCompound();
        door.writeNbt(nbt, registries);

        BackroomsDoorBlockEntity restored = new BackroomsDoorBlockEntity(new BlockPos(3, 1, -7), ModBlocks.BACKROOMS_DOOR.getDefaultState());
        restored.readNbt(nbt, registries);
        NbtCompound again = new NbtCompound();
        restored.writeNbt(again, registries);
        assertEquals(nbt, again, "Le NBT est stable apres un aller-retour.");
        assertTrue(restored.isConfigured(), "La destination est restauree.");
        assertEquals(BackroomsConstants.LEVEL_ZERO_WORLD.getValue().toString(), again.getString("DestDim"), "Dimension cible.");
        assertEquals(1.5, again.getDouble("DestX"), 1e-9, "X cible.");
        assertEquals("SOUTH", again.getString("DestFacing"), "Orientation cible.");
    }

    @Test
    @DisplayName("Une porte non configuree ignore un NBT vide")
    void unconfiguredDoor() {
        BackroomsDoorBlockEntity door = new BackroomsDoorBlockEntity(BlockPos.ORIGIN, ModBlocks.BACKROOMS_DOOR.getDefaultState());
        NbtCompound empty = new NbtCompound();
        door.readNbt(empty, registries);
        NbtCompound written = new NbtCompound();
        door.writeNbt(written, registries);
        assertFalse(written.contains("DestDim"), "Rien a ecrire sans cible.");
        assertFalse(written.contains("AttachedPortal"), "Aucun portail d'entree.");
        assertFalse(written.contains("ReturnPortal"), "Aucun portail de retour.");
    }

    @Test
    @DisplayName("Les identifiants de portails survivent au NBT et une orientation inconnue laisse la porte non configuree")
    void portalIdsAndInvalidFacing() {
        BackroomsDoorBlockEntity door = new BackroomsDoorBlockEntity(BlockPos.ORIGIN, ModBlocks.BACKROOMS_DOOR.getDefaultState());
        UUID attached = UUID.randomUUID();
        UUID back = UUID.randomUUID();
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("AttachedPortal", attached);
        nbt.putUuid("ReturnPortal", back);
        nbt.putString("DestDim", BackroomsConstants.LEVEL_ZERO_WORLD.getValue().toString());
        nbt.putDouble("DestX", 1.5);
        nbt.putDouble("DestY", 2.0);
        nbt.putDouble("DestZ", 1.5);
        nbt.putString("DestFacing", "DIAGONALE");
        door.readNbt(nbt, registries);
        assertEquals(attached, door.attachedPortalUuid(), "Portail d'entree restaure.");
        assertEquals(back, door.returnPortalUuid(), "Portail de retour restaure.");
        assertFalse(door.isConfigured(), "Une orientation inconnue invalide la cible.");
        NbtCompound written = new NbtCompound();
        door.writeNbt(written, registries);
        assertEquals(attached, written.getUuid("AttachedPortal"), "Portail d'entree reecrit.");
        assertEquals(back, written.getUuid("ReturnPortal"), "Portail de retour reecrit.");
        assertFalse(written.contains("DestFacing"), "Pas d'orientation a ecrire.");
        assertTrue(written.contains("DestDim") && written.contains("DestX"), "Dimension et position cibles conservees.");
    }

    @Test
    @DisplayName("Le bloc porte a un codec, sonne comme le chene noir, bloque fermee et ne tique que par sa moitie basse")
    void doorBlockCodecShapeAndTicker() {
        BackroomsDoorBlock door = ModBlocks.BACKROOMS_DOOR;
        BlockState state = door.getDefaultState();
        assertNotNull(door.getCodec(), "Codec present.");
        assertEquals(BlockSetType.DARK_OAK, door.getBlockSetType(), "Porte en chene noir : sons et ouverture a la main.");
        assertFalse(state.getCollisionShape(null, BlockPos.ORIGIN).isEmpty(), "Fermee, la porte bloque le passage.");
        VoxelShape open = state.with(DoorBlock.OPEN, true).getCollisionShape(null, BlockPos.ORIGIN);
        assertEquals(3.0 / 16.0, open.getMax(Direction.Axis.X) - open.getMin(Direction.Axis.X), 1e-9, "Ouverte, seul le battant reste, sur le cote.");
        assertNotNull(door.getTicker(null, state, ModBlockEntities.BACKROOMS_DOOR), "La moitie basse tique.");
        assertNull(door.getTicker(null, state.with(DoorBlock.HALF, DoubleBlockHalf.UPPER), ModBlockEntities.BACKROOMS_DOOR), "La moitie haute ne tique pas.");
        assertNull(door.getTicker(null, state, BlockEntityType.CHEST), "Un autre type de block entity ne tique pas ici.");
    }

    @Test
    @DisplayName("Hors serveur, la porte ne tique pas et laisse ses portails au serveur")
    void inertWithoutServer() {
        BackroomsDoorBlockEntity door = new BackroomsDoorBlockEntity(BlockPos.ORIGIN, ModBlocks.BACKROOMS_DOOR.getDefaultState());
        UUID attached = UUID.randomUUID();
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("AttachedPortal", attached);
        door.readNbt(nbt, registries);
        // Sans monde serveur, le moindre acces au monde planterait : le tick doit sortir avant.
        assertDoesNotThrow(door::serverTick, "Hors serveur, le tick ne fait rien.");
        door.discardPortals();
        assertEquals(attached, door.attachedPortalUuid(), "Cote client, la porte cassee ne retire rien : le serveur s'en charge.");
    }

    @Test
    @DisplayName("Une destination incomplete, sans position ou sans orientation, ne configure pas la porte")
    void incompleteDestinationIsNotConfigured() {
        BackroomsDoorBlockEntity door = new BackroomsDoorBlockEntity(BlockPos.ORIGIN, ModBlocks.BACKROOMS_DOOR.getDefaultState());
        NbtCompound nbt = new NbtCompound();
        nbt.putString("DestDim", BackroomsConstants.LEVEL_ZERO_WORLD.getValue().toString());
        nbt.putString("DestFacing", "SOUTH");
        door.readNbt(nbt, registries);
        assertFalse(door.isConfigured(), "Dimension et orientation sans position : incomplet.");
        NbtCompound noFacing = new NbtCompound();
        noFacing.putString("DestDim", BackroomsConstants.LEVEL_ZERO_WORLD.getValue().toString());
        noFacing.putDouble("DestX", 1.5);
        noFacing.putDouble("DestY", 2.0);
        noFacing.putDouble("DestZ", 1.5);
        door.readNbt(noFacing, registries);
        assertFalse(door.isConfigured(), "Dimension et position sans orientation : incomplet.");
    }
}
