package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ajoute le plan de decoupe des vues de portail aux vertex shaders au moment de leur chargement.
 *
 * <p>Tout vertex shader qui place ses sommets par {@code ModelViewMat} recoit un uniforme {@value #UNIFORM} et ecrit
 * {@code gl_ClipDistance[0]} : la distance signee du sommet au plan, en espace camera. Quand la decoupe est active
 * ({@link PortalClipping}), le pilote coupe tout ce qui est du mauvais cote du plan ; sinon cette distance est ignoree.
 * Ainsi la projection reste celle du joueur, la profondeur garde toute sa precision,
 * et le meme plan agit sur le terrain, les entities, le ciel et les nuages. La source arrive ici deja debarrassee de
 * ses {@code #moj_import}.
 */
public final class ShaderClipInjector {

    /** Nom de l'uniforme du plan : {@code (nx, ny, nz, d)}, avec {@code dot(plan, sommet) >= 0} du cote garde. */
    public static final String UNIFORM = "backrooms_ClipPlane";

    private static final Pattern MODEL_VIEW = Pattern.compile("\\buniform\\s+mat4\\s+ModelViewMat\\s*;");
    private static final Pattern POSITION = Pattern.compile("\\bin\\s+vec3\\s+Position\\s*;");
    private static final Pattern CHUNK_OFFSET = Pattern.compile("\\buniform\\s+vec3\\s+ChunkOffset\\s*;");
    private static final Pattern MAIN = Pattern.compile("\\bvoid\\s+main\\s*\\(\\s*\\)\\s*\\{");
    private static final String GL_POSITION = "gl_Position";
    private static final String DECLARATION = "uniform vec4 " + UNIFORM + ";\n";

    private ShaderClipInjector() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** {@code true} pour un vertex shader qui place ses sommets par {@code ModelViewMat} : il recoit le plan. */
    static boolean applies(String source) {
        return source.contains(GL_POSITION) && MODEL_VIEW.matcher(source).find() && POSITION.matcher(source).find();
    }

    /**
     * Source transformee : l'uniforme declare avant {@code main}, la distance de decoupe ecrite en premiere instruction.
     *
     * @return la source transformee, ou la source recue (meme instance) si le shader n'est pas concerne ou l'est deja
     */
    public static String inject(String source) {
        if (!applies(source) || source.contains(UNIFORM)) {
            return source;
        }
        Matcher main = MAIN.matcher(source);
        if (!main.find()) {
            return source;
        }
        // Les shaders de terrain placent leurs sommets par rapport a la section : meme decalage pour la distance.
        String position = CHUNK_OFFSET.matcher(source).find() ? "Position + ChunkOffset" : "Position";
        return source.substring(0, main.start())
                + DECLARATION
                + source.substring(main.start(), main.end())
                + "\n    gl_ClipDistance[0] = dot(" + UNIFORM + ", ModelViewMat * vec4(" + position + ", 1.0));"
                + source.substring(main.end());
    }
}
