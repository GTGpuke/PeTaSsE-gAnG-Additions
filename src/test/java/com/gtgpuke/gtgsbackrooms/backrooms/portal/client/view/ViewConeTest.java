package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@DisplayName("Volume visible a travers un cadre")
class ViewConeTest {

    /** Cadre de 1 x 2 centre en (0, 65, 0), largeur selon X, hauteur selon Y, contenu vers +Z. */
    private static final Vec3d CENTER = new Vec3d(0.0, 65.0, 0.0);
    private static final Vec3d AXIS_W = new Vec3d(1.0, 0.0, 0.0);
    private static final Vec3d AXIS_H = new Vec3d(0.0, 1.0, 0.0);
    private static final Vec3d NORMAL = new Vec3d(0.0, 0.0, 1.0);
    /** Camera 5 blocs devant le cadre, du cote oppose au contenu. */
    private static final Vec3d APEX = new Vec3d(0.0, 65.0, -5.0);

    private static ViewCone cone(Vec3d apex) {
        ViewCone cone = new ViewCone();
        cone.set(apex, CENTER, AXIS_W, AXIS_H, NORMAL, 0.5, 1.0);
        return cone;
    }

    @Test
    @DisplayName("Ce qui est derriere le cadre, dans la pyramide, est garde ; la pyramide s'elargit avec la distance")
    void contentInsideThePyramidIsKept() {
        ViewCone cone = cone(APEX);
        assertTrue(cone.intersects(new Box(-0.4, 64.5, 1.0, 0.4, 65.5, 2.0)), "Juste derriere le cadre, dans l'axe.");
        // A 25 blocs de la camera, la demi-largeur visible vaut 0,5 x 25 / 5 = 2,5.
        assertTrue(cone.intersects(new Box(1.5, 64.5, 20.0, 2.5, 65.5, 21.0)), "Loin derriere, decale mais dans la pyramide.");
        assertFalse(cone.intersects(new Box(3.0, 64.5, 20.0, 4.0, 65.5, 21.0)), "Loin derriere, au-dela du bord de la pyramide.");
    }

    @Test
    @DisplayName("Ce qui est devant le plan du cadre est coupe, meme dans l'axe de la pyramide")
    void contentInFrontOfTheFrameIsCut() {
        ViewCone cone = cone(APEX);
        assertFalse(cone.intersects(new Box(-0.4, 64.5, -2.0, 0.4, 65.5, -1.0)), "Entre la camera et le cadre.");
        assertFalse(cone.intersects(new Box(-0.4, 64.5, -9.0, 0.4, 65.5, -8.0)), "Derriere la camera.");
    }

    @Test
    @DisplayName("Ce qui est a cote ou au-dessus du cadre est coupe")
    void contentBesideTheFrameIsCut() {
        ViewCone cone = cone(APEX);
        // A 6 blocs de la camera, la demi-largeur visible vaut 0,6 et la demi-hauteur 1,2.
        assertFalse(cone.intersects(new Box(2.0, 64.5, 0.5, 3.0, 65.5, 1.5)), "A cote du cadre.");
        assertFalse(cone.intersects(new Box(-0.4, 67.0, 0.5, 0.4, 68.0, 1.5)), "Au-dessus du cadre.");
        assertFalse(cone.intersects(new Box(-0.4, 62.0, 0.5, 0.4, 63.0, 1.5)), "Sous le cadre.");
    }

    @Test
    @DisplayName("Une boite a cheval sur une arete de la pyramide est gardee : le test est conservateur")
    void boxesStraddlingAnEdgeAreKept() {
        ViewCone cone = cone(APEX);
        assertTrue(cone.intersects(new Box(0.0, 64.0, 0.5, 1.0, 66.0, 1.5)), "Deborde du cadre a droite.");
        assertTrue(cone.intersects(new Box(-8.0, 60.0, -1.0, 8.0, 70.0, 1.0)), "Enorme boite qui contient le cadre.");
    }

    @Test
    @DisplayName("Camera du cote du contenu : la pyramide se retourne vers le cadre et le plan coupe toujours")
    void cameraOnTheContentSideStillBoundsThePyramid() {
        ViewCone cone = cone(new Vec3d(0.0, 65.0, 5.0));
        assertTrue(cone.intersects(new Box(-0.4, 64.5, 1.0, 0.4, 65.5, 2.0)), "Entre la camera et le cadre, dans la pyramide.");
        // A 4 blocs de la camera, la demi-largeur visible vaut 0,4 : la boite commence a 2.
        assertFalse(cone.intersects(new Box(2.0, 64.5, 1.0, 3.0, 65.5, 2.0)), "A cote de la pyramide.");
        assertFalse(cone.intersects(new Box(-0.4, 64.5, -2.0, 0.4, 65.5, -1.0)), "De l'autre cote du plan du cadre.");
    }

    @Test
    @DisplayName("Camera dans le plan du cadre : plus de pyramide, seul le plan coupe")
    void cameraInTheFramePlaneKeepsOnlyThePlane() {
        ViewCone cone = cone(new Vec3d(0.0, 65.0, 0.0));
        assertTrue(cone.intersects(new Box(5.0, 64.0, 1.0, 6.0, 66.0, 2.0)), "Loin a cote, mais derriere le plan.");
        assertFalse(cone.intersects(new Box(-0.4, 64.5, -2.0, 0.4, 65.5, -1.0)), "Devant le plan.");
    }

    @Test
    @DisplayName("Une instance se redefinit d'une image a l'autre")
    void instanceIsReused() {
        ViewCone cone = cone(APEX);
        assertFalse(cone.intersects(new Box(1.5, 64.5, 0.5, 2.5, 65.5, 1.5)), "Coupe par la pyramide depuis 5 blocs.");
        cone.set(new Vec3d(0.0, 65.0, -0.5), CENTER, AXIS_W, AXIS_H, NORMAL, 0.5, 1.0);
        assertTrue(cone.intersects(new Box(1.5, 64.5, 0.5, 2.5, 65.5, 1.5)), "Depuis un demi-bloc, la pyramide est bien plus ouverte.");
    }

    @Test
    @DisplayName("Sans limite, quand la camera est passee de l'autre cote, tout est garde, meme devant le plan")
    void unboundedKeepsEverything() {
        ViewCone cone = cone(APEX);
        cone.unbounded();
        assertTrue(cone.intersects(new Box(2.0, 64.5, 0.5, 3.0, 65.5, 1.5)), "Hors de la pyramide.");
        assertTrue(cone.intersects(new Box(-0.4, 64.5, -2.0, 0.4, 65.5, -1.0)), "Devant le plan.");
    }
}
