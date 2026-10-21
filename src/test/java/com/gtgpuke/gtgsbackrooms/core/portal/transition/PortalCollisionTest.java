package com.gtgpuke.gtgsbackrooms.core.portal.transition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.BackroomsPortalEntityRegistry;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;

@DisplayName("Collision a travers un portail")
class PortalCollisionTest {

    private static final double EPSILON = 1.0e-9;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    /** Portail 1x2 oriente nord, centre en (0, 64, 0) : on entre par le nord (z < 0). */
    private static BackroomsPortalEntity northPortal() {
        return BackroomsPortalEntity.builder(null)
                .position(new Vec3d(0.0, 64.0, 0.0))
                .facingSource(PortalFacing.NORTH)
                .facingDestination(PortalFacing.SOUTH)
                .destinationDim(BackroomsConstants.LEVEL_ZERO_WORLD)
                .destinationPos(new Vec3d(10.0, 64.0, 10.0))
                .size(1.0f, 2.0f)
                .build();
    }

    /** Portail oriente nord en {@code center}, vers une arrivee d'orientation donnee. */
    private static BackroomsPortalEntity portal(Vec3d center, PortalFacing exit, Vec3d destination) {
        return BackroomsPortalEntity.builder(null)
                .position(center)
                .facingSource(PortalFacing.NORTH)
                .facingDestination(exit)
                .destinationDim(BackroomsConstants.LEVEL_ZERO_WORLD)
                .destinationPos(destination)
                .size(1.0f, 2.0f)
                .build();
    }

    private static void assertBox(Box expected, Box actual, String message) {
        assertEquals(expected.minX, actual.minX, EPSILON, message);
        assertEquals(expected.minY, actual.minY, EPSILON, message);
        assertEquals(expected.minZ, actual.minZ, EPSILON, message);
        assertEquals(expected.maxX, actual.maxX, EPSILON, message);
        assertEquals(expected.maxY, actual.maxY, EPSILON, message);
        assertEquals(expected.maxZ, actual.maxZ, EPSILON, message);
    }

    @Test
    @DisplayName("Seule la partie d'une boite du cote de sortie du plan compte, quel que soit l'axe")
    void exitSideKeepsWhatIsBeyondThePlane() {
        Box box = new Box(0.0, 0.0, 0.0, 2.0, 2.0, 2.0);
        Vec3d center = new Vec3d(1.0, 1.0, 1.0);
        assertBox(new Box(1.0, 0.0, 0.0, 2.0, 2.0, 2.0), PortalCollision.exitSide(box, new Vec3d(1.0, 0.0, 0.0), center), "Vers l'est.");
        assertBox(new Box(0.0, 0.0, 0.0, 1.0, 2.0, 2.0), PortalCollision.exitSide(box, new Vec3d(-1.0, 0.0, 0.0), center), "Vers l'ouest.");
        assertBox(new Box(0.0, 1.0, 0.0, 2.0, 2.0, 2.0), PortalCollision.exitSide(box, new Vec3d(0.0, 1.0, 0.0), center), "Vers le haut.");
        assertBox(new Box(0.0, 0.0, 0.0, 2.0, 1.0, 2.0), PortalCollision.exitSide(box, new Vec3d(0.0, -1.0, 0.0), center), "Vers le bas.");
        assertBox(new Box(0.0, 0.0, 1.0, 2.0, 2.0, 2.0), PortalCollision.exitSide(box, new Vec3d(0.0, 0.0, 1.0), center), "Vers le sud.");
        assertBox(new Box(0.0, 0.0, 0.0, 2.0, 2.0, 1.0), PortalCollision.exitSide(box, new Vec3d(0.0, 0.0, -1.0), center), "Vers le nord.");
        Vec3d beyond = new Vec3d(2.0, 2.0, 2.0);
        assertNull(PortalCollision.exitSide(box, new Vec3d(1.0, 0.0, 0.0), beyond), "Entierement derriere un plan est-ouest.");
        assertNull(PortalCollision.exitSide(box, new Vec3d(0.0, 1.0, 0.0), beyond), "Entierement sous un plan horizontal.");
        assertNull(PortalCollision.exitSide(box, new Vec3d(0.0, 0.0, 1.0), beyond), "Entierement derriere un plan nord-sud.");
    }

