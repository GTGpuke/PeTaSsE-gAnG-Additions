package com.gtgpuke.gtgsbackrooms.core.portal.client.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.mixin.client.CameraAccessor;

@DisplayName("Roulis de la camera du joueur a la traversee d'un portail qui tourne autour d'un axe horizontal")
class CrossingRollTest {

    private static final float EPSILON = 1.0e-4f;
    private static final float HALF_TURN = (float) (Math.PI / 2.0);

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @AfterEach
    void reset() {
        CrossingRoll.clear();
    }

    private static Camera camera(float yaw, float pitch) {
        Camera camera = new Camera();
        ((CameraAccessor) camera).backrooms$setRotation(yaw, pitch);
        return camera;
    }

    private static Vec3d up(Quaternionf rotation) {
        Vector3f up = rotation.transform(new Vector3f(0.0f, 1.0f, 0.0f));
        return new Vec3d(up.x(), up.y(), up.z());
    }

    @Test
    @DisplayName("Tombe dans un sol de cote, sorti d'un mur : l'image vue est penchee d'un quart de tour")
    void floorToWallSidewaysRollsAQuarterTurn() {
        float roll = PortalViewTransform.crossingRoll(PortalFacing.UP, PortalFacing.NORTH, 90.0f, 0.0f);
        assertEquals(HALF_TURN, Math.abs(roll), EPSILON, "Regard vers l'ouest, le long de l'axe est-ouest autour duquel tourne le portail : un quart de tour de roulis.");
        // Applique en entier, le roulis rend exactement l'image vue par le cadre : le haut de l'image est celui transpose.
        Vec2f look = PortalFacing.transformLook(PortalFacing.UP, PortalFacing.NORTH, 90.0f, 0.0f);
        Quaternionf rolled = new Quaternionf(camera(look.x, look.y).getRotation()).rotateZ(roll);
        Vec3d expectedUp = PortalFacing.transformVector(PortalFacing.UP, PortalFacing.NORTH, up(camera(90.0f, 0.0f).getRotation()));
        assertEquals(0.0, expectedUp.subtract(up(rolled)).length(), EPSILON, "Le haut de l'image est celui de la vue.");
    }

    @Test
    @DisplayName("Aucun roulis entre deux cadres debout, ni en travers de l'axe du portail, ni quand le regard devient vertical")
    void noRollWhenTheImageStaysUpright() {
        assertEquals(0.0f, PortalViewTransform.crossingRoll(PortalFacing.NORTH, PortalFacing.EAST, 37.0f, 20.0f), EPSILON, "Debout vers debout.");
        assertEquals(0.0f, PortalViewTransform.crossingRoll(PortalFacing.UP, PortalFacing.NORTH, 180.0f, 30.0f), EPSILON,
                "Regard vers le nord, en travers de l'axe de rotation : le portail ne change que le tangage.");
        assertEquals(0.0f, PortalViewTransform.crossingRoll(PortalFacing.UP, PortalFacing.NORTH, 180.0f, 0.0f), EPSILON,
                "Regard devenu vertical : son cap garde deja le haut de l'image.");
        assertEquals(0.0f, PortalViewTransform.crossingRoll(PortalFacing.UP, PortalFacing.DOWN, 75.0f, 40.0f), EPSILON, "Du sol au plafond : une translation.");
        // Le lacet du joueur n'est jamais ramene a un tour : chaque tour de plus change le signe de la rotation de sa camera.
        for (float turns = -3.0f; turns <= 3.0f; turns++) {
            assertEquals(0.0f, PortalViewTransform.crossingRoll(PortalFacing.SOUTH, PortalFacing.SOUTH, 180.0f + 360.0f * turns, 0.0f), EPSILON,
                    "Debout vers debout apres " + turns + " tours sur soi-meme.");
        }
    }

