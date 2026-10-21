package com.gtgpuke.gtgsbackrooms.core.portal.client.view;

import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Projection dont le plan proche est remplace par un plan quelconque de l'espace camera (methode d'Eric Lengyel) : le
 * pilote coupe tout ce qui est du mauvais cote du plan, sans rien demander aux shaders. Une vue de portail s'en sert
 * quand un autre mod dessine le terrain : ses shaders ignorent la distance de decoupe que {@code ShaderClipInjector}
 * ajoute a ceux de vanilla, et ce qui est entre la camera de la vue et le plan d'arrivee se dessinerait par-dessus la
 * vue. Le reste du temps, la projection du joueur suffit, le plan passe aux shaders ({@code PortalClipping}).
 *
 * <p>La precision de la profondeur s'en ressent loin du plan : deux faces lointaines qui se touchent peuvent scintiller.
 * Le frustum de culling garde la projection du joueur, {@code Frustum} exigeant une sous-matrice orthogonale.
 */
public final class ObliqueProjection {

    /** Coin du plan lointain et inverse de la projection, reutilises : le thread de rendu seulement. */
    private static final Vector4f CORNER = new Vector4f();
    private static final Matrix4f INVERSE = new Matrix4f();

    private ObliqueProjection() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Projection de meme ecran que {@code projection}, dont le plan proche est {@code plane} : un point du plan prend la
     * profondeur du plan proche, ce qui est devant lui est coupe, et le coin du plan lointain du cote du plan garde la
     * sienne.
     *
     * @param projection projection d'origine, en perspective
     * @param plane plan en espace camera, {@code (a, b, c, d)} avec {@code ax + by + cz + d >= 0} du cote garde
     * @param out matrice qui recoit le resultat
     * @return {@code out}, ou {@code projection} telle quelle si la camera n'est pas du cote coupe du plan : rien a couper
     */
    public static Matrix4f apply(Matrix4f projection, Vector4f plane, Matrix4f out) {
        if (plane.w >= 0.0f) {
            return projection;
        }
        // Coin du plan lointain du cote du plan, en espace camera (homogene) : la projection inverse de (+-1, +-1, 1, 1).
        INVERSE.set(projection).invert();
        CORNER.set(Math.signum(plane.x), Math.signum(plane.y), 1.0f, 1.0f);
        INVERSE.transform(CORNER);
        // Quatrieme ligne de la projection appliquee au coin : la profondeur homogene qu'il doit garder.
        float depth = projection.m03() * CORNER.x + projection.m13() * CORNER.y + projection.m23() * CORNER.z + projection.m33() * CORNER.w;
        float scale = 2.0f * depth / plane.dot(CORNER);
        out.set(projection);
        // Troisieme ligne : le plan, mis a l'echelle, moins la quatrieme ligne.
        out.m02(plane.x * scale - projection.m03());
        out.m12(plane.y * scale - projection.m13());
        out.m22(plane.z * scale - projection.m23());
        out.m32(plane.w * scale - projection.m33());
        return out;
    }
}