    @Test
    @DisplayName("L'image d'une boite passe d'un cote a l'autre et revient a l'identique")
    void boxesCrossBothWays() {
        Vec3d source = new Vec3d(0.0, 64.0, 0.0);
        Vec3d destination = new Vec3d(10.0, 64.0, 10.0);
        Box box = new Box(0.0, 63.0, -1.0, 1.0, 65.0, 0.0);
        // Nord vers sud : simple translation, ce qui est devant l'entree est derriere la sortie.
        assertBox(new Box(10.0, 63.0, 9.0, 11.0, 65.0, 10.0),
                PortalCollision.toDestination(portal(source, PortalFacing.SOUTH, destination), box), "Nord vers sud.");
        // Nord vers est : un quart de tour, l'image reste une boite alignee sur les axes.
        BackroomsPortalEntity eastward = portal(source, PortalFacing.EAST, destination);
        Box turned = PortalCollision.toDestination(eastward, box);
        assertBox(new Box(9.0, 63.0, 9.0, 10.0, 65.0, 10.0), turned, "Nord vers est.");
        assertBox(box, PortalCollision.fromDestination(eastward, turned), "Aller-retour.");
    }

    @Test
    @DisplayName("Une entity n'est engagee que les pieds devant un portail ouvert, le centre en face de l'ouverture, la boite contre le cadre")
    void engagesOnlyInFrontAndInTheFrame() {
        BackroomsPortalEntity portal = northPortal();
        ItemEntity item = new ItemEntity(EntityType.ITEM, null);
        item.setPosition(0.3, 63.5, -0.3);
        assertTrue(PortalCollision.engages(portal, item, item.getBoundingBox().stretch(0.0, 0.0, 0.5)), "Devant le cadre, vers lui.");
        item.setPosition(0.3, 63.5, 0.3);
        assertFalse(PortalCollision.engages(portal, item, item.getBoundingBox()), "Les pieds derriere le plan.");
        item.setPosition(3.0, 63.5, -0.3);
        assertFalse(PortalCollision.engages(portal, item, item.getBoundingBox().stretch(0.0, 0.0, 0.5)), "Devant, mais a cote du cadre.");
        item.setPosition(0.6, 63.5, -0.3);
        assertFalse(PortalCollision.engages(portal, item, item.getBoundingBox().stretch(0.0, 0.0, 0.5)),
                "Le centre a cote du cadre, la boite contre son bord : on contourne le portail, rien de l'autre cote ne gene.");
        portal.setTeleportable(false);
        item.setPosition(0.3, 63.5, -0.3);
        assertFalse(PortalCollision.engages(portal, item, item.getBoundingBox().stretch(0.0, 0.0, 0.5)), "Portail ferme.");
        // Un cadre au sol s'engage aux yeux : les pieds deja sous le plan, les yeux encore au-dessus, l'objet est engage.
        BackroomsPortalEntity floor = BackroomsPortalEntity.builder(null).position(new Vec3d(0.5, 64.5, 0.0)).facingSource(PortalFacing.UP)
                .facingDestination(PortalFacing.DOWN).destinationDim(World.OVERWORLD).destinationPos(new Vec3d(10.5, 64.5, 0.0)).size(1.0f, 2.0f).build();
        item.setPosition(0.5, 64.4, 0.0);
        assertTrue(PortalCollision.engages(floor, item, item.getBoundingBox().stretch(0.0, -0.5, 0.0)), "Les yeux au-dessus du plan du sol : engage.");
        item.setPosition(0.5, 64.0, 0.0);
        assertFalse(PortalCollision.engages(floor, item, item.getBoundingBox().stretch(0.0, -0.5, 0.0)), "Les yeux passes : plus engage.");
    }

    @Test
    @DisplayName("Les portails viennent des mondes branches : ceux du client, ou le registre du serveur par defaut")
    void portalsComeFromThePluggedWorlds() {
        ItemEntity item = new ItemEntity(EntityType.ITEM, null);
        List<VoxelShape> collisions = List.of();
        boolean[] asked = {false};
        PortalCollision.setWorlds(new PortalCollision.Worlds() {
            @Override
            public List<BackroomsPortalEntity> portals(World world) {
                asked[0] = true;
                return List.of();
            }

            @Override
            public World destination(BackroomsPortalEntity portal) {
                return null;
            }
        });
        try {
            assertSame(collisions, PortalCollision.withOtherSide(item, null, collisions, item.getBoundingBox()),
                    "Aucun portail branche : les collisions vanilla, telles quelles.");
            assertTrue(asked[0], "Les portails viennent des mondes branches.");
        } finally {
            PortalCollision.setWorlds(PortalCollision.SERVER_WORLDS);
        }
        assertSame(BackroomsPortalEntityRegistry.all(), PortalCollision.SERVER_WORLDS.portals(null),
                "Par defaut, le registre des portails du serveur, tous mondes confondus.");
    }
}
