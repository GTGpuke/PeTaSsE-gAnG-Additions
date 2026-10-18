package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.sound;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;

@DisplayName("Son a travers un portail")
class PortalSoundTest {

    private static final double EPSILON = 1.0e-6;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    /** Portail regardant le nord en (10.5, 65, 10.5), d'un bloc sur deux : son cote d'entree est vers les Z decroissants. */
    private static BackroomsPortalEntity portal() {
        return BackroomsPortalEntity.builder(null)
                .position(new Vec3d(10.5, 65.0, 10.5))
                .facingSource(PortalFacing.NORTH)
                .facingDestination(PortalFacing.EAST)
                .destinationDim(World.NETHER)
                .destinationPos(new Vec3d(100.5, 70.0, -40.5))
                .size(1.0f, 2.0f)
                .build();
    }

    @Test
    @DisplayName("La portee d'un son suit son volume, entre seize et soixante-quatre blocs")
    void radiusFollowsVolume() {
        assertEquals(PortalSound.MIN_RADIUS, PortalSound.radius(0.2f), EPSILON, "Un son faible porte quand meme seize blocs.");
        assertEquals(32.0, PortalSound.radius(2.0f), EPSILON, "Seize blocs par unite de volume.");
        assertEquals(PortalSound.MAX_RADIUS, PortalSound.radius(10000.0f), EPSILON, "Un tonnerre ne porte pas au-dela de soixante-quatre blocs.");
    }

    @Test
    @DisplayName("Le trajet jusqu'au cadre consomme un seizieme de volume par bloc, sans descendre sous zero")
    void travelToTheFrameCostsVolume() {
        assertEquals(1.0f, PortalSound.volumeThroughPortal(1.0f, 0.0), 1.0e-6f, "Un son colle au cadre passe entier.");
        assertEquals(0.5f, PortalSound.volumeThroughPortal(1.0f, 8.0), 1.0e-6f, "A huit blocs, la moitie.");
        assertEquals(0.0f, PortalSound.volumeThroughPortal(1.0f, 40.0), 1.0e-6f, "Trop loin : le son s'est eteint en chemin.");
    }

    @Test
    @DisplayName("Un son ressort du cadre par lequel l'auditeur voit l'autre cote, s'il est de l'autre cote et a portee")
    void soundLeavesThroughTheFrameTheListenerLooksThrough() {
        BackroomsPortalEntity portal = portal();
        Vec3d listener = new Vec3d(10.5, 65.0, 6.5);
        assertEquals(new Vec3d(10.5, 65.0, 10.5), PortalSound.exitPoint(portal, new Vec3d(10.5, 65.0, 14.5), listener, 16.0),
                "Dans l'axe, derriere le cadre : il ressort au milieu du cadre.");
        assertEquals(new Vec3d(11.0, 65.0, 10.5), PortalSound.exitPoint(portal, new Vec3d(13.5, 65.0, 14.5), listener, 16.0),
                "De biais : il ressort au bord du cadre le plus proche.");
        assertNull(PortalSound.exitPoint(portal, new Vec3d(10.5, 65.0, 8.5), listener, 16.0), "Une source de ce cote n'est pas vue par le cadre.");
        assertNull(PortalSound.exitPoint(portal, new Vec3d(10.5, 65.0, 14.5), new Vec3d(10.5, 65.0, 12.5), 16.0),
                "Un auditeur derriere la face n'entend rien par elle.");
        assertNull(PortalSound.exitPoint(portal, new Vec3d(10.5, 65.0, 30.5), listener, 16.0), "Une source trop loin du cadre.");
        assertNull(PortalSound.exitPoint(portal, new Vec3d(10.5, 65.0, 14.5), new Vec3d(10.5, 65.0, -9.5), 16.0),
                "Un auditeur trop loin du cadre.");
    }
}
