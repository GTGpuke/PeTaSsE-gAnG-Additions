package com.gtgpuke.gtgsbackrooms.backrooms.level0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.IntBinaryOperator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroCoords;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.coord.LevelZeroLayerStackLayout;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.noise.LevelZeroSeedResolver;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalExit;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalSettings;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalTargets.Target;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;

@DisplayName("Arrivee d'un portail dans le Level 0")
class LevelZeroArrivalTest {

    private static final long SEED = LevelZeroSeedResolver.resolveLayerSeed(998877L, 0);
    private static final double EPSILON = 1.0e-9;
    /** Un couloir est-ouest d'un bloc de large, le long de z = 1. */
    private static final IntBinaryOperator EAST_WEST_CORRIDOR = (x, z) -> z == 1 ? 1 : 0;
    @Test
    @DisplayName("La zone sure de l'origine est ouverte : le cadre s'y pose, dans l'orientation preferee, au-dessus du sol du layer 0")
    void originCellTakesThePreferredFacing() {
        Target target = LevelZeroArrival.find(SEED, 1, 1, PortalFacing.NORTH, PortalSettings.DEFAULT);
        assertNotNull(target, "Une destination est trouvee.");
        assertEquals(BackroomsConstants.LEVEL_ZERO_WORLD, target.dimension(), "Dimension Level 0.");
        assertEquals(PortalFacing.NORTH, target.facing(), "L'orientation preferee tient.");
        assertEquals(LevelZeroCoords.cellCenterWorld(0) + 0.5, target.position().x, EPSILON, "Centre de la cellule d'origine en X.");
        assertEquals(LevelZeroLayerStackLayout.airMinY(0) + 1.0, target.position().y, EPSILON, "Centre a mi-hauteur d'un cadre de deux blocs.");
    }

    @Test
    @DisplayName("Un cadre large se pose la ou toutes ses colonnes, et le bloc devant et derriere chacune, sont libres")
    void wideFrameFitsInTheGeneratedMaze() {
        PortalSettings wide = new PortalSettings(3, 3, true);
        LevelZeroArrival.LayoutCache maze = new LevelZeroArrival.LayoutCache(SEED);
        Target target = LevelZeroArrival.search(maze, 200, -300, PortalFacing.SOUTH, wide);
        assertNotNull(target, "Une destination est trouvee.");
        int x = (int) Math.floor(target.position().x);
        int z = (int) Math.floor(target.position().z);
        assertTrue(LevelZeroArrival.fits(maze, x, z, target.facing(), wide.width()), "Le cadre de trois blocs tient a l'arrivee.");
    }

    @Test
    @DisplayName("Dans un couloir, le cadre se tourne pour le barrer : ses deux faces donnent sur le couloir")
    void frameTurnsAcrossTheCorridor() {
        assertFalse(LevelZeroArrival.fits(EAST_WEST_CORRIDOR, 1, 1, PortalFacing.NORTH, 1), "Face au nord, le cadre donnerait sur les murs.");
        assertTrue(LevelZeroArrival.fits(EAST_WEST_CORRIDOR, 1, 1, PortalFacing.EAST, 1), "Face a l'est, il barre le couloir.");
        assertFalse(LevelZeroArrival.fits((x, z) -> z <= 1 ? 1 : 0, 1, 1, PortalFacing.NORTH, 1), "Un mur juste derriere : le cadre ne tient pas.");
        assertEquals(PortalFacing.EAST, LevelZeroArrival.fittingFacing(EAST_WEST_CORRIDOR, 1, 1, PortalFacing.NORTH, 1),
                "La preferee ne tient pas : la premiere orientation qui tient est prise.");
        assertEquals(PortalFacing.WEST, LevelZeroArrival.fittingFacing(EAST_WEST_CORRIDOR, 1, 1, PortalFacing.WEST, 1),
                "La preferee tient : elle est gardee.");
        Target target = LevelZeroArrival.search(EAST_WEST_CORRIDOR, 1, 1, PortalFacing.NORTH, PortalSettings.DEFAULT);
        assertNotNull(target, "Une destination est trouvee dans le couloir.");
        assertEquals(PortalFacing.EAST, target.facing(), "Le cadre barre le couloir.");
    }

    @Test
    @DisplayName("Trop large pour tout le labyrinthe : repli sur la cellule ouverte la plus proche, face a un bloc libre")
    void tooWideFallsBackToTheNearestOpenCell() {
        PortalSettings wide = new PortalSettings(3, 2, true);
        assertNull(LevelZeroArrival.fittingFacing(EAST_WEST_CORRIDOR, 1, 1, PortalFacing.NORTH, wide.width()), "Rien ne tient sur trois blocs.");
        Target target = LevelZeroArrival.search(EAST_WEST_CORRIDOR, 1, 1, PortalFacing.NORTH, wide);
        assertNotNull(target, "Une destination de repli est trouvee.");
        assertEquals(PortalFacing.EAST, target.facing(), "La sortie de repli donne sur un bloc libre.");
        assertEquals(1.5, target.position().x, EPSILON, "Au centre de la cellule ouverte la plus proche.");
    }

