package com.gtgpuke.gtgsbackrooms.backrooms.portal.entity;

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
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants;
import com.gtgpuke.gtgsbackrooms.init.ModEntities;

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
        assertNull(portal.owner(), "Portail libre, sans bloc proprietaire.");
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
    @DisplayName("La sauvegarde NBT restitue exactement le portail, bloc proprietaire compris")
    void nbtRoundTrip() {
        BackroomsPortalEntity original = validBuilder().teleportable(false).owner(World.OVERWORLD, new BlockPos(4, 65, -9)).build();
        NbtCompound nbt = new NbtCompound();
        original.writeCustomDataToNbt(nbt);

        BackroomsPortalEntity restored = new BackroomsPortalEntity(ModEntities.BACKROOMS_PORTAL, null);
        restored.readCustomDataFromNbt(nbt);
        assertEquals(original.facingSource(), restored.facingSource(), "Orientation source.");
        assertEquals(original.facingDestination(), restored.facingDestination(), "Orientation destination.");
        assertEquals(original.destinationDim(), restored.destinationDim(), "Dimension destination.");
        assertEquals(original.destinationPos(), restored.destinationPos(), "Position destination.");
        assertEquals(original.width(), restored.width(), 1e-6f, "Largeur.");
        assertEquals(original.height(), restored.height(), 1e-6f, "Hauteur.");
        assertEquals(original.teleportable(), restored.teleportable(), "Teleportable.");
        assertEquals(original.visible(), restored.visible(), "Visible.");
        assertEquals(GlobalPos.create(World.OVERWORLD, new BlockPos(4, 65, -9)), restored.owner(), "Dimension et position du bloc proprietaire.");

        NbtCompound free = new NbtCompound();
        validBuilder().build().writeCustomDataToNbt(free);
        assertFalse(free.contains("OwnerDim"), "Un portail libre n'ecrit pas de proprietaire.");
    }

    @Test
    @DisplayName("Un NBT corrompu retombe sur des valeurs sures")
    void nbtFallbacks() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("FacingSource", "DIAGONAL");
        nbt.putString("DestinationDim", "");
        nbt.putFloat("Width", -4.0f);
        nbt.putString("OwnerDim", "");
        BackroomsPortalEntity portal = new BackroomsPortalEntity(ModEntities.BACKROOMS_PORTAL, null);
        portal.readCustomDataFromNbt(nbt);
        assertEquals(PortalFacing.NORTH, portal.facingSource(), "Orientation par defaut.");
        assertEquals(World.OVERWORLD, portal.destinationDim(), "Dimension par defaut.");
        assertEquals(0.1f, portal.width(), 1e-6f, "Largeur minimale.");
        assertTrue(portal.visible(), "Visible par defaut.");
        assertTrue(portal.teleportable(), "Teleportable par defaut.");
        assertNull(portal.owner(), "Un proprietaire sans position n'est pas retenu.");
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
    @DisplayName("Une orientation hors bornes recue du reseau retombe sur le nord ; une destination mise a jour invalide les caches")
    void trackedDataUpdates() {
        BackroomsPortalEntity portal = validBuilder().facingSource(PortalFacing.EAST).facingDestination(PortalFacing.WEST).build();
        Vec3d destination = portal.destinationPos();
        assertSame(destination, portal.destinationPos(), "Le centre d'arrivee est mis en cache.");
        assertSame(portal.destinationDim(), portal.destinationDim(), "La dimension d'arrivee aussi.");
        portal.getDataTracker().writeUpdatedEntries(List.of(
                new DataTracker.SerializedEntry<>(BackroomsPortalEntity.TRACKED_FACING_SOURCE.id(), TrackedDataHandlerRegistry.INTEGER, 99),
                new DataTracker.SerializedEntry<>(BackroomsPortalEntity.TRACKED_FACING_DESTINATION.id(), TrackedDataHandlerRegistry.INTEGER, -1),
                new DataTracker.SerializedEntry<>(BackroomsPortalEntity.TRACKED_DESTINATION_X.id(), TrackedDataHandlerRegistry.LONG,
                        Double.doubleToRawLongBits(42.5)),
                new DataTracker.SerializedEntry<>(BackroomsPortalEntity.TRACKED_DESTINATION_DIM.id(), TrackedDataHandlerRegistry.STRING,
                        World.NETHER.getValue().toString()),
                new DataTracker.SerializedEntry<>(BackroomsPortalEntity.TRACKED_TELEPORTABLE.id(), TrackedDataHandlerRegistry.BOOLEAN, false)));
        assertEquals(PortalFacing.NORTH, portal.facingSource(), "Ordinal trop grand : nord.");
        assertEquals(PortalFacing.NORTH, portal.facingDestination(), "Ordinal negatif : nord.");
        Vec3d updated = portal.destinationPos();
        assertNotEquals(destination, updated, "Le cache a ete invalide par la mise a jour.");
        assertEquals(42.5, updated.x, 1e-9, "Nouvelle abscisse d'arrivee.");
        assertEquals(destination.y, updated.y, 1e-9, "Ordonnee inchangee.");
        assertEquals(World.NETHER, portal.destinationDim(), "La nouvelle dimension remplace celle du cache.");
        assertFalse(portal.teleportable(), "La fermeture arrive au client par le tracker.");
    }

    @Test
    @DisplayName("Un point passe de l'autre cote puis revient a l'identique : l'aller et le retour sont inverses")
    void toAndFromDestinationAreInverse() {
        BackroomsPortalEntity portal = validBuilder().build();
        Vec3d point = new Vec3d(11.25, 65.5, 17.0);
        Vec3d there = portal.toDestination(point);
        assertEquals(PortalFacing.transformPoint(PortalFacing.NORTH, portal.getPos(), PortalFacing.EAST, portal.destinationPos(), point),
                there, "Meme passage que celui des orientations.");
        Vec3d back = portal.fromDestination(there);
        assertEquals(point.x, back.x, 1e-9, "X.");
        assertEquals(point.y, back.y, 1e-9, "Y.");
        assertEquals(point.z, back.z, 1e-9, "Z.");
    }

    @Test
    @DisplayName("Un NBT ancien sans visibilite, sans traversabilite ou sans position de proprietaire garde des valeurs sures")
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
        assertNull(portal.owner(), "Une dimension sans position ne fait pas un proprietaire.");
    }

    @Test
    @DisplayName("Le point du cadre le plus proche d'une source est sa projection sur le plan, ramenee dans le cadre")
    void nearestFramePoint() {
        BackroomsPortalEntity portal = validBuilder().build();
        // Portail nord de 2 x 3 centre en (10, 64, 20) : largeur le long de X, hauteur le long de Y, plan en Z = 20.
        assertEquals(new Vec3d(10.5, 64.5, 20.0), portal.nearestFramePoint(10.5, 64.5, 15.0), "Devant le cadre : projection droite.");
        assertEquals(new Vec3d(9.0, 65.5, 20.0), portal.nearestFramePoint(4.0, 80.0, 12.0), "Loin en biais : le coin le plus proche.");
        assertEquals(new Vec3d(11.0, 62.5, 20.0), portal.nearestFramePoint(30.0, 50.0, 25.0), "Derriere, en bas a l'est : l'autre coin.");
    }

    @Test
    @DisplayName("Le cadre contient un point du plan dans sa largeur et sa hauteur, et rien au-dela")
    void frameContains() {
        BackroomsPortalEntity portal = validBuilder().build();
        // Portail nord de 2 x 3 centre en (10, 64, 20) : largeur le long de X, hauteur le long de Y.
        assertTrue(portal.frameContains(10.0, 64.0, 20.0), "Le centre est dans le cadre.");
        assertTrue(portal.frameContains(10.9, 65.4, 20.0), "Pres du coin, en largeur comme en hauteur.");
        assertFalse(portal.frameContains(11.1, 64.0, 20.0), "Trop a l'est.");
        assertFalse(portal.frameContains(10.0, 65.6, 20.0), "Trop haut.");
        assertFalse(portal.frameContains(8.9, 62.4, 20.0), "Trop a l'ouest et trop bas.");
    }

    @Test
    @DisplayName("Une boite, a sa place ou decalee, est a cheval sur le cadre d'arrivee si elle traverse son plan dans le cadre")
    void arrivalFrameStraddles() {
        BackroomsPortalEntity portal = validBuilder().build();
        // Arrivee est de 2 x 3 centree en (1.5, 1, 1.5) : plan x = 1.5, largeur le long de Z, hauteur le long de Y.
        assertTrue(portal.arrivalFrameStraddles(new Box(1.2, 0.0, 1.2, 1.8, 1.8, 1.8), 0.0, 0.0, 0.0), "Un joueur a moitie sorti du cadre.");
        assertTrue(portal.arrivalFrameStraddles(new Box(1.2, 0.0, 2.2, 1.8, 1.8, 2.8), 0.0, 0.0, 0.0), "Au bord du cadre, il y est encore engage.");
        assertFalse(portal.arrivalFrameStraddles(new Box(1.5, 0.0, 1.2, 2.1, 1.8, 1.8), 0.0, 0.0, 0.0),
                "Tout entier devant, la boite touchant le plan.");
        assertFalse(portal.arrivalFrameStraddles(new Box(0.6, 0.0, 1.2, 1.2, 1.8, 1.8), 0.0, 0.0, 0.0), "Tout entier derriere.");
        assertFalse(portal.arrivalFrameStraddles(new Box(1.2, 0.0, 3.0, 1.8, 1.8, 3.6), 0.0, 0.0, 0.0),
                "A cheval sur le plan, mais a cote du cadre.");
        assertFalse(portal.arrivalFrameStraddles(new Box(1.2, 3.0, 1.2, 1.8, 4.8, 1.8), 0.0, 0.0, 0.0),
                "A cheval sur le plan, mais au-dessus du cadre.");
        assertFalse(portal.arrivalFrameStraddles(new Box(1.2, 0.0, 1.2, 1.8, 1.8, 1.8), -0.6, 0.0, 0.0),
                "Affichee un peu en arriere, elle n'est pas encore sortie.");
    }

    @Test
    @DisplayName("Une boite, a sa place ou decalee, est engagee dans le cadre d'entree si elle traverse son plan dans le cadre")
    void sourceFrameStraddles() {
        BackroomsPortalEntity portal = validBuilder().build();
        // Entree nord de 2 x 3 centree en (10, 64, 20) : plan z = 20, largeur le long de X.
        assertTrue(portal.sourceFrameStraddles(new Box(9.7, 62.5, 19.7, 10.3, 64.3, 20.3), 0.0, 0.0, 0.0), "A moitie entre dans le cadre d'entree.");
        assertFalse(portal.sourceFrameStraddles(new Box(9.7, 62.5, 18.0, 10.3, 64.3, 18.6), 0.0, 0.0, 0.0), "Encore devant le cadre d'entree.");
        assertTrue(portal.sourceFrameStraddles(new Box(9.7, 62.5, 18.0, 10.3, 64.3, 18.6), 0.0, 0.0, 1.7),
                "Affichee un bloc et demi plus loin, elle est engagee dans le cadre d'entree.");
    }

    @Test
    @DisplayName("Une face n'est franchie que vers son arriere, par une trajectoire qui passe en face de son ouverture a bref delai")
    void enteredOnlyTowardsItsBack() {
        BackroomsPortalEntity portal = validBuilder().build();
        // Entree nord de 2 x 3 en (10, 64, 20) : devant elle, z < 20 ; ouverture de x = 9 a 11.
        assertTrue(portal.enteredBy(10.0, 64.0, 19.5, 0.0, 0.0, 0.2, 10), "Vers l'arriere, le cadre droit devant : il y entre.");
        assertTrue(portal.enteredBy(10.0, 64.0, 20.1, 0.0, 0.0, 0.2, 10), "Le plan passe au tick precedent, dans le cadre : il y entre.");
        assertFalse(portal.enteredBy(10.0, 64.0, 20.5, 0.0, 0.0, 0.2, 10), "Passe depuis plus d'un tick : il n'y entre plus.");
        assertFalse(portal.enteredBy(10.0, 64.0, 19.9, 0.0, 0.0, -0.2, 10), "Vers l'avant : il entre par la face dos a dos.");
        assertFalse(portal.enteredBy(10.0, 64.0, 19.9, 0.2, 0.0, 0.0, 10), "Le long du plan, ou immobile : aucune face.");
        assertFalse(portal.enteredBy(12.0, 64.0, 19.5, 0.0, 0.0, 0.2, 10), "A cote du cadre : il le longe.");
        assertTrue(portal.enteredBy(12.0, 64.0, 19.5, -0.6, 0.0, 0.2, 10), "En biais vers l'ouverture : il y entre.");
        assertFalse(portal.enteredBy(10.0, 64.0, 17.0, 0.0, 0.0, 0.2, 10), "Trop loin pour atteindre le plan dans le delai.");
    }

    @Test
    @DisplayName("Seul le premier calcul de la lumiere transmise reverifie tout le cadre d'un portail")
    void onlyTheFirstLightPassRefreshesTheFrame() {
        BackroomsPortalEntity portal = validBuilder().build();
        assertTrue(portal.takeLightRefresh(), "Un portail qui apparait fait reverifier son cadre.");
        assertFalse(portal.takeLightRefresh(), "Une seule fois.");
    }

    @Test
    @DisplayName("Un cadre est centre en un point a un millieme de bloc pres")
    void centeredAtItsOwnCenterOnly() {
        BackroomsPortalEntity portal = validBuilder().build();
        assertTrue(portal.centeredAt(portal.getPos().add(0.0, 0.0, 1.0e-4)), "Le meme centre, aux arrondis pres.");
        assertFalse(portal.centeredAt(portal.getPos().add(0.0, 0.0, 0.01)), "Un centieme de bloc a cote : un autre cadre.");
    }

    @Test
    @DisplayName("Une taille relue refait les boites du cadre d'entree et du cadre d'arrivee")
    void boxesFollowARereadSize() {
        BackroomsPortalEntity portal = validBuilder().build();
        Box detection = portal.detectionBox();
        Box arrival = portal.arrivalBox();
        NbtCompound nbt = new NbtCompound();
        portal.writeCustomDataToNbt(nbt);
        nbt.putFloat("Width", 4.0f);
        portal.readCustomDataFromNbt(nbt);
        assertNotEquals(detection, portal.detectionBox(), "La boite du cadre d'entree suit la nouvelle largeur.");
        assertNotEquals(arrival, portal.arrivalBox(), "Celle du cadre d'arrivee aussi.");
    }

    @Test
    @DisplayName("La boite d'arrivee entoure le cadre d'arrivee, gardee tant que l'arrivee ne bouge pas")
    void arrivalBoxFollowsTheDestination() {
        BackroomsPortalEntity portal = validBuilder().build();
        Box arrival = portal.arrivalBox();
        // Arrivee est de 2 x 3 en (1.5, 1, 1.5) : un demi-bloc d'epaisseur le long de X, la largeur le long de Z.
        assertEquals(new Box(1.25, -0.5, 0.5, 1.75, 2.5, 2.5), arrival, "Le cadre d'arrivee, d'un demi-bloc d'epaisseur.");
        assertSame(arrival, portal.arrivalBox(), "Gardee tant que l'arrivee ne bouge pas.");
        assertEquals(arrival.expand(3.0), portal.arrivalCrossingBox(), "Elargie de trois blocs pour la recherche.");
        assertTrue(portal.withinArrivalWidth(1.5, 9.0, 2.4), "En face de l'ouverture d'arrivee, a toute hauteur.");
        assertFalse(portal.withinArrivalWidth(1.5, 1.0, 2.6), "A cote du cadre d'arrivee.");
        assertSame(portal.arrivalCrossingBox(), portal.arrivalCrossingBox(), "Elargie, gardee de meme.");
        portal.setDestinationPos(new Vec3d(4.5, 1.0, 1.5));
        assertEquals(new Box(4.25, -0.5, 0.5, 4.75, 2.5, 2.5), portal.arrivalBox(), "Recalculee quand l'arrivee bouge.");
        assertEquals(new Box(1.25, -3.5, -2.5, 7.75, 5.5, 5.5), portal.arrivalCrossingBox(), "Elargie, recalculee de meme.");
    }
}
