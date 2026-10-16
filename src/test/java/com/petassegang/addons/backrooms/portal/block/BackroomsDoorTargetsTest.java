package com.petassegang.addons.backrooms.portal.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.Direction;

import com.petassegang.addons.backrooms.BackroomsConstants;
import com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroCoords;
import com.petassegang.addons.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;
import com.petassegang.addons.backrooms.level0.generation.layout.LevelZeroLayout;
import com.petassegang.addons.backrooms.level0.generation.noise.LevelZeroSeedResolver;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;

@DisplayName("Ciblage automatique des portes Backrooms")
class BackroomsDoorTargetsTest {

    private static final long SEED = LevelZeroSeedResolver.resolveLayerSeed(998877L, 0);

    @Test
    @DisplayName("La zone sure de l'origine est ouverte : la cellule de depart est retenue")
    void originCellIsOpen() {
        long cell = BackroomsDoorTargets.nearestOpenCell(new BackroomsDoorTargets.LayoutCache(SEED), 0, 0);
        assertEquals(0, (int) (cell >> 32), "Cellule X d'origine.");
        assertEquals(0, (int) cell, "Cellule Z d'origine.");
    }

    @Test
    @DisplayName("Depuis un mur, la cellule ouverte la plus proche est choisie et son centre est bien libre")
    void nearestOpenCellFromWall() {
        int wallX = -1;
        int wallZ = -1;
        for (int x = 10; x < 400 && wallX < 0; x++) {
            for (int z = 10; z < 400 && wallX < 0; z++) {
                if (!LevelZeroLayout.isWalkableCell(SEED, x, z)) {
                    wallX = x;
                    wallZ = z;
                }
            }
        }
        assertTrue(wallX >= 0, "Un mur existe dans l'echantillon.");
        BackroomsDoorTargets.LayoutCache layouts = new BackroomsDoorTargets.LayoutCache(SEED);
        long cell = BackroomsDoorTargets.nearestOpenCell(layouts, wallX, wallZ);
        assertNotEquals(Long.MIN_VALUE, cell, "Une cellule ouverte est trouvee.");
        int cellX = (int) (cell >> 32);
        int cellZ = (int) cell;
        assertTrue(LevelZeroLayout.isWalkableCell(SEED, cellX, cellZ), "La cellule retenue est ouverte.");
        assertTrue(layouts.isCellCenterOpen(cellX, cellZ), "Son bloc central est libre dans le labyrinthe genere.");
        assertTrue(Math.max(Math.abs(cellX - wallX), Math.abs(cellZ - wallZ)) <= 3,
                "Le Level 0 est dense : une cellule ouverte est a moins de 3 cellules.");
    }

    @Test
    @DisplayName("La sortie preferee est gardee si elle donne sur une cellule ouverte, sinon une sortie ouverte est choisie")
    void exitFacingPrefersOpenNeighbour() {
        BackroomsDoorTargets.LayoutCache layouts = new BackroomsDoorTargets.LayoutCache(SEED);
        assertEquals(PortalFacing.WEST, BackroomsDoorTargets.exitFacing(layouts, 0, 0, PortalFacing.WEST),
                "La zone de l'origine est entierement ouverte : l'ouest est garde.");
        for (int x = 10; x < 400; x++) {
            for (int z = 10; z < 400; z++) {
                if (!LevelZeroLayout.isWalkableCell(SEED, x, z) || !LevelZeroLayout.isWalkableCell(SEED, x, z - 1)
                        || LevelZeroLayout.isWalkableCell(SEED, x, z + 1)) {
                    continue;
                }
                // Cellule ouverte avec un mur au sud et un passage au nord.
                PortalFacing exit = BackroomsDoorTargets.exitFacing(layouts, x, z, PortalFacing.SOUTH);
                assertNotEquals(PortalFacing.SOUTH, exit, "La sortie ne doit pas donner sur le mur sud.");
                assertTrue(LevelZeroLayout.isWalkableCell(SEED, x + exit.direction.getOffsetX(), z + exit.direction.getOffsetZ()),
                        "La sortie choisie donne sur une cellule ouverte.");
                return;
            }
        }
        throw new AssertionError("Aucune cellule de test trouvee.");
    }

    @Test
    @DisplayName("La destination Level 0 est au centre d'une cellule ouverte du layer 0, a hauteur de portail")
    void levelZeroTarget() {
        BackroomsDoorTargets.Target target = BackroomsDoorTargets.levelZeroTarget(SEED, 37, -112, PortalFacing.fromDirection(Direction.EAST));
        assertNotNull(target, "Une destination est trouvee.");
        assertEquals(BackroomsConstants.LEVEL_ZERO_WORLD, target.dimension(), "Dimension Level 0.");
        assertEquals(LevelZeroLayerStackLayout.airMinY(0) + BackroomsDoorTargets.PORTAL_CENTER_ABOVE_FLOOR, target.position().y, 1e-9,
                "Centre du portail a un bloc au-dessus du sol du layer 0.");
        int cellX = LevelZeroCoords.worldToCell((int) Math.floor(target.position().x));
        int cellZ = LevelZeroCoords.worldToCell((int) Math.floor(target.position().z));
        assertTrue(LevelZeroLayout.isWalkableCell(SEED, cellX, cellZ), "La cellule d'arrivee est ouverte.");
        assertEquals(LevelZeroCoords.cellCenterWorld(cellX) + 0.5, target.position().x, 1e-9, "Centre de la cellule en X.");
        assertTrue(target.facing().direction.getAxis().isHorizontal(), "La sortie est horizontale.");
    }

    @Test
    @DisplayName("Sans cellule ouverte dans le rayon de recherche, il n'y a pas de destination")
    void noOpenCellNoTarget() {
        assertEquals(Long.MIN_VALUE, BackroomsDoorTargets.nearestOpenCell((x, z) -> 0, 3, -4), "Aucune cellule ouverte.");
        assertNull(BackroomsDoorTargets.levelZeroTarget((x, z) -> 0, 37, -112, PortalFacing.NORTH), "Pas de destination sans cellule ouverte.");
    }

    @Test
    @DisplayName("La sortie retombe sur la premiere orientation ouverte, sinon sur celle demandee")
    void exitFacingFallbacks() {
        assertEquals(PortalFacing.EAST, BackroomsDoorTargets.exitFacing((x, z) -> x == 1 && z == 0 ? 1 : 0, 0, 0, PortalFacing.NORTH),
                "Seul l'est est ouvert : la sortie est a l'est.");
        assertEquals(PortalFacing.NORTH, BackroomsDoorTargets.exitFacing((x, z) -> 0, 0, 0, PortalFacing.NORTH),
                "Rien d'ouvert : la sortie demandee est gardee.");
    }

    @Test
    @DisplayName("Une sortie verticale demandee retombe sur la premiere sortie horizontale ouverte")
    void verticalPreferenceFallsBackToHorizontal() {
        assertEquals(PortalFacing.NORTH, BackroomsDoorTargets.exitFacing((x, z) -> 1, 0, 0, PortalFacing.UP),
                "Le haut n'est pas une sortie : la premiere orientation horizontale ouverte est prise.");
    }
}
