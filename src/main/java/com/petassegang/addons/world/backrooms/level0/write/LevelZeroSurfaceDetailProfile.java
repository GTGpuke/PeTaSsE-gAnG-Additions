package com.petassegang.addons.world.backrooms.level0.write;

/**
 * Profil de details surfaciques connectes d'une colonne du Level 0.
 *
 * <p>Cette structure reste conservee meme pendant la pause des overlays :
 * elle porte le contrat semantique minimal qui permettra de rebrancher plus
 * tard de vraies textures connectees sans repenser toute la plomberie.
 */
public record LevelZeroSurfaceDetailProfile(
        LevelZeroSurfaceDetail floorDetail,
        int floorConnectionMask,
        LevelZeroSurfaceDetail wallDetail,
        int wallConnectionMask,
        LevelZeroSurfaceDetail ceilingDetail,
        int ceilingConnectionMask) {

    /**
     * Retourne la variante connectee du detail de sol.
     *
     * @return variante connectee normalisee
     */
    public LevelZeroConnectedDetailVariant floorVariant() {
        return variantOf(floorDetail, floorConnectionMask);
    }

    /**
     * Retourne la variante connectee du detail mural.
     *
     * @return variante connectee normalisee
     */
    public LevelZeroConnectedDetailVariant wallVariant() {
        return variantOf(wallDetail, wallConnectionMask);
    }

    /**
     * Retourne la variante connectee du detail de plafond.
     *
     * @return variante connectee normalisee
     */
    public LevelZeroConnectedDetailVariant ceilingVariant() {
        return variantOf(ceilingDetail, ceilingConnectionMask);
    }

    /**
     * Profil vide sans aucun detail.
     *
     * @return profil sans details
     */
    public static LevelZeroSurfaceDetailProfile none() {
        return new LevelZeroSurfaceDetailProfile(
                LevelZeroSurfaceDetail.NONE,
                0,
                LevelZeroSurfaceDetail.NONE,
                0,
                LevelZeroSurfaceDetail.NONE,
                0);
    }

    private static LevelZeroConnectedDetailVariant variantOf(LevelZeroSurfaceDetail detail, int connectionMask) {
        if (detail == LevelZeroSurfaceDetail.NONE) {
            return LevelZeroConnectedDetailVariant.SINGLE;
        }
        return LevelZeroConnectedDetailVariant.fromConnectionMask(connectionMask);
    }
}
