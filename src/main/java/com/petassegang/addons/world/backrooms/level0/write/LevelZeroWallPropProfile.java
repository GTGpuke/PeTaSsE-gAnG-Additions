package com.petassegang.addons.world.backrooms.level0.write;

/**
 * Profil semantique des petits details muraux d'une colonne.
 */
public record LevelZeroWallPropProfile(
        int baseboardFaceMask,
        LevelZeroBaseboardStyle baseboardStyle,
        LevelZeroWallFixture fixture,
        int fixtureFaceMask,
        int fixtureY) {

    /**
     * Profil vide sans detail mural.
     *
     * @return profil vide
     */
    public static LevelZeroWallPropProfile none() {
        return new LevelZeroWallPropProfile(0, LevelZeroBaseboardStyle.NONE, LevelZeroWallFixture.NONE, 0, 0);
    }

    /**
     * Retourne {@code true} si une plinthe est presente sur au moins une face.
     *
     * @return {@code true} si une plinthe murale est attendue
     */
    public boolean hasBaseboard() {
        return baseboardFaceMask != 0 && baseboardStyle != LevelZeroBaseboardStyle.NONE;
    }

    /**
     * Retourne la variante connectee de plinthe.
     *
     * @return variante normalisee pour les futurs assets fins
     */
    public LevelZeroConnectedDetailVariant baseboardVariant() {
        return LevelZeroConnectedDetailVariant.fromConnectionMask(baseboardFaceMask);
    }

    /**
     * Retourne {@code true} si un petit element mural est present.
     *
     * @return {@code true} si un interrupteur ou une prise est present
     */
    public boolean hasFixture() {
        return fixture != LevelZeroWallFixture.NONE && fixtureFaceMask != 0;
    }
}
