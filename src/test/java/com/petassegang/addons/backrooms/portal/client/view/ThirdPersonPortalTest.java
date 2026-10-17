package com.petassegang.addons.backrooms.portal.client.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.MinecraftTestBootstrap;
import com.petassegang.addons.backrooms.BackroomsConstants;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;

@DisplayName("Camera a la troisieme personne derriere un portail")
class ThirdPersonPortalTest {

    private static final double EPSILON = 1.0e-9;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    /** Portail 1x2 oriente sud (on y entre depuis z > z0), centre en (0.5, 64, z0). */
    private static BackroomsPortalEntity southPortal(double z, boolean visible) {
        return BackroomsPortalEntity.builder(null)
                .position(new Vec3d(0.5, 64.0, z))
                .facingSource(PortalFacing.SOUTH)
                .facingDestination(PortalFacing.NORTH)
                .destinationDim(BackroomsConstants.LEVEL_ZERO_WORLD)
                .destinationPos(new Vec3d(0.5, 64.0, 100.0))
                .size(1.0f, 2.0f)
                .visible(visible)
                .build();
    }

    @Test
    @DisplayName("Le segment oeil-camera ne compte que s'il passe le plan de face, et a quelle fraction")
    void crossingFraction() {
        BackroomsPortalEntity portal = southPortal(0.0, true);
        assertEquals(0.25, ThirdPersonPortal.crossingFraction(portal, new Vec3d(0.5, 64.6, 1.0), new Vec3d(0.5, 64.6, -3.0)), EPSILON,
                "Un bloc devant, trois derriere.");
        assertEquals(Double.MAX_VALUE, ThirdPersonPortal.crossingFraction(portal, new Vec3d(0.5, 64.6, 2.0), new Vec3d(0.5, 64.6, 1.0)),
                "Les deux devant.");
        assertEquals(Double.MAX_VALUE, ThirdPersonPortal.crossingFraction(portal, new Vec3d(0.5, 64.6, -1.0), new Vec3d(0.5, 64.6, 3.0)),
                "De derriere vers l'avant.");
    }

    @Test
    @DisplayName("Juste apres le passage, la camera est de l'autre cote ; quand son recul sort du cadre, elle revient")
    void cameraBehindTheFrame() {
        BackroomsPortalEntity portal = southPortal(0.0, true);
        Vec3d eye = new Vec3d(0.5, 64.62, 0.3);
        assertSame(portal, ThirdPersonPortal.crossedBy(List.of(portal), eye, new Vec3d(0.5, 65.3, -3.6)),
                "Le recul passe le plan dans le cadre : vue de l'autre cote.");
        assertNull(ThirdPersonPortal.crossedBy(List.of(portal), new Vec3d(0.5, 64.62, 2.5), new Vec3d(0.5, 65.6, -1.4)),
                "Plus loin, regard incline : le recul passe au-dessus du linteau.");
        assertNull(ThirdPersonPortal.crossedBy(List.of(portal), eye, new Vec3d(0.5, 64.62, 4.3)), "Camera devant : rien a faire.");
        assertNull(ThirdPersonPortal.crossedBy(List.of(southPortal(0.0, false)), eye, new Vec3d(0.5, 65.3, -3.6)), "Portail invisible.");
    }

    @Test
    @DisplayName("De deux portails sur le recul, c'est le plus proche de l'oeil qui compte")
    void nearestPortalWins() {
        BackroomsPortalEntity near = southPortal(0.0, true);
        BackroomsPortalEntity far = southPortal(-2.0, true);
        Vec3d eye = new Vec3d(0.5, 64.62, 0.3);
        Vec3d camera = new Vec3d(0.5, 64.62, -3.7);
        assertSame(near, ThirdPersonPortal.crossedBy(List.of(far, near), eye, camera), "Le plus proche, dans un sens.");
        assertSame(near, ThirdPersonPortal.crossedBy(List.of(near, far), eye, camera), "Et dans l'autre.");
    }
}
