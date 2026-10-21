package com.gtgpuke.gtgsbackrooms.core.portal.client.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Projection a plan proche oblique")
class ObliqueProjectionTest {

    private static final float NEAR = 0.05f;
    private static final float FAR = 256.0f;
    private static final Matrix4f PROJECTION = new Matrix4f().perspective((float) Math.toRadians(70.0), 16.0f / 9.0f, NEAR, FAR);

    @Test
    @DisplayName("Le plan devient le plan proche : dessus a -1, devant coupe, le coin lointain garde +1, ordre et ecran gardes")
    void planeBecomesNearPlane() {
        // Garder ce qui est a plus de cinq blocs devant la camera (z <= -5 en espace camera).
        Vector4f plane = new Vector4f(0.0f, 0.0f, -1.0f, -5.0f);
        Matrix4f oblique = ObliqueProjection.apply(PROJECTION, plane, new Matrix4f());
        assertEquals(-1.0f, ndcZ(oblique, 0.3f, -0.2f, -5.0f), 1.0e-4f, "Un point du plan est sur le plan proche.");
        assertTrue(ndcZ(oblique, 0.0f, 0.0f, -3.0f) < -1.0f, "Un point entre la camera et le plan est coupe.");
        assertEquals(1.0f, ndcZ(oblique, 0.0f, 0.0f, -FAR), 1.0e-3f, "Le centre du plan lointain garde sa profondeur.");
        float near = ndcZ(oblique, 0.0f, 0.0f, -10.0f);
        float far = ndcZ(oblique, 0.0f, 0.0f, -100.0f);
        assertTrue(near > -1.0f && far > near && far < 1.0f, "Les points gardes s'ordonnent du plan vers le lointain : " + near + " < " + far + ".");
        assertEquals(ndcX(PROJECTION, 1.5f, 0.7f, -20.0f), ndcX(oblique, 1.5f, 0.7f, -20.0f), 1.0e-5f, "L'abscisse d'ecran ne change pas.");
        assertEquals(ndcY(PROJECTION, 1.5f, 0.7f, -20.0f), ndcY(oblique, 1.5f, 0.7f, -20.0f), 1.0e-5f, "L'ordonnee d'ecran ne change pas.");
    }

    @Test
    @DisplayName("Un plan incline garde la meme regle, de son cote")
    void tiltedPlane() {
        // Plan vu de biais : normale vers la gauche et le fond, a quatre blocs.
        Vector4f plane = new Vector4f(-0.6f, 0.0f, -0.8f, -4.0f);
        Matrix4f oblique = ObliqueProjection.apply(PROJECTION, plane, new Matrix4f());
        // Point du plan : -0.6x - 0.8z = 4 avec x = 0 donne z = -5.
        assertEquals(-1.0f, ndcZ(oblique, 0.0f, 1.0f, -5.0f), 1.0e-4f, "Un point du plan incline est sur le plan proche.");
        assertTrue(ndcZ(oblique, 2.0f, 0.0f, -3.0f) < -1.0f, "Du cote coupe du plan incline : coupe.");
        assertTrue(ndcZ(oblique, -2.0f, 0.0f, -8.0f) > -1.0f, "Du cote garde du plan incline : garde.");
        // Coin du plan lointain du cote du plan (NDC -1, 0, 1) : il garde sa profondeur, comme le centre pour un plan axial.
        assertEquals(1.0f, ndcZ(oblique, -FAR * (float) Math.tan(Math.toRadians(35.0)) * 16.0f / 9.0f, 0.0f, -FAR), 1.0e-3f,
                "Le coin lointain du cote du plan garde sa profondeur.");
    }

    @Test
    @DisplayName("Camera deja du cote garde : la projection d'origine sert telle quelle")
    void cameraOnKeptSide() {
        Vector4f behind = new Vector4f(0.0f, 0.0f, 1.0f, 2.0f);
        assertSame(PROJECTION, ObliqueProjection.apply(PROJECTION, behind, new Matrix4f()), "Rien a couper : meme matrice.");
    }

    private static float ndcZ(Matrix4f matrix, float x, float y, float z) {
        Vector4f clip = matrix.transform(new Vector4f(x, y, z, 1.0f));
        return clip.z / clip.w;
    }

    private static float ndcX(Matrix4f matrix, float x, float y, float z) {
        Vector4f clip = matrix.transform(new Vector4f(x, y, z, 1.0f));
        return clip.x / clip.w;
    }

    private static float ndcY(Matrix4f matrix, float x, float y, float z) {
        Vector4f clip = matrix.transform(new Vector4f(x, y, z, 1.0f));
        return clip.y / clip.w;
    }
}
