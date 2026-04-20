package com.petassegang.addons.world.backrooms.level0.noise;

import java.util.Random;

/**
 * API centralisee de derives deterministes pour le pipeline Level 0.
 *
 * <p>Cette premiere version reste volontairement compatible avec les derives
 * historiques du layout actuel. Elle sert de point d'ancrage avant une
 * migration plus stricte vers les stages de la pipeline v6.
 *
 * <p>Autrement dit : si une nouvelle couche a besoin d'un hasard deterministe,
 * elle doit passer par ce fichier plutot que recreer son propre mix.
 */
public final class StageRandom {

    /**
     * Stages actuellement mappes sur les anciens sels du Level 0.
     */
    public enum Stage {
        LIGHTS(0x4C49474854L),
        BIOME_LIGHTING(0x42494F4D454C4954L),
        LARGE_ROOM_LIGHTING(0x524F4F4D4C4954L),
        LARGE_ROOMS(0x4C41524745524F4DL),
        LAYER_LAYOUT(0x4C415945524C4159L),
        NOISE_GEOMETRY(0x4E4F49534547454FL),
        SURFACE_DETAILS(0x44455441494C53L),
        WALL_PROPS(0x57414C4C50524F50L),
        STRUCTURES(0x5354525543545552L),
        SECTOR_CACHE(0x534543544F52L),
        SECTOR_MAZE(0x4D415A45L),
        SURFACE_BIOME(0x535552464143454CL);

        private final long salt;

        Stage(long salt) {
            this.salt = salt;
        }
    }

    private StageRandom() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Reproduit le mix historique du layout actuel.
     *
     * @param seed seed de base
     * @param stage stage logique du derive
     * @param x coordonnee ou composante X
     * @param z coordonnee ou composante Z
     * @return hash 64 bits deterministe
     */
    public static long mixLegacy(long seed, Stage stage, long x, long z) {
        long mixed = seed ^ stage.salt;
        mixed ^= x * 0x9E3779B97F4A7C15L;
        mixed = Long.rotateLeft(mixed, 17);
        mixed ^= z * 0xC2B2AE3D27D4EB4FL;
        mixed = Long.rotateLeft(mixed, 31);
        mixed *= 0x165667B19E3779F9L;
        return mixed;
    }

    /**
     * Cree un {@link Random} compatible avec les derives historiques.
     *
     * @param seed seed de base
     * @param stage stage logique du derive
     * @param x coordonnee ou composante X
     * @param z coordonnee ou composante Z
     * @return random deterministe seedé
     */
    public static Random createLegacyRandom(long seed, Stage stage, long x, long z) {
        return new Random(mixLegacy(seed, stage, x, z));
    }
}
