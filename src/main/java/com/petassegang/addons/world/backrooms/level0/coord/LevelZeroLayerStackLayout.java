package com.petassegang.addons.world.backrooms.level0.coord;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Schema vertical canonique vise par la spec multi-layer du Level 0.
 *
 * <p>Cette classe est maintenant la reference active de la pile verticale
 * multi-layer du Level 0. Elle reste aussi une source de verite testee pour
 * les Y canoniques du writer et de la dimension.</p>
 *
 * <p>Quand on cherche "ou se trouve le sol / l'air / le plafond / la bedrock
 * inter-layer ?", c'est ici que la reponse doit etre lue.
 */
public final class LevelZeroLayerStackLayout {

    /** Nombre de layers cible pour la premiere bascule multi-layer. */
    public static final int DEFAULT_LAYER_COUNT = 5;

    /** Hauteur technique de dimension, contrainte a un multiple de 16 par Minecraft. */
    public static final int DIMENSION_HEIGHT = 64;

    /** Nombre minimal de layers vise par la spec actuelle. */
    public static final int MIN_LAYER_COUNT = 3;

    /** Nombre maximal de layers vise par la spec actuelle. */
    public static final int MAX_LAYER_COUNT = 5;

    /** Ecart vertical entre deux layers successifs. */
    public static final int LAYER_PITCH = 10;

    /** Offset du sol visible a l'interieur d'un layer. */
    public static final int FLOOR_OFFSET = 0;

    /** Debut de l'air jouable a l'interieur d'un layer. */
    public static final int AIR_MIN_OFFSET = 1;

    /** Fin de l'air jouable a l'interieur d'un layer. */
    public static final int AIR_MAX_OFFSET = 4;

    /** Offset du plafond a l'interieur d'un layer. */
    public static final int CEILING_OFFSET = 5;

    /** Debut de la bedrock technique entre deux layers. */
    public static final int INTER_LAYER_BEDROCK_MIN_OFFSET = 6;

    /** Fin de la bedrock technique entre deux layers. */
    public static final int INTER_LAYER_BEDROCK_MAX_OFFSET = 9;

    private LevelZeroLayerStackLayout() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Retourne le Y de base d'un layer.
     *
     * @param layerIndex index du layer, base 0
     * @return Y de base du layer
     */
    public static int baseY(int layerIndex) {
        requireLayerIndex(layerIndex);
        return layerIndex * LAYER_PITCH;
    }

    /**
     * Retourne le Y du sol pour un layer donne.
     *
     * @param layerIndex index du layer, base 0
     * @return coordonnee Y du sol
     */
    public static int floorY(int layerIndex) {
        return baseY(layerIndex) + FLOOR_OFFSET;
    }

    /**
     * Retourne le Y minimal de l'air pour un layer donne.
     *
     * @param layerIndex index du layer, base 0
     * @return coordonnee Y minimale de l'air
     */
    public static int airMinY(int layerIndex) {
        return baseY(layerIndex) + AIR_MIN_OFFSET;
    }

    /**
     * Retourne le Y maximal de l'air pour un layer donne.
     *
     * @param layerIndex index du layer, base 0
     * @return coordonnee Y maximale de l'air
     */
    public static int airMaxY(int layerIndex) {
        return baseY(layerIndex) + AIR_MAX_OFFSET;
    }

    /**
     * Retourne le Y du plafond pour un layer donne.
     *
     * @param layerIndex index du layer, base 0
     * @return coordonnee Y du plafond
     */
    public static int ceilingY(int layerIndex) {
        return baseY(layerIndex) + CEILING_OFFSET;
    }

    /**
     * Retourne le Y minimal de la bedrock technique entre ce layer et le suivant.
     *
     * @param layerIndex index du layer, base 0
     * @return coordonnee Y minimale de la bedrock inter-layer
     */
    public static int interLayerBedrockMinY(int layerIndex) {
        return baseY(layerIndex) + INTER_LAYER_BEDROCK_MIN_OFFSET;
    }

    /**
     * Retourne le Y maximal de la bedrock technique entre ce layer et le suivant.
     *
     * @param layerIndex index du layer, base 0
     * @return coordonnee Y maximale de la bedrock inter-layer
     */
    public static int interLayerBedrockMaxY(int layerIndex) {
        return baseY(layerIndex) + INTER_LAYER_BEDROCK_MAX_OFFSET;
    }

    /**
     * Retourne le Y minimal canonique de la dimension multi-layer.
     *
     * @return Y minimal
     */
    public static int minimumY() {
        return 0;
    }

    /**
     * Retourne le nombre de layers cible par defaut.
     *
     * @return nombre de layers cible
     */
    public static int defaultLayerCount() {
        return DEFAULT_LAYER_COUNT;
    }

    /**
     * Retourne la hauteur recommandee pour un nombre donne de layers.
     *
     * @param layerCount nombre de layers cibles
     * @return hauteur logique recommandee de la dimension
     */
    public static int worldHeight(int layerCount) {
        requireLayerCount(layerCount);
        return layerCount * LAYER_PITCH;
    }