    @Test
    @DisplayName("Toutes orientations, tous regards : le roulis rend l'image vue, en moins d'un demi-tour, et rien entre deux cadres debout")
    void rollMatchesTheViewForEveryPortalAndLook() {
        int rolled = 0;
        for (PortalFacing entry : PortalFacing.values()) {
            for (PortalFacing arrival : PortalFacing.values()) {
                boolean upright = entry.normal.y == 0.0 && arrival.normal.y == 0.0 || entry.normal.y + arrival.normal.y == 0.0 && entry.normal.y != 0.0;
                for (float yaw = -180.0f; yaw < 180.0f; yaw += 15.0f) {
                    for (float pitch = -90.0f; pitch <= 90.0f; pitch += 15.0f) {
                        String label = entry + " vers " + arrival + ", lacet " + yaw + ", tangage " + pitch;
                        float roll = PortalViewTransform.crossingRoll(entry, arrival, yaw, pitch);
                        // Un quaternion et son oppose sont la meme rotation : lu sans precaution, un roulis nul vaut un tour entier.
                        assertTrue(Math.abs(roll) <= Math.PI + EPSILON, label + " : au plus un demi-tour, jamais un tour complet.");
                        if (upright) {
                            assertEquals(0.0f, roll, EPSILON, label + " : l'image reste droite, aucun roulis.");
                        } else if (Math.abs(roll) > EPSILON) {
                            rolled++;
                        }
                        Vec2f look = PortalFacing.transformLook(entry, arrival, yaw, pitch);
                        Quaternionf seen = new Quaternionf(camera(look.x, look.y).getRotation()).rotateZ(roll);
                        Vec3d expectedUp = PortalFacing.transformVector(entry, arrival, up(camera(yaw, pitch).getRotation()));
                        assertEquals(0.0, expectedUp.subtract(up(seen)).length(), 1.0e-3, label + " : le haut de l'image est celui de la vue.");
                    }
                }
            }
        }
        assertTrue(rolled > 0, "Les portails qui tournent autour d'un axe horizontal penchent bien l'image.");
    }

    @Test
    @DisplayName("La camera part du roulis et se redresse en moins d'une demi-seconde, vite d'abord")
    void rollFadesOut() {
        CrossingRoll.start(0.8f, 1000L);
        assertTrue(CrossingRoll.active(), "Le redressement commence.");
        Camera start = camera(30.0f, 10.0f);
        CrossingRoll.apply(start, 1000L);
        assertEquals(0.0, up(new Quaternionf(camera(30.0f, 10.0f).getRotation()).rotateZ(0.8f)).subtract(up(start.getRotation())).length(), EPSILON,
                "A la traversee, tout le roulis.");
        Vector3f expectedVertical = start.getRotation().transform(new Vector3f(0.0f, 1.0f, 0.0f));
        assertTrue(expectedVertical.distance(start.getVerticalPlane()) < EPSILON, "Les directions de la camera suivent.");
        Camera early = camera(30.0f, 10.0f);
        CrossingRoll.apply(early, 900L);
        assertEquals(0.0, up(start.getRotation()).subtract(up(early.getRotation())).length(), EPSILON, "Une image d'avant la traversee : tout le roulis.");
        Camera half = camera(30.0f, 10.0f);
        CrossingRoll.apply(half, 1000L + CrossingRoll.DURATION_MS / 2);
        assertEquals(0.0, up(new Quaternionf(camera(30.0f, 10.0f).getRotation()).rotateZ(0.1f)).subtract(up(half.getRotation())).length(), EPSILON,
                "A mi-chemin, un huitieme du roulis (le cube de la moitie).");
        Camera done = camera(30.0f, 10.0f);
        CrossingRoll.apply(done, 1000L + CrossingRoll.DURATION_MS);
        assertFalse(CrossingRoll.active(), "Redresse : fini.");
        assertEquals(camera(30.0f, 10.0f).getRotation(), done.getRotation(), "La camera est droite.");
        Camera idle = camera(30.0f, 10.0f);
        CrossingRoll.apply(idle, 5000L);
        assertEquals(camera(30.0f, 10.0f).getRotation(), idle.getRotation(), "Sans traversee, rien.");
    }

    @Test
    @DisplayName("Un roulis imperceptible ne lance rien, et une deconnexion arrete le redressement")
    void negligibleRollAndClear() {
        CrossingRoll.start(1.0e-5f, 0L);
        assertFalse(CrossingRoll.active(), "Rien a redresser.");
        CrossingRoll.start(0.5f, 0L);
        CrossingRoll.clear();
        assertFalse(CrossingRoll.active(), "Deconnexion : plus rien.");
    }
}
