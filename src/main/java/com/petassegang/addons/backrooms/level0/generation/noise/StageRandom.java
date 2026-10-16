package com.petassegang.addons.backrooms.level0.generation.noise;

import java.util.Random;

/**
 * Derives deterministes de seed pour la generation du Level 0.
 *
 * <p>Chaque etape de generation possede son propre sel afin que deux couches
 * ne reutilisent jamais la meme sequence. Les sels sont figes : les changer
 * modifie la generation de tous les mondes existants.
 */
public final class StageRandom {

    /** Etapes de generation et leurs sels historiques. */
    public enum Stage {
        LIGHTS(0x4C49474854L),
        BIOME_LIGHTING(0x42494F4D454C4954L),
        LARGE_ROOM_LIGHTING(0x524F4F4D4C4954L),
        LARGE_ROOMS(0x4C41524745524F4DL),
        LAYER_LAYOUT(0x4C415945524C4159L),
        NOISE_GEOMETRY(0x4E4F49534547454FL),
        WALL_PROPS(0x57414C4C50524F50L),
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
     * Hash 64 bits deterministe d'une position pour une etape donnee.
     *
     * @param seed seed de base
     * @param stage etape de generation
     * @param x composante X (ou premiere composante libre)
     * @param z composante Z (ou seconde composante libre)
     * @return hash deterministe
     */
    public static long mix(long seed, Stage stage, long x, long z) {
        long mixed = seed ^ stage.salt;
        mixed ^= x * 0x9E3779B97F4A7C15L;
        mixed = Long.rotateLeft(mixed, 17);
        mixed ^= z * 0xC2B2AE3D27D4EB4FL;
        mixed = Long.rotateLeft(mixed, 31);
        mixed *= 0x165667B19E3779F9L;
        return mixed;
    }

    /**
     * Cree un {@link Random} seede par {@link #mix}.
     *
     * @param seed seed de base
     * @param stage etape de generation
     * @param x composante X
     * @param z composante Z
     * @return random deterministe
     */
    public static Random createRandom(long seed, Stage stage, long x, long z) {
        return new Random(mix(seed, stage, x, z));
    }
}
