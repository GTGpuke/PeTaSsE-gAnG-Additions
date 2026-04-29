package com.petassegang.addons.backrooms.level.level0.generation.coord;

/**
 * Tranche verticale absolue utilisee par le writer pour une couche du Level 0.
 *
 * <p>Elle sert maintenant de contrat vertical actif entre :
 * le chunk generator, le writer, les details muraux et les tests de metadata.
 * Une slice ne decrit donc pas seulement des Y pratiques, mais une couche
 * complete et stable de la generation multi-layer.</p>
 *
 * <p>Le {@code ChunkGenerator} raisonne en listes de slices ; le writer ecrit
 * ensuite exactement dans la tranche qui lui est fournie.
 */
public record LevelZeroVerticalSlice(
        int layerIndex,
        int baseY,
        int bedrockMinY,
        int bedrockMaxY,
        int subfloorY,
        int floorY,
        int airMinY,
        int airMaxY,
        int ceilingY) {

    /**
     * Retourne la tranche monocouche legacy actuelle.
     *
     * @return tranche verticale absolue correspondant au rendu actuel
     */
    public static LevelZeroVerticalSlice legacySingleLayer() {
        return new LevelZeroVerticalSlice(
                0,
                LevelZeroVerticalLayout.floorY(),
                LevelZeroVerticalLayout.bedrockY(),
                LevelZeroVerticalLayout.bedrockY(),
                LevelZeroVerticalLayout.subfloorY(),
                LevelZeroVerticalLayout.floorY(),
                LevelZeroVerticalLayout.airMinY(),
                LevelZeroVerticalLayout.airMaxY(),
                LevelZeroVerticalLayout.ceilingY());
    }

    /**
     * Retourne la tranche verticale canonique d'un layer de la future pile multi-layer.
     *
     * @param layerIndex index du layer, base 0
     * @return tranche verticale absolue canonique
     */
    public static LevelZeroVerticalSlice canonicalLayer(int layerIndex) {
        // subfloorY reste a -1 tant qu'on n'a pas reintroduit de sous-couche
        // jouable specifique par layer. Le writer manipule aujourd'hui surtout
        // floor / air / ceiling / bedrock inter-layer.
        return new LevelZeroVerticalSlice(
                layerIndex,
                LevelZeroLayerStackLayout.baseY(layerIndex),
                LevelZeroLayerStackLayout.interLayerBedrockMinY(layerIndex),
                LevelZeroLayerStackLayout.interLayerBedrockMaxY(layerIndex),
                -1,
                LevelZeroLayerStackLayout.floorY(layerIndex),
                LevelZeroLayerStackLayout.airMinY(layerIndex),
                LevelZeroLayerStackLayout.airMaxY(layerIndex),
                LevelZeroLayerStackLayout.ceilingY(layerIndex));
    }

    /**
     * Retourne le premier Y de bedrock de la tranche.
     *
     * @return debut de la bedrock technique
     */
    public int bedrockY() {
        return bedrockMinY;
    }
}
