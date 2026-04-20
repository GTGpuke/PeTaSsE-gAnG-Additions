package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.world.backrooms.level0.noise.StageRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Verifie la stabilite des derives de seed du Level 0.
 */
@DisplayName("StageRandom du Level 0")
class BackroomsLevelZeroStageRandomTest {

    @Test
    @DisplayName("Le mix legacy reste deterministe pour une meme entree")
    void testLegacyMixIsDeterministic() {
        long first = StageRandom.mixLegacy(12345L, StageRandom.Stage.LIGHTS, 17, -9);
        long second = StageRandom.mixLegacy(12345L, StageRandom.Stage.LIGHTS, 17, -9);

        assertEquals(first, second,
                "Le mix legacy doit renvoyer exactement le meme hash pour les memes entrees.");
    }

    @Test
    @DisplayName("Deux stages differents produisent des derives distincts")
    void testDifferentStagesProduceDifferentHashes() {
        long lights = StageRandom.mixLegacy(12345L, StageRandom.Stage.LIGHTS, 17, -9);
        long biomeLights = StageRandom.mixLegacy(12345L, StageRandom.Stage.BIOME_LIGHTING, 17, -9);
        long roomLights = StageRandom.mixLegacy(12345L, StageRandom.Stage.LARGE_ROOM_LIGHTING, 17, -9);
        long rooms = StageRandom.mixLegacy(12345L, StageRandom.Stage.LARGE_ROOMS, 17, -9);
        long geometry = StageRandom.mixLegacy(12345L, StageRandom.Stage.NOISE_GEOMETRY, 17, -9);

        assertNotEquals(lights, biomeLights,
                "Le stage BIOME_LIGHTING doit rester distinct du derive historique des neons.");
        assertNotEquals(lights, roomLights,
                "Le stage LARGE_ROOM_LIGHTING doit rester distinct du derive historique des neons.");
        assertNotEquals(lights, rooms,
                "Deux stages differents ne doivent pas reutiliser le meme derive.");
        assertNotEquals(lights, geometry,
                "Le stage NOISE_GEOMETRY doit garder un derive distinct.");
    }

    @Test
    @DisplayName("Le random legacy reste seedé de maniere deterministe")
    void testLegacyRandomSequenceIsDeterministic() {
        java.util.Random first = StageRandom.createLegacyRandom(998877L, StageRandom.Stage.SECTOR_MAZE, 4, 7);
        java.util.Random second = StageRandom.createLegacyRandom(998877L, StageRandom.Stage.SECTOR_MAZE, 4, 7);

        assertEquals(first.nextInt(), second.nextInt(),
                "Le premier tirage doit etre identique pour un random legacy equivalent.");
        assertEquals(first.nextInt(), second.nextInt(),
                "Le second tirage doit rester identique pour garantir le determinisme.");
    }

    @Test
    @DisplayName("Le stage de biome de surface reproduit le sel historique")
    void testSurfaceBiomeStageMatchesHistoricalSalt() {
        long expected = legacyMixWithSalt(0L, 12, -8, 0x535552464143454CL);
        long actual = StageRandom.mixLegacy(0L, StageRandom.Stage.SURFACE_BIOME, 12, -8);

        assertEquals(expected, actual,
                "Le stage SURFACE_BIOME doit rester compatible avec le sel historique des biomes cosmetiques.");
    }

    private static long legacyMixWithSalt(long seed, long x, long z, long salt) {
        long mixed = seed ^ salt;
        mixed ^= x * 0x9E3779B97F4A7C15L;
        mixed = Long.rotateLeft(mixed, 17);
        mixed ^= z * 0xC2B2AE3D27D4EB4FL;
        mixed = Long.rotateLeft(mixed, 31);
        mixed *= 0x165667B19E3779F9L;
        return mixed;
    }
}
