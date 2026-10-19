package com.gtgpuke.gtgsbackrooms.compat.iris;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Compatibilite avec le mod de shaders Iris. Avec un pack de shaders actif, Iris dessine le monde dans son propre
 * pipeline (geometrie dans ses tampons, eclairage differe, composition, passe finale ecrite dans la texture de couleur
 * de l'ecran hors de tout test de stencil) : une vue de portail dessinee au milieu du rendu vanilla n'y est qu'une
 * surface de plus, eclairee comme le cadre. Les vues passent alors par {@code ShaderPackPortalRenderer} : apres l'image
 * du monde du joueur, chaque vue est un rendu complet de son renderer ({@code WorldRenderer.render}), ou Iris deroule
 * tout son pipeline avec celui qu'il tient pour la dimension de la vue (un pipeline par dimension, comme il le fait deja
 * d'un changement de monde a l'autre), puis l'image du joueur est remise autour du cadre.
 *
 * <p>Seule l'API publique d'Iris ({@code IrisApi}, v0) est lue, depuis {@link IrisApiBridge}, et seulement quand le mod
 * est charge : sans lui, aucune classe d'Iris n'est touchee. Le reste du mod ne connait Iris que par cette classe.
 */
public final class IrisCompat {

    private static final String MOD_ID = "iris";
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded(MOD_ID);
    /** Pack actif, releve a la premiere instruction de chaque rendu du monde du joueur ({@link #refresh}) et lu partout ailleurs. */
    private static boolean shaderPackInUse;

    private IrisCompat() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** {@code true} si Iris est charge. */
    public static boolean isLoaded() {
        return LOADED;
    }

    /**
     * Releve si un pack de shaders est actif, une fois par image, a la premiere instruction vanilla de
     * {@code WorldRenderer.render} pour le monde du joueur : derriere la tete de la methode, ou Iris prepare son pipeline
     * pour l'image. Releve avant (en tete de {@code GameRenderer.renderWorld}), son API dit "pas de pack" sur la premiere
     * image apres une connexion ou un rechargement de pack. La decision vaut pour toute l'image (entities coupees par un
     * portail ou dessinees entieres, puis vues), quoi qu'il arrive entre-temps.
     */
    public static void refresh() {
        shaderPackInUse = LOADED && IrisApiBridge.shaderPackInUse();
    }

    /** {@code true} si un pack de shaders etait actif au debut de l'image : les vues de portail se dessinent apres le monde. */
    public static boolean shaderPackInUse() {
        return shaderPackInUse;
    }
}
