package com.petassegang.addons.backrooms.level.level0.generation.write;

import com.petassegang.addons.backrooms.level.level0.generation.coord.LevelZeroVerticalSlice;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroLayoutSampler;
import com.petassegang.addons.backrooms.level.level0.generation.noise.StageRandom;

/**
 * Resolveur semantique des petits details muraux du Level 0.
 *
 * <p>Cette classe ne pose aucun bloc. Elle decide seulement quel profil mural
 * une colonne doit porter : plinthe, prise, interrupteur, face choisie et
 * hauteur d'ancrage. Le rendu concret est ensuite pris en charge par le writer
 * ou par le modele mural cote client.
 */
public final class LevelZeroWallPropResolver {

    /**
     * Resolve les petits details muraux d'une colonne.
     *
     * @param walkable {@code true} si la colonne est traversable
     * @param exposedWallpaper {@code true} si le mur est expose
     * @param faceMask masque des faces exposees
     * @param worldX coordonnee monde X
     * @param worldZ coordonnee monde Z
     * @return profil des details muraux
     */
    public LevelZeroWallPropProfile resolve(boolean walkable,
                                            boolean exposedWallpaper,
                                            int faceMask,
                                            LevelZeroVerticalSlice verticalSlice,
                                            int worldX,
                                            int worldZ) {
        if (walkable || !exposedWallpaper || faceMask == 0) {
            return LevelZeroWallPropProfile.none();
        }

        return resolveAtWorld(worldX, worldZ, faceMask, verticalSlice);
    }

    /**
     * Resolve le profil mural complet a partir des coordonnees monde et des faces exposees.
     *
     * <p>Utilise cote client pour les details muraux fins rendus directement sur le wallpaper.
     *
     * @param worldX coordonnee monde X
     * @param worldZ coordonnee monde Z
     * @param exposedFaceMask masque des faces visibles
     * @return profil mural coherent pour cette colonne
     */
    public static LevelZeroWallPropProfile resolveAtWorld(int worldX,
                                                          int worldZ,
                                                          int exposedFaceMask,
                                                          LevelZeroVerticalSlice verticalSlice) {
        if (exposedFaceMask == 0) {
            return LevelZeroWallPropProfile.none();
        }

        int baseboardFaceMask = exposedFaceMask;
        LevelZeroBaseboardStyle baseboardStyle = baseboardFaceMask == 0
                ? LevelZeroBaseboardStyle.NONE
                : LevelZeroBaseboardStyle.WHITE;
        long hash = StageRandom.mixLegacy(0L, StageRandom.Stage.WALL_PROPS, worldX, worldZ);
        int fixtureFace = pickVisibleFace(exposedFaceMask, hash);
        int roll = Math.floorMod(hash, 1024);
        if (roll < 4) {
            return new LevelZeroWallPropProfile(baseboardFaceMask,
                    baseboardStyle,
                    LevelZeroWallFixture.SWITCH,
                    fixtureFace,
                    verticalSlice.floorY() + 2);
        }
        if (roll < 10) {
            return new LevelZeroWallPropProfile(baseboardFaceMask,
                    baseboardStyle,
                    LevelZeroWallFixture.OUTLET,
                    fixtureFace,
                    verticalSlice.floorY() + 1);
        }
        return new LevelZeroWallPropProfile(baseboardFaceMask, baseboardStyle, LevelZeroWallFixture.NONE, 0, 0);
    }

    /**
     * Resolve le profil de plinthe a partir des coordonnees monde et des faces exposees.
     *
     * <p>Utilise cote client pour le rendu mural fin, sans reposer sur le writer.
     *
     * @param worldX coordonnee monde X
     * @param worldZ coordonnee monde Z
     * @param exposedFaceMask masque des faces visibles
     * @return profil de plinthe coherent pour cette colonne
     */
    public static LevelZeroWallPropProfile resolveBaseboardAtWorld(int worldX,
                                                                   int worldZ,
                                                                   int exposedFaceMask,
                                                                   LevelZeroVerticalSlice verticalSlice) {
        if (exposedFaceMask == 0) {
            return LevelZeroWallPropProfile.none();
        }
        LevelZeroWallPropProfile profile = resolveAtWorld(worldX, worldZ, exposedFaceMask, verticalSlice);
        return new LevelZeroWallPropProfile(
                profile.baseboardFaceMask(),
                profile.baseboardStyle(),
                LevelZeroWallFixture.NONE,
                0,
                0);
    }

    private static int pickVisibleFace(int faceMask, long hash) {
        int[] faces = new int[4];
        int count = 0;
        if ((faceMask & LevelZeroLayoutSampler.NORTH_MASK) != 0) {
            faces[count++] = LevelZeroLayoutSampler.NORTH_MASK;
        }
        if ((faceMask & LevelZeroLayoutSampler.EAST_MASK) != 0) {
            faces[count++] = LevelZeroLayoutSampler.EAST_MASK;
        }
        if ((faceMask & LevelZeroLayoutSampler.SOUTH_MASK) != 0) {
            faces[count++] = LevelZeroLayoutSampler.SOUTH_MASK;
        }
        if ((faceMask & LevelZeroLayoutSampler.WEST_MASK) != 0) {
            faces[count++] = LevelZeroLayoutSampler.WEST_MASK;
        }
        if (count == 0) {
            return 0;
        }
        return faces[Math.floorMod(hash, count)];
    }
}
