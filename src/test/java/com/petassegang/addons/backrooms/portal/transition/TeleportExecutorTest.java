package com.petassegang.addons.backrooms.portal.transition;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.MinecraftTestBootstrap;
import com.petassegang.addons.backrooms.BackroomsConstants;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.PortalFacing;

@DisplayName("Detection de traversee d'un portail et cooldown des joueurs")
class TeleportExecutorTest {

    /** Boite d'un joueur debout. */
    private static final double HALF_WIDTH = 0.3;
    private static final double HEIGHT = 1.8;

    private static BackroomsPortalEntity portal;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
        // Portail 1x2 oriente nord, centre en (0, 64, 0) : le cadre va de y=63 a y=65 et de x=-0.5 a x=0.5.
        portal = BackroomsPortalEntity.builder(null)
                .position(new Vec3d(0.0, 64.0, 0.0))
                .facingSource(PortalFacing.NORTH)
                .facingDestination(PortalFacing.SOUTH)
                .destinationDim(BackroomsConstants.LEVEL_ZERO_WORLD)
                .destinationPos(new Vec3d(1.5, 2.0, 1.5))
                .size(1.0f, 2.0f)
                .build();
    }

    private static boolean crossed(double lastX, double lastY, double lastZ, double currentX, double currentY, double currentZ) {
        return TeleportExecutor.crossedPortalPlane(portal, lastX, lastY, lastZ, currentX, currentY, currentZ, HALF_WIDTH, HEIGHT);
    }

    @Test
    @DisplayName("Marcher du cote d'entree vers l'arriere, le corps dans le cadre, compte comme une traversee")
    void walkingThroughCounts() {
        assertTrue(crossed(0.0, 63.0, -0.5, 0.0, 63.0, 0.3), "Du nord vers le sud, les pieds au bas du cadre.");
        assertTrue(crossed(0.45, 63.0, -0.2, 0.45, 63.0, 0.1), "Le centre au bord du cadre, l'epaule au-dela.");
        assertTrue(crossed(0.0, 61.5, -0.2, 0.0, 61.5, 0.1), "Les pieds sous le cadre, la tete dedans.");
    }

    @Test
    @DisplayName("Revenir par l'arriere, passer a cote ou s'arreter avant le plan ne compte pas")
    void otherMovesDoNotCount() {
        assertFalse(crossed(0.0, 63.0, 0.3, 0.0, 63.0, -0.5), "Du sud vers le nord : mauvais sens.");
        assertFalse(crossed(0.85, 63.0, -0.2, 0.85, 63.0, 0.1), "Le corps passe juste a cote du cadre : il ressortirait dans le mur d'en face.");
        assertFalse(crossed(0.75, 63.0, -0.2, 0.75, 63.0, 0.1), "Le centre a cote du cadre, l'epaule dedans : on contourne le portail.");
        assertFalse(crossed(-3.0, 63.0, -0.5, -3.0, 63.0, 0.3), "Trois blocs a l'ouest du cadre.");
        assertFalse(crossed(0.0, 63.0, -2.0, 0.0, 63.0, -0.1), "Le plan n'est pas atteint.");
        assertFalse(crossed(0.0, 63.0, -0.5, 0.0, 63.0, -0.5), "Immobile du cote d'entree.");
        assertFalse(crossed(0.0, 70.0, -0.5, 0.0, 70.0, 0.3), "Bien au-dessus du cadre.");
        assertFalse(crossed(0.0, 58.0, -0.5, 0.0, 58.0, 0.3), "Sous le cadre, la tete comprise.");
    }

    @Test
    @DisplayName("Une entity traverse d'apres son deplacement du tick, mais pas en apparaissant ni en sautant de position")
    void entityCrossingIgnoresSpawnsAndJumps() {
        ItemEntity item = new ItemEntity(EntityType.ITEM, null);
        item.setPosition(0.0, 63.0, 0.3);
        item.prevX = 0.0;
        item.prevY = 63.0;
        item.prevZ = -0.2;
        assertTrue(TeleportExecutor.crossedThisTick(portal, item), "Un pas du nord vers le sud, dans le cadre.");
        // Avant son premier tick, sa position precedente est l'origine du monde : loin au nord, le trajet couperait le plan.
        item.prevX = 0.0;
        item.prevY = 63.0;
        item.prevZ = -40.0;
        assertFalse(TeleportExecutor.crossedThisTick(portal, item), "Un saut de quarante blocs ne traverse rien.");
    }

    @Test
    @DisplayName("Un joueur qui vient de traverser attend la duree du cooldown avant de repasser")
    void cooldownWindow() {
        UUID player = UUID.randomUUID();
        int cooldown = 20;
        assertFalse(TeleportExecutor.isOnCooldown(player, 100L, cooldown), "Jamais passe : pas de cooldown.");
        TeleportExecutor.markCrossed(player, 100L);
        assertTrue(TeleportExecutor.isOnCooldown(player, 100L, cooldown), "Au tick de la traversee : cooldown.");
        assertTrue(TeleportExecutor.isOnCooldown(player, 100L + cooldown - 1, cooldown), "Juste avant la fin : cooldown.");
        assertFalse(TeleportExecutor.isOnCooldown(player, 100L + cooldown, cooldown), "A la fin du cooldown : libre.");
    }
}
