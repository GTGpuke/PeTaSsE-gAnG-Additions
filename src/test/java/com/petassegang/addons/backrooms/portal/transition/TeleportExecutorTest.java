package com.petassegang.addons.backrooms.portal.transition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.MinecraftTestBootstrap;
import com.petassegang.addons.backrooms.BackroomsConstants;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;

@DisplayName("Detection de traversee d'un portail")
class TeleportExecutorTest {

    private static BackroomsPortalEntity portal;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
        // Portail 1x2 oriente nord, centre en (0, 64, 0) : le cadre va de y=63 a y=65 et de x=-0.5 a x=0.5.
        portal = BackroomsPortalEntity.builder(null)
                .position(new Vec3d(0.0, 64.0, 0.0))
                .facingSource(PortalFacing.NORTH)
                .facingDestination(PortalFacing.SOUTH)
                .destinationDim(BackroomsConstants.LEVEL_ZERO_WORLD)
                .destinationPos(new Vec3d(1.5, 2.0, 1.5))
                .size(1.0f, 2.0f)
                .build();
    }

    @Test
    @DisplayName("Marcher du cote d'entree vers l'arriere, dans le cadre, compte comme une traversee")
    void walkingThroughCounts() {
        assertTrue(TeleportExecutor.crossedPortalPlane(portal, new Vec3d(0.0, 63.0, -0.5), new Vec3d(0.0, 63.0, 0.3)),
                "Du nord vers le sud, les pieds au bas du cadre.");
        assertTrue(TeleportExecutor.crossedPortalPlane(portal, new Vec3d(0.9, 63.0, -0.2), new Vec3d(0.9, 63.0, 0.1)),
                "Juste dans la marge laterale.");
    }

    @Test
    @DisplayName("Revenir par l'arriere, passer a cote ou s'arreter avant le plan ne compte pas")
    void otherMovesDoNotCount() {
        assertFalse(TeleportExecutor.crossedPortalPlane(portal, new Vec3d(0.0, 63.0, 0.3), new Vec3d(0.0, 63.0, -0.5)),
                "Du sud vers le nord : mauvais sens.");
        assertFalse(TeleportExecutor.crossedPortalPlane(portal, new Vec3d(3.0, 63.0, -0.5), new Vec3d(3.0, 63.0, 0.3)),
                "Trois blocs a cote du cadre.");
        assertFalse(TeleportExecutor.crossedPortalPlane(portal, new Vec3d(0.0, 63.0, -2.0), new Vec3d(0.0, 63.0, -0.1)),
                "Le plan n'est pas atteint.");
        assertFalse(TeleportExecutor.crossedPortalPlane(portal, new Vec3d(0.0, 70.0, -0.5), new Vec3d(0.0, 70.0, 0.3)),
                "Bien au-dessus du cadre.");
    }

    @Test
    @DisplayName("La variante sans allocation donne le meme verdict que celle a vecteurs")
    void primitiveOverloadMatches() {
        assertTrue(TeleportExecutor.crossedPortalPlane(portal, 0.0, 63.0, -0.5, 0.0, 63.0, 0.3), "Traversee dans le cadre.");
        assertFalse(TeleportExecutor.crossedPortalPlane(portal, 0.0, 63.0, 0.3, 0.0, 63.0, -0.5), "Mauvais sens.");
        assertFalse(TeleportExecutor.crossedPortalPlane(portal, 3.0, 63.0, -0.5, 3.0, 63.0, 0.3), "A cote du cadre.");
        assertFalse(TeleportExecutor.crossedPortalPlane(portal, 0.0, 63.0, -0.5, 0.0, 63.0, -0.5), "Immobile du cote d'entree.");
    }

    @Test
    @DisplayName("Un franchissement hors du cadre, a gauche ou en dessous, ne compte pas")
    void crossingOutsideTheFrameLeftOrBelow() {
        assertFalse(TeleportExecutor.crossedPortalPlane(portal, new Vec3d(-3.0, 63.0, -0.5), new Vec3d(-3.0, 63.0, 0.3)), "Trop a l'ouest du cadre.");
        assertFalse(TeleportExecutor.crossedPortalPlane(portal, new Vec3d(0.0, 58.0, -0.5), new Vec3d(0.0, 58.0, 0.3)), "Sous le cadre.");
    }
}
