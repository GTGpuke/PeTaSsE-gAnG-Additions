package com.gtgpuke.gtgsbackrooms.core.portal.client.world;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.client.render.VertexFormats;

@DisplayName("Quadrilateres rognes au plan d'un portail, sur le processeur")
class ClippedVertexConsumersTest {

    private static final VertexFormat ENTITY = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;
    private static final float EPSILON = 1.0e-5f;
    private static final int RED = 3;
    private static final int TEXTURE_U = 7;
    private static final int NORMAL_Z = 15;

    @Test
    @DisplayName("Du cote garde, le quadrilatere passe entier ; de l'autre, rien")
    void wholeOrNothing() {
        Recorder kept = clip(new float[] {1.0f, 0.0f, 0.0f, 0.0f}, 1.0f, 2.0f, 0.0f, 1.0f);
        assertEquals(4, kept.vertices.size(), "Entierement garde : quatre sommets.");
        assertArrayEquals(new float[] {1.0f, 0.0f, 0.0f}, kept.position(0), EPSILON, "Premier sommet inchange.");
        assertArrayEquals(new float[] {1.0f, 1.0f, 0.0f}, kept.position(3), EPSILON, "Dernier sommet inchange.");
        assertEquals(0, clip(new float[] {1.0f, 0.0f, 0.0f, 0.0f}, -2.0f, -1.0f, 0.0f, 1.0f).vertices.size(),
                "Entierement de l'autre cote : rien.");
    }

    @Test
    @DisplayName("Coupe par le milieu : un quadrilatere qui s'arrete au plan, attributs interpoles")
    void halfQuad() {
        Recorder half = clip(new float[] {1.0f, 0.0f, 0.0f, 0.0f}, -1.0f, 1.0f, 0.0f, 1.0f);
        assertEquals(4, half.vertices.size(), "Moitie gardee : un quadrilatere.");
        assertArrayEquals(new float[] {0.0f, 0.0f, 0.0f}, half.position(0), EPSILON, "Point de coupe sur le bord du bas.");
        assertArrayEquals(new float[] {1.0f, 0.0f, 0.0f}, half.position(1), EPSILON, "Coin garde.");
        assertArrayEquals(new float[] {1.0f, 1.0f, 0.0f}, half.position(2), EPSILON, "Coin garde.");
        assertArrayEquals(new float[] {0.0f, 1.0f, 0.0f}, half.position(3), EPSILON, "Point de coupe sur le bord du haut.");
        assertEquals(0.5f, half.vertices.get(0)[TEXTURE_U], EPSILON, "Texture interpolee au point de coupe.");
        assertEquals(128.0f, half.vertices.get(0)[RED], EPSILON, "Couleur interpolee au point de coupe.");
        assertEquals(1.0f, half.vertices.get(0)[NORMAL_Z], EPSILON, "Normale gardee.");
    }

    @Test
    @DisplayName("Un coin coupe : un pentagone, ecrit en deux quadrilateres")
    void cornerCut() {
        // Garde x + y <= 1,5 sur le carre unite : seul le coin (1, 1) est de l'autre cote.
        Recorder corner = clip(new float[] {-1.0f, -1.0f, 0.0f, 1.5f}, 0.0f, 1.0f, 0.0f, 1.0f);
        assertEquals(8, corner.vertices.size(), "Cinq sommets : deux quadrilateres.");
        assertArrayEquals(corner.position(6), corner.position(7), EPSILON, "Le second se ferme en triangle.");
        for (int i = 0; i < corner.vertices.size(); i++) {
            float[] position = corner.position(i);
            assertTrue(position[0] + position[1] <= 1.5f + EPSILON, "Aucun sommet au-dela du plan.");
        }
    }

    @Test
    @DisplayName("Un seul coin garde : un triangle, son dernier sommet repete")
    void singleCorner() {
        Recorder triangle = clip(new float[] {1.0f, 1.0f, 0.0f, -1.5f}, 0.0f, 1.0f, 0.0f, 1.0f);
        assertEquals(4, triangle.vertices.size(), "Un triangle s'ecrit comme un quadrilatere.");
        assertArrayEquals(triangle.position(2), triangle.position(3), EPSILON, "Dernier sommet repete.");
        assertArrayEquals(new float[] {1.0f, 1.0f, 0.0f}, triangle.position(1), EPSILON, "Le coin garde.");
    }

