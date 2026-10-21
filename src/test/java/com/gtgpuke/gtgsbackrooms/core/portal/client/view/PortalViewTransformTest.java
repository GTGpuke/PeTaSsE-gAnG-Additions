package com.gtgpuke.gtgsbackrooms.core.portal.client.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.mixin.client.CameraAccessor;

@DisplayName("Camera et plan de decoupe de la vue a travers un portail")
class PortalViewTransformTest {

    private static final Matrix4f IDENTITY_VIEW = new Matrix4f();
    private static final float EPSILON = 1.0e-5f;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    private static float distance(Vector4f plane, double x, double y, double z) {
        return plane.dot(new Vector4f((float) x, (float) y, (float) z, 1.0f));
    }

    @Test
    @DisplayName("Le plan de decoupe garde le cote du contenu : devant le cadre coupe, derriere garde, le plan lui-meme garde")
    void clipPlaneKeepsTheContentSide() {
        // Camera a l'origine regardant vers -Z ; cadre a 5 blocs devant, contenu au-dela (normale vers -Z).
        Vector4f out = new Vector4f();
        Vector4f plane = PortalViewTransform.clipPlane(new Vec3d(0.0, 0.0, -1.0), new Vec3d(0.0, 0.0, -5.0), Vec3d.ZERO, IDENTITY_VIEW, out);
        assertSame(out, plane, "Le plan est ecrit dans le vecteur fourni, sans allocation.");
        assertEquals(0.0f, plane.x, EPSILON, "Normale sans composante X.");
        assertEquals(0.0f, plane.y, EPSILON, "Normale sans composante Y.");
        assertEquals(-1.0f, plane.z, EPSILON, "Normale vers le contenu.");
        assertEquals(-5.0f, plane.w, EPSILON, "La camera est a 5 blocs du plan, du cote coupe.");
        assertTrue(distance(plane, 0.0, 0.0, -4.0) < 0.0f, "Un point entre la camera et le cadre est coupe.");
        assertTrue(distance(plane, 0.0, 0.0, -6.0) > 0.0f, "Un point derriere le cadre est garde.");
        assertEquals(0.0f, distance(plane, 3.0, -2.0, -5.0), EPSILON, "Un point du plan est a distance nulle, ou qu'il soit sur le plan.");
    }

    @Test
    @DisplayName("Le plan suit la rotation de la camera : il vit dans l'espace ou les shaders placent les sommets")
    void clipPlaneFollowsTheCamera() {
        Camera camera = new Camera();
        ((CameraAccessor) camera).backrooms$setRotation(90.0f, 0.0f);
        Matrix4f view = PortalViewTransform.positionMatrix(camera, new Matrix4f());
        Vec3d cameraPos = new Vec3d(10.0, 65.0, 10.0);
        // Cadre 6 blocs a l'ouest de la camera, contenu plus a l'ouest encore.
        Vector4f plane = PortalViewTransform.clipPlane(new Vec3d(-1.0, 0.0, 0.0), new Vec3d(4.0, 65.0, 10.0), cameraPos, view, new Vector4f());
        // Un point du monde passe par la meme matrice que les sommets : sa distance au plan est celle du monde.
        Vector4f kept = view.transform(new Vector4f(2.0f - 10.0f, 0.0f, 0.0f, 1.0f));
        Vector4f cut = view.transform(new Vector4f(7.0f - 10.0f, 0.0f, 0.0f, 1.0f));
        assertEquals(2.0f, plane.dot(kept), EPSILON, "Deux blocs derriere le cadre : distance +2.");
        assertEquals(-3.0f, plane.dot(cut), EPSILON, "Trois blocs devant le cadre : distance -3.");
        assertEquals(1.0f, Math.abs(plane.x) + Math.abs(plane.y) + Math.abs(plane.z), EPSILON, "La normale reste unitaire et alignee sur un axe camera.");
    }

