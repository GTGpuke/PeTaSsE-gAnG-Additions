package com.petassegang.addons.backrooms.portal.client.view;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;

/**
 * Dessine la zone de vue d'un portail pour le stencil, la profondeur et le fond.
 *
 * <p>La zone n'est pas un simple quad mais une boite mince : le cadre, prolonge de {@link #DEPTH} derriere son
 * plan. Vue de face, sa silhouette est exactement le cadre. Quand la camera touche le plan, le plan proche de la
 * projection couperait un quad et laisserait un vide au bord de l'ecran ; la boite est donc decoupee en logiciel
 * contre ce plan proche, et la section ouverte est refermee par un polygone. La zone couvre ainsi toujours ce que
 * la camera voit a travers le cadre : ni plus (la dimension courante reste visible a cote du cadre), ni moins.
 *
 * <p>La boite se dessine sans elimination des faces arriere, le temps de son seul dessin : vanilla suppose cette
 * elimination toujours active (ses couches de terrain ne la retablissent pas), et une vue dessinee sans elle montrerait
 * les faces arriere de la glace, du verre et de l'eau, deux fois plus opaques qu'en vrai.
 */
public final class FrameAreaRenderer {

    /** Epaisseur de la boite derriere le plan : couvre le plan proche vanilla (0,05) meme vu de biais. */
    private static final double DEPTH = 0.2;
    /** Plan de decoupe en espace camera, un peu au-dela du plan proche vanilla pour que la section ne soit pas coupee a son tour. */
    private static final float CLIP_Z = -(GameRenderer.CAMERA_DEPTH + 0.002f);
    private static final float WELD_EPSILON = 1.0e-5f;
    private static final int CORNER_COUNT = 8;
    private static final int FACE_VERTICES = 4;
    /** Une face decoupee par un plan a au plus cinq sommets ; la section d'une boite au plus six, douze avant soudure. */
    private static final int CLIPPED_MAX = 5;
    private static final int CAP_MAX = 12;
    private static final Vector3f SCRATCH_VERTEX = new Vector3f();

    /** Coins de la boite en espace camera (x, y, z par coin) : bit 0 = +largeur, bit 1 = +hauteur, bit 2 = derriere le plan. */
    private static final float[] CORNERS = new float[CORNER_COUNT * 3];
    private static final float[] CLIPPED = new float[CLIPPED_MAX * 3];
    private static final float[] CAP = new float[CAP_MAX * 3];
    private static final float[] CAP_ANGLES = new float[CAP_MAX];
    private static int capCount;

    private static final int[][] FACES = {
            {0, 1, 3, 2}, // Plan du portail.
            {4, 6, 7, 5}, // Fond.
            {0, 4, 5, 1}, // Bas.
            {2, 3, 7, 6}, // Haut.
            {0, 2, 6, 4}, // Cote largeur negative.
            {1, 5, 7, 3}, // Cote largeur positive.
    };

