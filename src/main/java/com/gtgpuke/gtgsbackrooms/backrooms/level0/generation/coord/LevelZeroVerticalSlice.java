package com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord;

/**
 * Tranche verticale absolue d'un layer : les Y exacts que le writer doit remplir.
 *
 * @param layerIndex index du layer, base 0
 * @param floorY Y du sol
 * @param airMinY premier Y d'air jouable
 * @param airMaxY dernier Y d'air jouable
 * @param ceilingY Y du plafond
 * @param bedrockMinY premier Y de bedrock technique au-dessus du plafond
 * @param bedrockMaxY dernier Y de bedrock technique au-dessus du plafond
 */
public record LevelZeroVerticalSlice(
        int layerIndex,
        int floorY,
        int airMinY,
        int airMaxY,
        int ceilingY,
        int bedrockMinY,
        int bedrockMaxY) {

    /**
     * Construit la slice canonique d'un layer.
     *
     * @param layerIndex index du layer, base 0
     * @return slice absolue du layer
     */
    public static LevelZeroVerticalSlice of(int layerIndex) {
        return new LevelZeroVerticalSlice(
                layerIndex,
                LevelZeroLayerStackLayout.floorY(layerIndex),
                LevelZeroLayerStackLayout.airMinY(layerIndex),
                LevelZeroLayerStackLayout.airMaxY(layerIndex),
                LevelZeroLayerStackLayout.ceilingY(layerIndex),
                LevelZeroLayerStackLayout.interLayerBedrockMinY(layerIndex),
                LevelZeroLayerStackLayout.interLayerBedrockMaxY(layerIndex));
    }
}
