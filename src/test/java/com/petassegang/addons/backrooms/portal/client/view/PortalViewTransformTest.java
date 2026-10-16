package com.petassegang.addons.backrooms.portal.client.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.petassegang.addons.MinecraftTestBootstrap;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;
import com.petassegang.addons.mixin.client.CameraAccessor;

@DisplayName("Projection oblique de la vue a travers un portail")
class PortalViewTransformTest {

    private static final Matrix4f PROJECTION = new Matrix4f().perspective((float) Math.toRadians(70.0), 16.0f / 9.0f, 0.05f, 100.0f);
    private static final Matrix4f IDENTITY_VIEW = new Matrix4f();

    private static float ndcDepth(Matrix4f projection, float viewZ) {
        Vector4f clip = new Vector4f(0.0f, 0.0f, viewZ, 1.0f);
        projection.transform(clip);
        return clip.z / clip.w;
    }

    @Test
    @DisplayName("Le plan du portail devient le plan proche : devant lui tout est coupe, derriere tout reste visible")
    void portalPlaneBecomesNearPlane() {
        // Camera a l'origine regardant vers -Z ; plan du portail a 5 blocs devant, cote visible au-dela.
        Matrix4f oblique = PortalViewTransform.obliqueProjection(PROJECTION, IDENTITY_VIEW,
                new Vec3d(0.0, 0.0, -1.0), new Vec3d(0.0, 0.0, -5.0), Vec3d.ZERO, new Matrix4f());
        assertTrue(ndcDepth(oblique, -4.0f) < -1.0f, "Un point entre la camera et le portail est coupe.");
        assertEquals(-1.0f, ndcDepth(oblique, -5.0f), 1.0e-4f, "Le plan du portail est exactement le plan proche.");
        float behind = ndcDepth(oblique, -6.0f);
        assertTrue(behind > -1.0f && behind <= 1.0f, "Un point derriere le portail est visible.");
        assertTrue(ndcDepth(oblique, -99.0f) <= 1.0f, "Le fond de la scene reste dans le volume.");
        assertEquals(ndcDepth(PROJECTION, -4.0f) < -1.0f, false, "La projection d'origine, elle, voit toujours ce point.");
    }

    @Test
    @DisplayName("Quand la camera est deja du cote visible, la projection est copiee telle quelle")
    void cameraOnVisibleSideKeepsProjection() {
        Matrix4f out = new Matrix4f();
        PortalViewTransform.obliqueProjection(PROJECTION, IDENTITY_VIEW,
                new Vec3d(0.0, 0.0, 1.0), new Vec3d(0.0, 0.0, -5.0), Vec3d.ZERO, out);
        assertEquals(PROJECTION, out, "Aucun clipping quand le plan est derriere la camera.");
    }

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
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
        Vec3d remote = PortalViewTransform.remoteCameraPos(portal, cameraPos);
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
}
