package com.petassegang.addons.backrooms.portal.propagation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.petassegang.addons.MinecraftTestBootstrap;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;

@DisplayName("Regard des mobs a travers un portail")
class PortalPursuitTest {

    private static final double EPSILON = 1.0e-9;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    /** Portail regardant le nord en (10.5, 65, 10.5), vers une arrivee regardant l'est. */
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
    @DisplayName("Un mob voit par le cadre seulement depuis le cote d'entree, a travers l'ouverture")
    void seesOnlyThroughTheFrame() {
        BackroomsPortalEntity portal = portal();
        Vec3d front = new Vec3d(10.5, 65.5, 6.5);
        Vec3d crossing = PortalPursuit.frameCrossing(portal, front, new Vec3d(10.5, 65.5, 14.5));
        assertNotNull(crossing, "En face, a travers l'ouverture.");
        assertEquals(0.0, portal.signedDistance(crossing), EPSILON, "Le point de passage est sur le plan.");
        assertEquals(10.5, crossing.x, EPSILON, "Au milieu du cadre.");
        assertNull(PortalPursuit.frameCrossing(portal, front, new Vec3d(16.5, 65.5, 14.5)), "En biais : la ligne passe a cote du cadre.");
        assertNull(PortalPursuit.frameCrossing(portal, new Vec3d(10.5, 65.5, 14.5), front), "Depuis l'arriere, le portail ne laisse rien voir.");
        assertNull(PortalPursuit.frameCrossing(portal, front, new Vec3d(10.5, 65.5, 8.5)), "Une image devant le plan n'est pas de l'autre cote.");
    }

    @Test
    @DisplayName("Le mob vise un point derriere le plan, au pied du cadre : son chemin traverse le portail")
    void throughPointIsBehindThePlaneAtFloorLevel() {
        BackroomsPortalEntity portal = portal();
        Vec3d through = PortalPursuit.throughPoint(portal);
        assertEquals(10.5, through.x, EPSILON, "Dans l'axe du cadre.");
        assertEquals(64.0, through.y, EPSILON, "Au pied du cadre.");
        assertEquals(-PortalPursuit.THROUGH_DEPTH, portal.signedDistance(through), EPSILON, "Derriere le plan.");
    }
}