    @Test
    @DisplayName("La camera distante est l'image de la camera du joueur a travers le portail, regard compris")
    void remoteCameraFollowsThePortal() {
        BackroomsPortalEntity portal = BackroomsPortalEntity.builder(null)
                .position(new Vec3d(10.0, 65.0, 10.0))
                .facingSource(PortalFacing.NORTH)
                .facingDestination(PortalFacing.SOUTH)
                .destinationDim(World.OVERWORLD)
                .destinationPos(new Vec3d(100.0, 65.0, 100.0))
                .size(1.0f, 2.0f)
                .build();
        // Deux blocs devant le portail nord (au nord) : deux blocs devant le portail sud (au sud), sans demi-tour.
        Vec3d cameraPos = new Vec3d(10.0, 66.0, 8.0);
        Vec3d remote = portal.toDestination(cameraPos);
        // La camera est devant l'entree ; son image se place derriere la sortie, pour voir a travers le cadre ce qui est devant.
        assertEquals(new Vec3d(100.0, 66.0, 98.0), remote, "Deux blocs devant l'entree : deux blocs derriere la sortie.");
        assertEquals(PortalFacing.transformPoint(PortalFacing.NORTH, portal.getPos(), PortalFacing.SOUTH, portal.destinationPos(), cameraPos),
                remote, "Meme transposition que celle des entities.");
        Vec2f look = PortalViewTransform.remoteLook(portal, 37.0f, 10.0f);
        Vec2f expected = PortalFacing.transformLook(PortalFacing.NORTH, PortalFacing.SOUTH, 37.0f, 10.0f);
        assertEquals(expected.x, look.x, 1.0e-6f, "Meme rotation du regard que celle des entities.");
        assertEquals(10.0f, look.y, 1.0e-4f, "Le pitch est conserve.");
    }

    @Test
    @DisplayName("La matrice de vue d'une camera est l'inverse de sa rotation")
    void positionMatrixFromCamera() {
        Camera camera = new Camera();
        assertEquals(new Matrix4f(), PortalViewTransform.positionMatrix(camera, new Matrix4f()), "Camera au repos : identite.");
        ((CameraAccessor) camera).backrooms$setRotation(90.0f, -20.0f);
        Matrix4f view = PortalViewTransform.positionMatrix(camera, new Matrix4f());
        assertEquals(new Matrix4f().rotation(camera.getRotation().conjugate(new Quaternionf())), view,
                "Comme GameRenderer : la rotation inverse de la camera, sans translation.");
    }

    /** Direction de la camera par sa rotation : devant (vanilla regarde vers -Z au repos) ou en haut. */
    private static Vec3d direction(Camera camera, float x, float y, float z) {
        Vector3f turned = camera.getRotation().transform(new Vector3f(x, y, z));
        return new Vec3d(turned.x(), turned.y(), turned.z());
    }

    private static BackroomsPortalEntity between(PortalFacing entry, PortalFacing arrival) {
        return BackroomsPortalEntity.builder(null).position(new Vec3d(0.5, 64.0, 0.5)).facingSource(entry).facingDestination(arrival)
                .destinationDim(World.NETHER).destinationPos(new Vec3d(10.5, 70.0, 10.5)).size(1.0f, 2.0f).build();
    }

    @Test
    @DisplayName("La rotation d'un portail envoie chaque direction sur son image, pour les 36 paires d'orientations")
    void portalRotationMatchesTheVectorTransform() {
        Vec3d sample = new Vec3d(0.3, -0.5, 0.7);
        for (PortalFacing entry : PortalFacing.values()) {
            for (PortalFacing arrival : PortalFacing.values()) {
                Vector3f turned = PortalViewTransform.portalRotation(entry, arrival, new Quaternionf())
                        .transform(new Vector3f((float) sample.x, (float) sample.y, (float) sample.z));
                Vec3d expected = PortalFacing.transformVector(entry, arrival, sample);
                assertEquals(0.0, expected.subtract(turned.x(), turned.y(), turned.z()).length(), EPSILON, entry + " vers " + arrival + ".");
            }
        }
    }

