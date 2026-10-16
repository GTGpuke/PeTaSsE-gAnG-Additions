package com.petassegang.addons.backrooms.level0.generation.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Derives de seed du Level 0")
class StageRandomTest {

    @Test
    @DisplayName("Le mix est deterministe et reproduit la formule historique")
    void mixIsDeterministicAndStable() {
        long expected = 12345L ^ 0x535552464143454CL;
        expected ^= 12L * 0x9E3779B97F4A7C15L;
        expected = Long.rotateLeft(expected, 17);
        expected ^= -8L * 0xC2B2AE3D27D4EB4FL;
        expected = Long.rotateLeft(expected, 31);
        expected *= 0x165667B19E3779F9L;

        assertEquals(expected, StageRandom.mix(12345L, StageRandom.Stage.SURFACE_BIOME, 12, -8),
                "Le sel et la formule du mix ne doivent jamais changer : ils fixent la generation des mondes.");
        assertEquals(StageRandom.mix(1L, StageRandom.Stage.LIGHTS, 3, 4), StageRandom.mix(1L, StageRandom.Stage.LIGHTS, 3, 4),
                "Deux appels identiques donnent le meme hash.");
    }

    @Test
    @DisplayName("Chaque etape produit un derive distinct pour une meme position")
    void stagesAreDistinct() {
        Set<Long> hashes = new HashSet<>();
        for (StageRandom.Stage stage : StageRandom.Stage.values()) {
            hashes.add(StageRandom.mix(12345L, stage, 17, -9));
        }
        assertEquals(StageRandom.Stage.values().length, hashes.size(),
                "Deux etapes ne doivent jamais partager le meme derive.");
    }

    @Test
    @DisplayName("Le hash varie avec chaque composante")
    void mixDependsOnEveryInput() {
        long base = StageRandom.mix(1L, StageRandom.Stage.LIGHTS, 2, 3);
        assertNotEquals(base, StageRandom.mix(2L, StageRandom.Stage.LIGHTS, 2, 3), "Le seed compte.");
        assertNotEquals(base, StageRandom.mix(1L, StageRandom.Stage.LIGHTS, 3, 3), "X compte.");
        assertNotEquals(base, StageRandom.mix(1L, StageRandom.Stage.LIGHTS, 2, 4), "Z compte.");
    }

    @Test
    @DisplayName("Le random cree est seede de maniere deterministe")
    void randomIsDeterministic() {
        Random first = StageRandom.createRandom(998877L, StageRandom.Stage.SECTOR_MAZE, 4, 7);
        Random second = StageRandom.createRandom(998877L, StageRandom.Stage.SECTOR_MAZE, 4, 7);
        for (int draw = 0; draw < 16; draw++) {
            assertEquals(first.nextInt(), second.nextInt(), "Tirage " + draw + " identique.");
        }
    }
}