    @Test
    @DisplayName("Un format aux attributs inconnus passe tel quel, sans decoupe")
    void unknownFormat() {
        // Attribut que le rogneur ne sait pas interpoler (identifiant libre du registre des attributs).
        VertexFormatElement extra = VertexFormatElement.register(31, 0, VertexFormatElement.ComponentType.FLOAT, VertexFormatElement.Usage.GENERIC, 1);
        VertexFormat format = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("Extra", extra).build();
        Recorder recorder = new Recorder();
        ClippedVertexConsumers.QuadClipper clipper = new ClippedVertexConsumers.QuadClipper(format, new float[] {1.0f, 0.0f, 0.0f, 0.0f});
        clipper.wrap(recorder);
        clipper.vertex(-5.0f, 0.0f, 0.0f).texture(0.25f, 0.75f);
        assertEquals(1, recorder.vertices.size(), "Sommet transmis aussitot.");
        assertArrayEquals(new float[] {-5.0f, 0.0f, 0.0f}, recorder.position(0), EPSILON, "Meme du cote coupe.");
        assertEquals(0.75f, recorder.vertices.get(0)[TEXTURE_U + 1], EPSILON, "Attributs transmis.");
    }

    @Test
    @DisplayName("Couches : le plan suit la camera, une couche de lignes passe telle quelle")
    void planeFromCamera() {
        float[] plane = new float[4];
        ClippedVertexConsumers.QuadClipper clipper = new ClippedVertexConsumers.QuadClipper(ENTITY, plane);
        // Equivalent de setPlane(normale +x, point (10, 0, 0), camera (12, 0, 0)) : garde x >= -2 en repere camera.
        plane[0] = 1.0f;
        plane[3] = 2.0f;
        Recorder recorder = new Recorder();
        clipper.wrap(recorder);
        quad(clipper, -3.0f, -1.0f, 0.0f, 1.0f);
        assertEquals(4, recorder.vertices.size(), "Un quadrilatere a cheval sur x = -2.");
        assertEquals(-2.0f, recorder.position(0)[0], EPSILON, "Coupe a x = -2, la ou est le plan vu de la camera.");
    }

    private static Recorder clip(float[] plane, float minX, float maxX, float minY, float maxY) {
        Recorder recorder = new Recorder();
        ClippedVertexConsumers.QuadClipper clipper = new ClippedVertexConsumers.QuadClipper(ENTITY, plane);
        clipper.wrap(recorder);
        quad(clipper, minX, maxX, minY, maxY);
        return recorder;
    }

    /** Un quadrilatere dans le plan z = 0 ; texture u et rouge suivent x de 0 a 1 et de 0 a 256 sur [-1, 1]. */
    private static void quad(VertexConsumer consumer, float minX, float maxX, float minY, float maxY) {
        corner(consumer, minX, minY);
        corner(consumer, maxX, minY);
        corner(consumer, maxX, maxY);
        corner(consumer, minX, maxY);
    }

    private static void corner(VertexConsumer consumer, float x, float y) {
        int red = Math.round((x + 1.0f) * 128.0f);
        consumer.vertex(x, y, 0.0f, 0xFF000000 | Math.min(red, 255) << 16, (x + 1.0f) / 2.0f, y, 0, 0xF000F0, 0.0f, 0.0f, 1.0f);
    }

    /** Enregistre ce qui lui est ecrit : position, couleur, texture, overlay, lumiere, normale par sommet. */
    private static final class Recorder implements VertexConsumer {

        private final List<float[]> vertices = new ArrayList<>();

        float[] position(int index) {
            float[] vertex = this.vertices.get(index);
            return new float[] {vertex[0], vertex[1], vertex[2]};
        }

        private float[] last() {
            return this.vertices.get(this.vertices.size() - 1);
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            float[] vertex = new float[16];
            vertex[0] = x;
            vertex[1] = y;
            vertex[2] = z;
            this.vertices.add(vertex);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            last()[RED] = red;
            last()[RED + 1] = green;
            last()[RED + 2] = blue;
            last()[RED + 3] = alpha;
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            last()[TEXTURE_U] = u;
            last()[TEXTURE_U + 1] = v;
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            last()[9] = u;
            last()[10] = v;
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            last()[11] = u;
            last()[12] = v;
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            last()[13] = x;
            last()[14] = y;
            last()[NORMAL_Z] = z;
            return this;
        }
    }
}