    @Test
    @DisplayName("La camera d'une vue tourne avec le portail, roulis compris : d'un sol a un mur, l'image se penche")
    void viewCameraRollsWithTheFloorToWallPortal() {
        // Le joueur regarde le cadre au sol de cote : vers l'ouest, cinquante degres vers le bas.
        Camera player = new Camera();
        ((CameraAccessor) player).backrooms$setRotation(90.0f, 50.0f);
        BackroomsPortalEntity floorToWall = between(PortalFacing.UP, PortalFacing.NORTH);
        Camera view = new Camera();
        PortalViewTransform.aim(view, floorToWall, player);
        Vec3d forward = PortalFacing.transformVector(PortalFacing.UP, PortalFacing.NORTH, direction(player, 0.0f, 0.0f, -1.0f));
        Vec3d up = PortalFacing.transformVector(PortalFacing.UP, PortalFacing.NORTH, direction(player, 0.0f, 1.0f, 0.0f));
        assertEquals(0.0, forward.subtract(direction(view, 0.0f, 0.0f, -1.0f)).length(), EPSILON, "Le regard est transpose.");
        assertEquals(0.0, up.subtract(direction(view, 0.0f, 1.0f, 0.0f)).length(), EPSILON, "Le haut de l'image aussi : la camera roule.");
        assertTrue(Math.abs(up.dotProduct(new Vec3d(forward.z, 0.0, -forward.x))) > 0.1,
                "Ce haut sort du plan vertical du regard : un lacet et un tangage seuls ne le donneraient pas.");
        Vec2f look = PortalViewTransform.remoteLook(floorToWall, 90.0f, 50.0f);
        assertEquals(look.x, view.getYaw(), EPSILON, "Lacet transpose, pour qui ne lit que le regard.");
        assertEquals(look.y, view.getPitch(), EPSILON, "Tangage transpose.");
        Vector3f horizontal = view.getRotation().transform(new Vector3f(0.0f, 0.0f, -1.0f));
        Vector3f vertical = view.getRotation().transform(new Vector3f(0.0f, 1.0f, 0.0f));
        Vector3f diagonal = view.getRotation().transform(new Vector3f(-1.0f, 0.0f, 0.0f));
        assertTrue(horizontal.distance(view.getHorizontalPlane()) < EPSILON && vertical.distance(view.getVerticalPlane()) < EPSILON
                && diagonal.distance(view.getDiagonalPlane()) < EPSILON, "Les directions que vanilla tire de la rotation suivent.");
        // Une vue vue a travers une autre part de la camera de celle-ci, roulis compris.
        Camera nested = new Camera();
        PortalViewTransform.aim(nested, between(PortalFacing.NORTH, PortalFacing.UP), view);
        assertEquals(0.0, direction(player, 0.0f, 1.0f, 0.0f).subtract(direction(nested, 0.0f, 1.0f, 0.0f)).length(), EPSILON,
                "Du sol au mur puis du mur au sol : le haut de l'image revient a celui du joueur.");
    }

    @Test
    @DisplayName("Entre deux cadres debout, la camera d'une vue ne roule pas ; la rotation complete se recopie")
    void standingPortalsDoNotRollAndRotationCopies() {
        Camera player = new Camera();
        ((CameraAccessor) player).backrooms$setRotation(37.0f, 20.0f);
        Camera view = new Camera();
        PortalViewTransform.aim(view, between(PortalFacing.NORTH, PortalFacing.EAST), player);
        Camera plain = new Camera();
        ((CameraAccessor) plain).backrooms$setRotation(view.getYaw(), view.getPitch());
        assertEquals(0.0, direction(plain, 0.0f, 1.0f, 0.0f).subtract(direction(view, 0.0f, 1.0f, 0.0f)).length(), EPSILON,
                "Le haut est celui d'un lacet et d'un tangage : aucun roulis.");
        Camera copy = new Camera();
        PortalViewTransform.copyRotation(view, copy);
        assertEquals(view.getRotation(), copy.getRotation(), "Rotation recopiee.");
        assertTrue(view.getHorizontalPlane().equals(copy.getHorizontalPlane()) && view.getVerticalPlane().equals(copy.getVerticalPlane())
                && view.getDiagonalPlane().equals(copy.getDiagonalPlane()), "Directions recopiees.");
    }
}
