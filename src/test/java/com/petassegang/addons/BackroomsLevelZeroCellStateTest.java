package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.level.level0.biome.LevelZeroSurfaceBiome;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellConnections;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellMicroPattern;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellState;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellTag;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroCellTopology;
import com.petassegang.addons.backrooms.level.level0.generation.layout.LevelZeroGeometryMask;
import com.petassegang.addons.backrooms.level.level0.generation.layout.sector.LevelZeroSectorRoomKind;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifie les helpers semantiques locaux des cellules du Level 0.
 */
@DisplayName("Etat de cellule du Level 0")
class BackroomsLevelZeroCellStateTest {

    @Test
    @DisplayName("La traversabilite locale tient compte du micro-pattern")
    void testLocalWalkabilityUsesMicroPattern() {
        LevelZeroCellState openState = new LevelZeroCellState(
                LevelZeroCellTag.CORRIDOR,
                LevelZeroCellTopology.CORRIDOR,
                LevelZeroCellConnections.NORTH,
                LevelZeroGeometryMask.none(),
                LevelZeroCellMicroPattern.bit(1, 1),
                1,
                1,
                LevelZeroSurfaceBiome.BASE,
                LevelZeroSectorRoomKind.NONE,
                false,
                false);
        LevelZeroCellState blockedState = new LevelZeroCellState(
                LevelZeroCellTag.CORRIDOR,
                LevelZeroCellTopology.CORRIDOR,
                LevelZeroCellConnections.NORTH,
                LevelZeroGeometryMask.none(),
                LevelZeroCellMicroPattern.bit(1, 1),
                0,
                1,
                LevelZeroSurfaceBiome.BASE,
                LevelZeroSectorRoomKind.NONE,
                false,
                false);

        assertEquals(true, openState.isLocallyWalkable(),
                "Le bloc central du motif de test doit rester traversable.");
        assertEquals(false, blockedState.isLocallyWalkable(),
                "Un bloc ferme par le micro-pattern ne doit plus etre traversable localement.");
        assertEquals(true, openState.hasConnection(LevelZeroCellConnections.NORTH),
                "La connexion stockee doit rester consultable depuis l'etat de cellule.");
    }
}
