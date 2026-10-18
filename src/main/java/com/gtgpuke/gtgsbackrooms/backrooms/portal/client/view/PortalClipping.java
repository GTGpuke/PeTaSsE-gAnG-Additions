package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import net.minecraft.util.math.Vec3d;

/**
 * Plans de decoupe actifs pendant le dessin d'une vue de portail ou d'une entity a cheval sur un portail : la pile des
 * plans (la vue, puis la vue imbriquee ; ou la moitie d'une entity, dans le monde du joueur ou dans une vue qui couvre
 * l'ecran) et leur envoi aux shaders.
 *
 * <p>Le plan, en espace camera, vient de {@link PortalViewTransform#clipPlane} et part a chaque shader que vanilla
 * active ({@code MixinShaderProgram}) par l'uniforme ajoute par {@link ShaderClipInjector} ; le pilote coupe alors
 * tout ce qui est du mauvais cote ({@code GL_CLIP_DISTANCE0}). Un shader sans cet uniforme coupe la decoupe le temps
 * de son dessin : sa distance de decoupe serait indefinie. Rien n'est alloue, les plans sont des vecteurs reutilises.
 */
public final class PortalClipping {

    /** Une vue, puis un niveau de portail imbrique ; ou la moitie d'une entity, toujours sous une seule vue au plus. */
    private static final int MAX_DEPTH = 2;
    private static final Vector4f[] PLANES = {new Vector4f(), new Vector4f()};
    private static int depth;
    /** Plan courant ignore le temps d'une passe : dans une vue imbriquee, les entities engagees dans le cadre d'arrivee s'y dessinent entieres. */
    private static boolean suspended;

    private PortalClipping() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Active un plan par-dessus le plan courant s'il y en a un.
     *
     * @param planeNormal normale du plan, vers le cote a garder
     * @param planePoint un point du plan
     * @param cameraPos camera de la vue
     * @param positionMatrix matrice de vue (rotation seule) de cette camera
     */
    public static void push(Vec3d planeNormal, Vec3d planePoint, Vec3d cameraPos, Matrix4f positionMatrix) {
        if (depth == MAX_DEPTH) {
            throw new IllegalStateException("Plus de deux plans de decoupe empiles.");
        }
        PortalViewTransform.clipPlane(planeNormal, planePoint, cameraPos, positionMatrix, PLANES[depth]);
        depth++;
        GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
    }

    /** Retire le plan courant ; sans plan, la decoupe est coupee. */
    public static void pop() {
        depth--;
        if (depth == 0) {
            GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
        }
    }

    /** Ignore le plan courant, sans le retirer, jusqu'a {@link #resume()} : les shaders actives entre-temps ne coupent rien. */
    public static void suspend() {
        suspended = true;
        GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
    }

    /** Reprend la decoupe ignoree par {@link #suspend()}. */
    public static void resume() {
        suspended = false;
        if (depth > 0) {
            GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
        }
    }

    /** Nombre de plans empiles : 1 dans une vue ou pour la moitie d'une entity, 2 dans une vue imbriquee dans une autre. */
    public static int depth() {
        return depth;
    }

    /** {@code true} tant qu'un plan est empile. */
    public static boolean isActive() {
        return depth > 0;
    }

    /**
     * Un shader vient d'etre active : il recoit le plan courant, ou coupe la decoupe s'il ne connait pas l'uniforme.
     *
     * @param uniformLocation emplacement de l'uniforme dans ce shader, negatif s'il n'existe pas
     */
    public static void onProgramBound(int uniformLocation) {
        if (depth == 0) {
            return;
        }
        if (suspended || uniformLocation < 0) {
            GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
            return;
        }
        GL11.glEnable(GL30.GL_CLIP_DISTANCE0);
        Vector4f plane = PLANES[depth - 1];
        GL20.glUniform4f(uniformLocation, plane.x, plane.y, plane.z, plane.w);
    }
}
