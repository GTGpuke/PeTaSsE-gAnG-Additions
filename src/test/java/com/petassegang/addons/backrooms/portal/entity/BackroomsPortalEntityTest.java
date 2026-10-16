package com.petassegang.addons.backrooms.portal.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.petassegang.addons.MinecraftTestBootstrap;
import com.petassegang.addons.backrooms.BackroomsConstants;

@DisplayName("Entity portail")
class BackroomsPortalEntityTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    private static BackroomsPortalEntity.Builder validBuilder() {
        return BackroomsPortalEntity.builder(null)
                .position(new Vec3d(10.0, 64.0, 20.0))
                .facingSource(PortalFacing.NORTH)
                .facingDestination(PortalFacing.EAST)
                .destinationDim(BackroomsConstants.LEVEL_ZERO_WORLD)
                .destinationPos(new Vec3d(1.5, 1.0, 1.5))
                .size(2.0f, 3.0f);
    }

    @Test
    @DisplayName("Le builder construit un portail dont les accesseurs restituent la configuration")
    void builderRoundTrip() {
        BackroomsPortalEntity portal = validBuilder().teleportable(false).visible(false).build();
        assertEquals(new Vec3d(10.0, 64.0, 20.0), portal.getPos(), "Position.");
        assertEquals(PortalFacing.NORTH, portal.facingSource(), "Orientation source.");
        assertEquals(PortalFacing.EAST, portal.facingDestination(), "Orientation destination.");
        assertEquals(BackroomsConstants.LEVEL_ZERO_WORLD, portal.destinationDim(), "Dimension destination.");
        assertEquals(new Vec3d(1.5, 1.0, 1.5), portal.destinationPos(), "Position destination.");
        assertEquals(2.0f, portal.width(), 1e-6f, "Largeur.");
        assertEquals(3.0f, portal.height(), 1e-6f, "Hauteur.");
        assertFalse(portal.teleportable(), "Teleportation desactivee.");
        assertFalse(portal.visible(), "Invisible.");
        assertFalse(portal.canHit(), "Un portail ne peut pas etre frappe.");
        assertNull(portal.ownerPos(), "Portail libre, sans porte.");
        portal.setTeleportable(true);
        assertTrue(portal.teleportable(), "Teleportation reactivee.");
    }

    @Test
    @DisplayName("Le builder refuse une configuration incomplete ou une taille nulle")
    void builderValidation() {
        assertThrows(IllegalStateException.class, () -> validBuilder().position(null).build(), "Position manquante.");
        assertThrows(IllegalStateException.class, () -> validBuilder().facingSource(null).build(), "Source manquante.");
        assertThrows(IllegalStateException.class, () -> validBuilder().facingDestination(null).build(), "Destination manquante.");
        assertThrows(IllegalStateException.class, () -> validBuilder().destinationDim(null).build(), "Dimension manquante.");
        assertThrows(IllegalStateException.class, () -> validBuilder().destinationPos(null).build(), "Position destination manquante.");
        assertThrows(IllegalStateException.class, () -> validBuilder().size(0.0f, 2.0f).build(), "Largeur nulle.");
        assertThrows(IllegalStateException.class, () -> validBuilder().size(2.0f, 0.0f).build(), "Hauteur nulle.");
    }

    @Test
    @DisplayName("La boite de detection suit l'orientation, est mise en cache, et la distance signee est positive devant")
    void geometry() {
        BackroomsPortalEntity portal = validBuilder().build();
        Box box = portal.detectionBox();
        assertEquals(2.0, box.getLengthX(), 1e-9, "La largeur suit axisW (X pour un portail nord).");
        assertEquals(3.0, box.getLengthY(), 1e-9, "La hauteur suit axisH (Y).");
        assertEquals(0.5, box.getLengthZ(), 1e-9, "L'epaisseur de detection vaut 0.5 bloc.");
        assertSame(box, portal.detectionBox(), "La boite est reutilisee tant que le portail ne bouge pas.");
        portal.setPosition(11.0, 64.0, 20.0);
        assertEquals(10.0, portal.detectionBox().minX, 1e-9, "La boite suit la nouvelle position.");
        double inFront = portal.signedDistance(new Vec3d(11.0, 64.0, 18.0));
        double behind = portal.signedDistance(new Vec3d(11.0, 64.0, 22.0));
        assertEquals(2.0, inFront, 1e-9, "Deux blocs au nord d'un portail nord : devant, distance positive.");
        assertEquals(-2.0, behind, 1e-9, "Deux blocs au sud : derriere, distance negative.");
    }

    @Test
    @DisplayName("La sauvegarde NBT restitue exactement le portail, porte proprietaire comprise")
    void nbtRoundTrip() {
        BackroomsPortalEntity original = validBuilder().teleportable(false).owner(World.OVERWORLD, new BlockPos(4, 65, -9)).build();
        NbtCompound nbt = new NbtCompound();
        original.writeCustomDataToNbt(nbt);

        BackroomsPortalEntity restored = new BackroomsPortalEntity(BackroomsPortalEntityType.INSTANCE, null);
        restored.readCustomDataFromNbt(nbt);
        assertEquals(original.facingSource(), restored.facingSource(), "Orientation source.");
        assertEquals(original.facingDestination(), restored.facingDestination(), "Orientation destination.");
        assertEquals(original.destinationDim(), restored.destinationDim(), "Dimension destination.");
        assertEquals(original.destinationPos(), restored.destinationPos(), "Position destination.");
        assertEquals(original.width(), restored.width(), 1e-6f, "Largeur.");
        assertEquals(original.height(), restored.height(), 1e-6f, "Hauteur.");
        assertEquals(original.teleportable(), restored.teleportable(), "Teleportable.");
        assertEquals(original.visible(), restored.visible(), "Visible.");
        assertEquals(World.OVERWORLD, restored.ownerDimension(), "Dimension de la porte proprietaire.");
        assertEquals(new BlockPos(4, 65, -9), restored.ownerPos(), "Position de la porte proprietaire.");

        NbtCompound free = new NbtCompound();
        validBuilder().build().writeCustomDataToNbt(free);
        assertFalse(free.contains("OwnerDim"), "Un portail libre n'ecrit pas de porte.");
    }

    @Test
    @DisplayName("Un NBT corrompu retombe sur des valeurs sures")
    void nbtFallbacks() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("FacingSource", "DIAGONAL");
        nbt.putString("DestinationDim", "");
        nbt.putFloat("Width", -4.0f);
        nbt.putString("OwnerDim", "");
        BackroomsPortalEntity portal = new BackroomsPortalEntity(BackroomsPortalEntityType.INSTANCE, null);
        portal.readCustomDataFromNbt(nbt);
        assertEquals(PortalFacing.NORTH, portal.facingSource(), "Orientation par defaut.");
        assertEquals(World.OVERWORLD, portal.destinationDim(), "Dimension par defaut.");
        assertEquals(0.1f, portal.width(), 1e-6f, "Largeur minimale.");
        assertTrue(portal.visible(), "Visible par defaut.");
        assertTrue(portal.teleportable(), "Teleportable par defaut.");
        assertNull(portal.ownerPos(), "Une porte sans position n'est pas retenue.");
    }

    @Test
    @DisplayName("La distance signee sans allocation vaut celle a vecteur, et la boite de traversee englobe la detection")
    void signedDistanceAndCrossingBox() {
        BackroomsPortalEntity portal = validBuilder().build();
        Vec3d front = new Vec3d(10.0, 64.0, 18.0);
        assertEquals(portal.signedDistance(front), portal.signedDistance(front.x, front.y, front.z), 1e-12, "Memes distances.");
        assertTrue(portal.signedDistance(front) > 0.0, "Le nord est le cote d'entree d'un portail oriente nord.");
        Box detection = portal.detectionBox();
        Box crossing = portal.crossingBox();
        assertSame(crossing, portal.crossingBox(), "La boite de traversee est mise en cache.");
        assertTrue(crossing.contains(detection.minX, detection.minY, detection.minZ)
                && crossing.contains(detection.maxX, detection.maxY, detection.maxZ), "Elle englobe la boite de detection.");
        assertEquals(detection.minX - 3.0, crossing.minX, 1e-9, "Trois blocs de marge : la vitesse maximale des projectiles.");
    }

    @Test
    @DisplayName("Un NBT sans dimension d'arrivee ou avec un identifiant illisible retombe sur l'overworld")
    void unreadableDestinationFallsBackToOverworld() {
        BackroomsPortalEntity portal = validBuilder().build();
        NbtCompound nbt = new NbtCompound();
        portal.writeCustomDataToNbt(nbt);
        nbt.remove("DestinationDim");
        portal.readCustomDataFromNbt(nbt);
        assertEquals(World.OVERWORLD, portal.destinationDim(), "Sans dimension ecrite : l'overworld.");
        nbt.putString("DestinationDim", "pas un identifiant");
        portal.readCustomDataFromNbt(nbt);
        assertEquals(World.OVERWORLD, portal.destinationDim(), "Identifiant illisible : l'overworld.");
        NbtCompound written = new NbtCompound();
        portal.writeCustomDataToNbt(written);
        assertEquals("pas un identifiant", written.getString("DestinationDim"),
                "La valeur brute est conservee telle quelle.");
    }

    @Test
    @DisplayName("Le portail ne se dessine que s'il est visible")
    void rendersOnlyWhenVisible() {
        assertTrue(validBuilder().build().shouldRender(0.0, 0.0, 0.0), "Visible : dessine.");
        assertFalse(validBuilder().visible(false).build().shouldRender(0.0, 0.0, 0.0), "Invisible : pas dessine.");
    }

    @Test
    @DisplayName("Une orientation hors bornes recue du reseau retombe sur le nord ; une destination mise a jour invalide le cache")
    void trackedDataUpdates() {
        BackroomsPortalEntity portal = validBuilder().facingSource(PortalFacing.EAST).facingDestination(PortalFacing.WEST).build();
        Vec3d destination = portal.destinationPos();
        assertSame(destination, portal.destinationPos(), "Le centre d'arrivee est mis en cache.");
        // Identifiants du tracker : les huit premiers sont ceux d'Entity, les notres suivent dans l'ordre de declaration.
        int facingSourceId = 8;
        int facingDestinationId = 9;
        int destinationXId = 11;
        portal.getDataTracker().writeUpdatedEntries(List.of(
                new DataTracker.SerializedEntry<>(facingSourceId, TrackedDataHandlerRegistry.INTEGER, 99),
                new DataTracker.SerializedEntry<>(facingDestinationId, TrackedDataHandlerRegistry.INTEGER, -1),
                new DataTracker.SerializedEntry<>(destinationXId, TrackedDataHandlerRegistry.LONG, Double.doubleToRawLongBits(42.5))));
        assertEquals(PortalFacing.NORTH, portal.facingSource(), "Ordinal trop grand : nord.");
        assertEquals(PortalFacing.NORTH, portal.facingDestination(), "Ordinal negatif : nord.");
        Vec3d updated = portal.destinationPos();
        assertNotEquals(destination, updated, "Le cache a ete invalide par la mise a jour.");
        assertEquals(42.5, updated.x, 1e-9, "Nouvelle abscisse d'arrivee.");
        assertEquals(destination.y, updated.y, 1e-9, "Ordonnee inchangee.");
    }

    @Test
    @DisplayName("Un NBT ancien sans visibilite, sans traversabilite ou sans position de porte garde des valeurs sures")
    void partialNbtDefaults() {
        BackroomsPortalEntity portal = validBuilder().visible(false).teleportable(false).build();
        NbtCompound nbt = new NbtCompound();
        portal.writeCustomDataToNbt(nbt);
        portal.readCustomDataFromNbt(nbt);
        assertFalse(portal.visible() || portal.teleportable(), "Ecrits a faux, visibilite et traversabilite sont relues a faux.");
        nbt.remove("Visible");
        nbt.remove("Teleportable");
        nbt.putString("OwnerDim", World.OVERWORLD.getValue().toString());
        portal.readCustomDataFromNbt(nbt);
        assertTrue(portal.visible(), "Sans cle, le portail est visible.");
        assertTrue(portal.teleportable(), "Sans cle, le portail est traversable.");
        assertNull(portal.ownerPos(), "Une dimension de porte sans position ne fait pas un proprietaire.");
        assertNull(portal.ownerDimension(), "Idem pour la dimension.");
    }

    @Test
    @DisplayName("Le cadre contient un point du plan dans sa largeur et sa hauteur, marge comprise, et rien au-dela")
    void frameContains() {
        BackroomsPortalEntity portal = validBuilder().build();
        // Portail nord de 2 x 3 centre en (10, 64, 20) : largeur le long de X, hauteur le long de Y.
        assertTrue(portal.frameContains(10.0, 64.0, 20.0, 0.0), "Le centre est dans le cadre.");
        assertTrue(portal.frameContains(11.4, 65.9, 20.0, 0.5), "Dans la marge, en largeur comme en hauteur.");
        assertFalse(portal.frameContains(11.6, 64.0, 20.0, 0.5), "Trop a l'est.");
        assertFalse(portal.frameContains(10.0, 66.1, 20.0, 0.5), "Trop haut.");
        assertFalse(portal.frameContains(8.4, 61.9, 20.0, 0.5), "Trop a l'ouest et trop bas.");
    }
}
