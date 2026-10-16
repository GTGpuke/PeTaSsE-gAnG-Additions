package com.petassegang.addons.backrooms.portal.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

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
    @DisplayName("Le bloc porte s'oriente face au joueur et possede sa block entity")
    void doorBlock() {
        BlockState state = ModBlocks.BACKROOMS_DOOR.getDefaultState();
        assertEquals(Direction.NORTH, state.get(BackroomsDoorBlock.FACING), "Orientation par defaut.");
        assertEquals(4, ModBlocks.BACKROOMS_DOOR.getStateManager().getStates().size(), "Quatre orientations.");
        assertTrue(ModBlocks.BACKROOMS_DOOR.createBlockEntity(BlockPos.ORIGIN, state) instanceof BackroomsDoorBlockEntity,
                "La porte cree sa block entity.");
        assertEquals(ModBlockEntities.BACKROOMS_DOOR, new BackroomsDoorBlockEntity(BlockPos.ORIGIN, state).getType(),
                "Type de block entity enregistre.");
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
    @DisplayName("Une porte non configuree tique sans rien faire hors serveur et ignore un NBT vide")
    void unconfiguredDoor() {
        BackroomsDoorBlockEntity door = new BackroomsDoorBlockEntity(BlockPos.ORIGIN, ModBlocks.BACKROOMS_DOOR.getDefaultState());
        door.serverTick();
        assertFalse(door.isConfigured(), "Sans monde, pas de destination.");
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
    @DisplayName("Le bloc porte a un codec, se rend par son modele et n'a pas de collision")
    void doorBlockCodecAndShape() {
        BackroomsDoorBlock door = ModBlocks.BACKROOMS_DOOR;
        BlockState state = door.getDefaultState();
        assertNotNull(door.getCodec(), "Codec present.");
        assertEquals(BlockRenderType.MODEL, door.getRenderType(state), "Rendu par modele.");
        assertTrue(state.getCollisionShape(null, BlockPos.ORIGIN).isEmpty(), "Aucune collision : on la traverse.");
    }

    @Test
    @DisplayName("Hors serveur ou portails desactives, la porte ne tique pas et n'a rien a retirer")
    void inertWithoutServerOrPortals() {
        BackroomsDoorBlockEntity door = new BackroomsDoorBlockEntity(BlockPos.ORIGIN, ModBlocks.BACKROOMS_DOOR.getDefaultState());
        door.discardPortals();
        assertNull(door.attachedPortalUuid(), "Rien a retirer sans monde.");
        BackroomsDoorBlockEntity.setPortalsEnabled(false);
        try {
            door.serverTick();
            assertFalse(door.isConfigured(), "Portails desactives : la porte ne cherche pas de destination.");
        } finally {
            BackroomsDoorBlockEntity.setPortalsEnabled(true);
        }
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
