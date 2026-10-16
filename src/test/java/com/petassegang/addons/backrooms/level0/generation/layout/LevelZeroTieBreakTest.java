package com.petassegang.addons.backrooms.level0.generation.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Departage entre cellules voisines du Level 0")
class LevelZeroTieBreakTest {

    @Test
    @DisplayName("Le meilleur score l'emporte, quelle que soit la position")
    void higherScoreWins() {
        assertTrue(LevelZeroTieBreak.neighborWins(10L, 11L, 0, 0, -1, -1), "Voisine mieux notee a l'ouest : elle gagne.");
        assertFalse(LevelZeroTieBreak.neighborWins(11L, 10L, 0, 0, 1, 1), "Voisine moins bien notee a l'est : elle perd.");
    }

    @Test
    @DisplayName("A score egal, l'est puis le sud l'emportent")
    void tiesAreBrokenByPosition() {
        assertTrue(LevelZeroTieBreak.neighborWins(5L, 5L, 0, 0, 1, 0), "Voisine a l'est : elle gagne.");
        assertFalse(LevelZeroTieBreak.neighborWins(5L, 5L, 0, 0, -1, 0), "Voisine a l'ouest : elle perd.");
        assertTrue(LevelZeroTieBreak.neighborWins(5L, 5L, 0, 0, 0, 1), "Meme X, voisine au sud : elle gagne.");
        assertFalse(LevelZeroTieBreak.neighborWins(5L, 5L, 0, 0, 0, -1), "Meme X, voisine au nord : elle perd.");
        assertFalse(LevelZeroTieBreak.neighborWins(5L, 5L, 0, 0, 0, 0), "La cellule ne se bat pas elle-meme.");
    }
}