    @Test
    @DisplayName("Un cadre couche se pose la ou toute son empreinte est libre : au sol du layer 0, ou sous son plafond")
    void lyingFramesNeedTheirFootprintFree() {
        PortalSettings flat = new PortalSettings(1, 2, true);
        assertTrue(LevelZeroArrival.fitsFlat((x, z) -> 1, 1, 1, PortalFacing.UP, flat), "Tout ouvert : l'empreinte tient.");
        assertFalse(LevelZeroArrival.fitsFlat(EAST_WEST_CORRIDOR, 1, 1, PortalFacing.UP, flat), "Au sol, les rangees partent vers le nord, dans le mur.");
        assertFalse(LevelZeroArrival.fitsFlat(EAST_WEST_CORRIDOR, 1, 1, PortalFacing.DOWN, flat), "Au plafond aussi.");
        assertTrue(LevelZeroArrival.fitsFlat(EAST_WEST_CORRIDOR, 1, 1, PortalFacing.UP, new PortalSettings(3, 1, true)), "Une rangee le long du couloir tient.");
        Target floor = LevelZeroArrival.search((x, z) -> 1, 1, 1, PortalFacing.UP, flat);
        assertNotNull(floor, "Une destination au sol est trouvee.");
        assertEquals(PortalFacing.UP, floor.facing(), "Couchee au sol.");
        assertEquals(LevelZeroLayerStackLayout.airMinY(0) + 0.5, floor.position().y, EPSILON, "Plan au milieu de la premiere rangee d'air.");
        Target ceiling = LevelZeroArrival.search((x, z) -> 1, 1, 1, PortalFacing.DOWN, flat);
        assertNotNull(ceiling, "Une destination au plafond est trouvee.");
        assertEquals(LevelZeroLayerStackLayout.airMaxY(0) + 0.5, ceiling.position().y, EPSILON, "Plan au milieu de la derniere rangee d'air, sous le plafond.");
        Target fallback = LevelZeroArrival.search(EAST_WEST_CORRIDOR, 1, 1, PortalFacing.UP, flat);
        assertNotNull(fallback, "Faute d'empreinte libre, repli sur la cellule ouverte la plus proche.");
        assertEquals(PortalFacing.UP, fallback.facing(), "Un cadre couche le reste au repli.");
        assertEquals(1.5, fallback.position().x, EPSILON, "Au centre de la cellule ouverte la plus proche.");
        // Un couloir trop etroit pres de l'entree, une place toute ouverte au-dela de x = 40 : couche, le cadre va jusqu'a elle.
        Target far = LevelZeroArrival.search((x, z) -> z == 1 || x >= 40 ? 1 : 0, 1, 1, PortalFacing.UP, flat);
        assertNotNull(far, "Une destination est trouvee.");
        assertTrue(far.position().x >= 40.0 && far.facing() == PortalFacing.UP, "Couche, il se pose la ou toute son empreinte est libre : " + far.position() + ".");
    }

    @Test
    @DisplayName("Un sens de sortie impose ne tourne pas : le cadre se pose plus loin, la ou il tient tel quel, sinon de repli dans ce sens")
    void imposedExitNeverTurns() {
        PortalSettings north = new PortalSettings(1, 2, true, true, false, false, PortalExit.NORTH, 0);
        // Un couloir est-ouest, puis une place toute ouverte au-dela de x = 40 : face au nord, le cadre ne tient que sur la place.
        Target far = LevelZeroArrival.search((x, z) -> z == 1 || x >= 40 ? 1 : 0, 1, 1, PortalFacing.NORTH, north);
        assertNotNull(far, "Une destination est trouvee.");
        assertEquals(PortalFacing.NORTH, far.facing(), "Le cadre regarde le nord, comme impose.");
        assertTrue(far.position().x >= 40.0, "Il se pose sur la place, ou il tient tel quel : " + far.position() + ".");
        Target fallback = LevelZeroArrival.search(EAST_WEST_CORRIDOR, 1, 1, PortalFacing.NORTH, north);
        assertNotNull(fallback, "Faute de place, une destination de repli.");
        assertEquals(PortalFacing.NORTH, fallback.facing(), "De repli, il regarde encore le nord.");
        assertEquals(PortalFacing.EAST, LevelZeroArrival.search(EAST_WEST_CORRIDOR, 1, 1, PortalFacing.NORTH, PortalSettings.DEFAULT).facing(),
                "Tout droit, le cadre se tourne pour barrer le couloir.");
    }

    @Test
    @DisplayName("Une largeur paire decale le centre d'un demi-bloc le long de la largeur")
    void evenWidthShiftsTheCenter() {
        Target target = LevelZeroArrival.search((x, z) -> 1, 1, 1, PortalFacing.SOUTH, new PortalSettings(2, 4, false));
        assertNotNull(target, "Une destination est trouvee.");
        assertEquals(1.5 + PortalFacing.SOUTH.axisW.x * 0.5, target.position().x, EPSILON, "Decale d'un demi-bloc.");
        assertEquals(LevelZeroLayerStackLayout.airMinY(0) + 2.0, target.position().y, EPSILON, "Centre a mi-hauteur d'un cadre de quatre blocs.");
    }

    @Test
    @DisplayName("Sans cellule ouverte dans le rayon de recherche, il n'y a pas de destination")
    void noOpenCellNoTarget() {
        assertNull(LevelZeroArrival.search((x, z) -> 0, 37, -112, PortalFacing.NORTH, PortalSettings.DEFAULT),
                "Pas de destination sans cellule ouverte.");
    }

    @Test
    @DisplayName("La sortie de repli retombe sur la premiere orientation ouverte, sinon sur celle demandee")
    void exitFacingFallbacks() {
        assertEquals(PortalFacing.NORTH, LevelZeroArrival.exitFacing((x, z) -> 1, 0, 0, PortalFacing.NORTH), "Tout est ouvert : la demandee.");
        assertEquals(PortalFacing.EAST, LevelZeroArrival.exitFacing((x, z) -> x == 1 && z == 0 ? 1 : 0, 0, 0, PortalFacing.NORTH),
                "Seul l'est est ouvert : la sortie est a l'est.");
        assertEquals(PortalFacing.NORTH, LevelZeroArrival.exitFacing((x, z) -> 0, 0, 0, PortalFacing.NORTH),
                "Rien d'ouvert : la sortie demandee est gardee.");
    }
}