    /**
     * Retourne le sommet heightmap canonique pour un stack multi-layer.
     *
     * @param layerCount nombre de layers cibles
     * @return hauteur solide max + 1
     */
    public static int heightmapTopY(int layerCount) {
        requireLayerCount(layerCount);
        return ceilingY(layerCount - 1) + 1;
    }

    /**
     * Retourne la hauteur recommandee pour la premiere version multi-layer.
     *
     * @return hauteur logique recommandee
     */
    public static int recommendedWorldHeight() {
        return worldHeight(DEFAULT_LAYER_COUNT);
    }

    /**
     * Retourne la hauteur utile du contenu multi-layer.
     *
     * @return hauteur utile du contenu
     */
    public static int recommendedContentHeight() {
        return recommendedWorldHeight();
    }

    /**
     * Retourne la hauteur logique recommandee de la dimension cible.
     *
     * @return logical height recommandee
     */
    public static int recommendedLogicalHeight() {
        return DIMENSION_HEIGHT;
    }

    /**
     * Retourne la hauteur de dimension recommandee pour respecter les contraintes moteur.
     *
     * @return hauteur technique de dimension
     */
    public static int recommendedDimensionHeight() {
        return DIMENSION_HEIGHT;
    }

    /**
     * Retourne le sommet heightmap recommande pour la premiere bascule multi-layer.
     *
     * @return hauteur solide max + 1 recommandee
     */
    public static int recommendedHeightmapTopY() {
        return heightmapTopY(DEFAULT_LAYER_COUNT);
    }

    /**
     * Retourne les slices canoniques de la premiere pile multi-layer cible.
     *
     * @return liste ordonnee des slices canoniques
     */
    public static List<LevelZeroVerticalSlice> defaultSlices() {
        return slices(DEFAULT_LAYER_COUNT);
    }

    /**
     * Retourne les slices canoniques pour un nombre donne de layers.
     *
     * @param layerCount nombre de layers cibles
     * @return liste ordonnee des slices canoniques
     */
    public static List<LevelZeroVerticalSlice> slices(int layerCount) {
        requireLayerCount(layerCount);
        // L'ordre des slices est important : il sert a la fois au writer, au
        // chunk generator et aux tests de metadata verticale. On garde donc une
        // liste strictement stable, du layer 0 vers le layer le plus haut.
        return IntStream.range(0, layerCount)
                .mapToObj(LevelZeroVerticalSlice::canonicalLayer)
                .toList();
    }

    /**
     * Retourne true si le Y fourni correspond au premier bloc mural d'un layer canonique.
     *
     * @param y coordonnee verticale absolue
     * @return true si le Y correspond au debut de paroi d'un layer canonique
     */
    public static boolean isCanonicalWallBaseY(int y) {
        return y >= minimumY()
                && y < recommendedWorldHeight()
                && Math.floorMod(y, LAYER_PITCH) == AIR_MIN_OFFSET;
    }

    /**
     * Retourne la slice canonique correspondant a un Y interieur de layer.
     *
     * @param y coordonnee verticale absolue
     * @return slice canonique correspondante
     */
    public static LevelZeroVerticalSlice sliceAtY(int y) {
        if (y < minimumY() || y >= recommendedWorldHeight()) {
            throw new IllegalArgumentException("Le Y doit rester dans la pile canonique active.");
        }
        // Chaque layer occupe un pitch vertical fixe de 10 blocs. Le mapping
        // par division entiere reste donc volontairement trivial pour eviter
        // toute ambiguite entre rendu mural, writer et debug.
        return LevelZeroVerticalSlice.canonicalLayer(Math.floorDiv(y, LAYER_PITCH));
    }

    /**
     * Retourne true si le Y fourni est dans l'air jouable du layer.
     *
     * @param layerIndex index du layer, base 0
     * @param y coordonnee verticale a verifier
     * @return true si le Y est dans l'interieur jouable du layer
     */
    public static boolean isInteriorY(int layerIndex, int y) {
        return y >= airMinY(layerIndex) && y <= airMaxY(layerIndex);
    }

    /**
     * Retourne true si le Y fourni est dans la bedrock technique du layer.
     *
     * @param layerIndex index du layer, base 0
     * @param y coordonnee verticale a verifier
     * @return true si le Y est dans la tranche bedrock inter-layer
     */
    public static boolean isInterLayerBedrockY(int layerIndex, int y) {
        return y >= interLayerBedrockMinY(layerIndex)
                && y <= interLayerBedrockMaxY(layerIndex);
    }

    private static void requireLayerIndex(int layerIndex) {
        if (layerIndex < 0) {
            throw new IllegalArgumentException("Le layer doit etre >= 0.");
        }
    }

    private static void requireLayerCount(int layerCount) {
        if (layerCount < MIN_LAYER_COUNT || layerCount > MAX_LAYER_COUNT) {
            throw new IllegalArgumentException(
                    "Le nombre de layers doit etre compris entre "
                            + MIN_LAYER_COUNT + " et " + MAX_LAYER_COUNT + '.');
        }
    }
}