    private FrameAreaRenderer() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Zone de vue, position seule (marquage du stencil) : le shader des positions est pose ici, les masques par l'appelant. */
    public static void draw(BackroomsPortalEntity portal, Vec3d cameraPos, Matrix4f positionMatrix) {
        RenderSystem.setShader(GameRenderer::getPositionProgram);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION);
        emit(buffer, portal, cameraPos, positionMatrix, false, 0.0f, 0.0f, 0.0f);
        drawIfAny(buffer);
    }

    /** Dessine sans eliminer les faces arriere : la boite est vue de dedans quand la camera touche le plan. */
    private static void drawIfAny(BufferBuilder buffer) {
        BuiltBuffer built = buffer.endNullable();
        if (built == null) {
            return;
        }
        RenderSystem.disableCull();
        try {
            BufferRenderer.drawWithGlobalProgram(built);
        } finally {
            RenderSystem.enableCull();
        }
    }

    /**
     * N'ecrit que la profondeur de la zone. Au plan lointain ({@code farPlane}), quelle que soit la profondeur deja
     * ecrite : avant de dessiner la vue, ou pour effacer la sienne. Sinon a sa vraie profondeur, la face la plus proche
     * de la boite l'emportant : juste apres un passage au plan lointain, pour tout ce qui se dessine ensuite devant le portail.
     */
    public static void writeDepth(BackroomsPortalEntity portal, Vec3d cameraPos, Matrix4f positionMatrix, boolean farPlane) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(farPlane ? GL11.GL_ALWAYS : GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(false, false, false, false);
        if (farPlane) {
            GL11.glDepthRange(1.0, 1.0);
        }
        try {
            draw(portal, cameraPos, positionMatrix);
        } finally {
            if (farPlane) {
                GL11.glDepthRange(0.0, 1.0);
            }
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }
    }

    /** Remplit la zone d'une couleur unie, sans toucher a la profondeur (horizon de la vue ou repli opaque). */
    public static void fill(BackroomsPortalEntity portal, Vec3d cameraPos, Matrix4f positionMatrix, float red, float green, float blue) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
            emit(buffer, portal, cameraPos, positionMatrix, true, red, green, blue);
            drawIfAny(buffer);
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }

    /** Toute la boite, decoupee contre le plan proche et refermee, en triangles. */
    private static void emit(
            BufferBuilder buffer,
            BackroomsPortalEntity portal,
            Vec3d cameraPos,
            Matrix4f positionMatrix,
            boolean colored,
            float red,
            float green,
            float blue) {
        computeCorners(portal, cameraPos, positionMatrix);
        capCount = 0;
        for (int[] face : FACES) {
            emitFan(buffer, CLIPPED, clipFace(face), colored, red, green, blue);
        }
        emitCap(buffer, colored, red, green, blue);
    }

    private static void computeCorners(BackroomsPortalEntity portal, Vec3d cameraPos, Matrix4f positionMatrix) {
        Vec3d center = portal.getPos();
        PortalFacing facing = portal.facingSource();
        double halfWidth = portal.width() / 2.0;
        double halfHeight = portal.height() / 2.0;
        for (int corner = 0; corner < CORNER_COUNT; corner++) {
            double sw = (corner & 1) != 0 ? halfWidth : -halfWidth;
            double sh = (corner & 2) != 0 ? halfHeight : -halfHeight;
            double sn = (corner & 4) != 0 ? -DEPTH : 0.0;
            float x = (float) (center.x - cameraPos.x + facing.axisW.x * sw + facing.axisH.x * sh + facing.normal.x * sn);
            float y = (float) (center.y - cameraPos.y + facing.axisW.y * sw + facing.axisH.y * sh + facing.normal.y * sn);
            float z = (float) (center.z - cameraPos.z + facing.axisW.z * sw + facing.axisH.z * sh + facing.normal.z * sn);
            positionMatrix.transformPosition(x, y, z, SCRATCH_VERTEX);
            CORNERS[corner * 3] = SCRATCH_VERTEX.x;
            CORNERS[corner * 3 + 1] = SCRATCH_VERTEX.y;
            CORNERS[corner * 3 + 2] = SCRATCH_VERTEX.z;
        }
    }

    /**
     * Decoupe une face contre le plan proche (Sutherland-Hodgman) : les sommets gardes vont dans {@link #CLIPPED},
     * les points d'intersection aussi dans {@link #CAP}.
     *
     * @return nombre de sommets de la face decoupee
     */
    private static int clipFace(int[] face) {
        int count = 0;
        for (int i = 0; i < FACE_VERTICES; i++) {
            int a = face[i] * 3;
            int b = face[(i + 1) % FACE_VERTICES] * 3;
            boolean insideA = CORNERS[a + 2] <= CLIP_Z;
            boolean insideB = CORNERS[b + 2] <= CLIP_Z;
            if (insideA) {
                count = append(CLIPPED, count, CORNERS[a], CORNERS[a + 1], CORNERS[a + 2]);
            }
            if (insideA != insideB) {
                float t = (CLIP_Z - CORNERS[a + 2]) / (CORNERS[b + 2] - CORNERS[a + 2]);
                float x = CORNERS[a] + (CORNERS[b] - CORNERS[a]) * t;
                float y = CORNERS[a + 1] + (CORNERS[b + 1] - CORNERS[a + 1]) * t;
                count = append(CLIPPED, count, x, y, CLIP_Z);
                if (capCount < CAP_MAX) {
                    capCount = append(CAP, capCount, x, y, CLIP_Z);
                }
            }
        }
        return count;
    }

    private static int append(float[] vertices, int count, float x, float y, float z) {
        vertices[count * 3] = x;
        vertices[count * 3 + 1] = y;
        vertices[count * 3 + 2] = z;
        return count + 1;
    }

    /** Section de la boite par le plan proche : points soudes, ordonnes autour de leur centre, en eventail. */
    private static void emitCap(BufferBuilder buffer, boolean colored, float red, float green, float blue) {
        int distinct = 0;
        for (int i = 0; i < capCount; i++) {
            float x = CAP[i * 3];
            float y = CAP[i * 3 + 1];
            boolean duplicate = false;
            for (int j = 0; j < distinct && !duplicate; j++) {
                duplicate = Math.abs(CAP[j * 3] - x) < WELD_EPSILON && Math.abs(CAP[j * 3 + 1] - y) < WELD_EPSILON;
            }
            if (!duplicate) {
                distinct = append(CAP, distinct, x, y, CLIP_Z);
            }
        }
        if (distinct < 3) {
            return;
        }
        float centerX = 0.0f;
        float centerY = 0.0f;
        for (int i = 0; i < distinct; i++) {
            centerX += CAP[i * 3];
            centerY += CAP[i * 3 + 1];
        }
        centerX /= distinct;
        centerY /= distinct;
        for (int i = 0; i < distinct; i++) {
            CAP_ANGLES[i] = (float) Math.atan2(CAP[i * 3 + 1] - centerY, CAP[i * 3] - centerX);
        }
        // Tri par insertion : six points au plus.
        for (int i = 1; i < distinct; i++) {
            float angle = CAP_ANGLES[i];
            float x = CAP[i * 3];
            float y = CAP[i * 3 + 1];
            int j = i - 1;
            while (j >= 0 && CAP_ANGLES[j] > angle) {
                CAP_ANGLES[j + 1] = CAP_ANGLES[j];
                CAP[(j + 1) * 3] = CAP[j * 3];
                CAP[(j + 1) * 3 + 1] = CAP[j * 3 + 1];
                j--;
            }
            CAP_ANGLES[j + 1] = angle;
            CAP[(j + 1) * 3] = x;
            CAP[(j + 1) * 3 + 1] = y;
        }
        emitFan(buffer, CAP, distinct, colored, red, green, blue);
    }

    private static void emitFan(BufferBuilder buffer, float[] vertices, int count, boolean colored, float red, float green, float blue) {
        for (int i = 1; i + 1 < count; i++) {
            emitVertex(buffer, vertices, 0, colored, red, green, blue);
            emitVertex(buffer, vertices, i, colored, red, green, blue);
            emitVertex(buffer, vertices, i + 1, colored, red, green, blue);
        }
    }

    private static void emitVertex(BufferBuilder buffer, float[] vertices, int index, boolean colored, float red, float green, float blue) {
        buffer.vertex(vertices[index * 3], vertices[index * 3 + 1], vertices[index * 3 + 2]);
        if (colored) {
            buffer.color(red, green, blue, 1.0f);
        }
    }
}
