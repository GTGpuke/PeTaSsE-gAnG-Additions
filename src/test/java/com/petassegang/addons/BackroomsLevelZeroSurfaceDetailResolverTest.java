package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.world.backrooms.level0.LevelZeroSurfaceBiome;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellConnections;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellMicroPattern;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellState;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTag;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroCellTopology;
import com.petassegang.addons.world.backrooms.level0.layout.LevelZeroGeometryMask;
import com.petassegang.addons.world.backrooms.level0.layout.sector.LevelZeroSectorRoomKind;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroSurfaceDetail;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroSurfaceDetailProfile;
import com.petassegang.addons.world.backrooms.level0.write.LevelZeroSurfaceDetailResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Verifie que la couche de surface details du Level 0 est bien gelee.
 */
@DisplayName("Surface detail resolver du Level 0")
class BackroomsLevelZeroSurfaceDetailResolverTest {

    @Test
    @DisplayName("Les details surfaciques sont desactives par defaut")
    void testSurfaceDetailsDisabledByDefault() {
        assertFalse(ModConfig.ENABLE_LEVEL_ZERO_SURFACE_DETAILS,
                "Les surface details doivent rester en pause tant qu'on se concentre sur les wall props.");
    }

    @Test
    @DisplayName("Le resolver renvoie un profil vide quand la couche est gelee")
    void testResolverReturnsEmptyProfileWhilePaused() {
        LevelZeroSurfaceDetailResolver resolver = new LevelZeroSurfaceDetailResolver();
        LevelZeroSurfaceDetailProfile profile = resolver.resolve(walkableState(), 32, 48);

        assertEquals(LevelZeroSurfaceDetailProfile.none(), profile,
                "Aucun detail surfacique ne doit remonter tant que la couche est gelee.");
    }

    @Test
    @DisplayName("Le rendu mural monde reste vide tant que la couche est gelee")
    void testWorldWallDetailReturnsNoneWhilePaused() {
        assertEquals(LevelZeroSurfaceDetail.NONE,
                LevelZeroSurfaceDetailResolver.resolveWallDetailAtWorld(91, -37),
                "Les overlays muraux ne doivent rien resoudre tant que la couche est gelee.");
    }

    private static LevelZeroCellState walkableState() {
        return new LevelZeroCellState(
                LevelZeroCellTag.CORRIDOR,
                LevelZeroCellTopology.CORRIDOR,
                LevelZeroCellConnections.NORTH | LevelZeroCellConnections.SOUTH,
                LevelZeroGeometryMask.none(),
                LevelZeroCellMicroPattern.FULL_OPEN,
                1,
                1,
                LevelZeroSurfaceBiome.BASE,
                LevelZeroSectorRoomKind.NONE,
                false,
                false);
    }
}
