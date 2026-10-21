package com.gtgpuke.gtgsbackrooms.core.portal.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.random.Random;

import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;

@DisplayName("Ciblage automatique des blocs portail : tirage au sort et surface")
class PortalTargetsTest {

    @Test
    @DisplayName("L'ecart tire au sort reste dans le rayon, et il est nul sans rayon")
    void scatterStaysWithinRadius() {
        Random random = Random.create(42L);
        assertEquals(0, PortalTargets.scatter(random, 0), "Sans rayon, aucun ecart.");
        boolean moved = false;
        for (int i = 0; i < 200; i++) {
            int offset = PortalTargets.scatter(random, 8);
            assertTrue(Math.abs(offset) <= 8, "L'ecart reste dans le rayon : " + offset + ".");
            moved |= offset != 0;
        }
        assertTrue(moved, "Avec un rayon, l'arrivee bouge.");
    }

    @Test
    @DisplayName("A la surface, un cadre au plafond est releve de deux blocs ; les autres se posent au sol")
    void ceilingLiftAtTheSurface() {
        assertEquals(PortalArea.CLEARANCE_BEHIND, PortalTargets.ceilingLift(PortalFacing.DOWN), "Un cadre au plafond est releve de deux blocs.");
        assertEquals(0, PortalTargets.ceilingLift(PortalFacing.UP), "Les autres cadres se posent au sol.");
    }
}
