package com.gtgpuke.gtgsbackrooms.core.portal.transition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Vec3d;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;

@DisplayName("Garde d'une arrivee au sol")
class ArrivalGuardTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @AfterEach
    void forget() {
        ArrivalGuard.clear(PortalCollision.Side.SERVER);
    }

    /** Portail vers une arrivee d'orientation donnee, centree en {@code destination}. */
    private static BackroomsPortalEntity portal(PortalFacing exit, Vec3d destination) {
        return BackroomsPortalEntity.builder(null)
                .position(new Vec3d(0.0, 64.0, 0.0))
                .facingSource(PortalFacing.NORTH)
                .facingDestination(exit)
                .destinationDim(BackroomsConstants.LEVEL_ZERO_WORLD)
                .destinationPos(destination)
                .size(1.0f, 2.0f)
                .build();
    }

    private static ItemEntity at(Vec3d position) {
        ItemEntity item = new ItemEntity(EntityType.ITEM, null);
        item.setPosition(position.x, position.y, position.z);
        return item;
    }

    @Test
    @DisplayName("Une garde se pose a la sortie d'un cadre au sol, se remplace, se leve par une autre arrivee, entity par entity")
    void guardsFollowEachEntityByIdentity() {
        Vec3d floor = new Vec3d(10.5, 64.0, 10.5);
        Vec3d otherFloor = new Vec3d(20.5, 64.0, 20.5);
        BackroomsPortalEntity toFloor = portal(PortalFacing.UP, floor);
        BackroomsPortalEntity toOtherFloor = portal(PortalFacing.UP, otherFloor);
        BackroomsPortalEntity toWall = portal(PortalFacing.SOUTH, new Vec3d(30.5, 64.0, 30.5));
        ItemEntity first = at(floor);
        ItemEntity second = at(floor);
        // Meme identifiant, comme deux entities de deux mondes : Entity.equals les confond, le suivi des gardes non.
        second.setId(first.getId());
        ArrivalGuard.arrive(first, toFloor);
        ArrivalGuard.arrive(second, toFloor);
        assertEquals(floor, ArrivalGuard.guardedCenter(first), "Sortie d'un cadre au sol : gardee.");

        ArrivalGuard.arrive(second, toWall);
        ArrivalGuard.tick(PortalCollision.Side.SERVER);
        assertNull(ArrivalGuard.guardedCenter(second), "Une arrivee par un cadre debout leve la garde.");
        assertEquals(floor, ArrivalGuard.guardedCenter(first), "Celle d'une autre entity de meme identifiant reste.");

        // La premiere gardee, suivie avant la seconde, se leve sans emporter l'autre.
        ArrivalGuard.arrive(second, toFloor);
        ArrivalGuard.arrive(first, toWall);
        ArrivalGuard.arrive(first, toWall);
        ArrivalGuard.tick(PortalCollision.Side.SERVER);
        assertNull(ArrivalGuard.guardedCenter(first), "Levee, et une seconde arrivee sans garde n'y change rien.");
        assertEquals(floor, ArrivalGuard.guardedCenter(second), "L'autre reste gardee.");

        ArrivalGuard.arrive(second, toOtherFloor);
        second.setPosition(otherFloor.x, otherFloor.y, otherFloor.z);
        ArrivalGuard.tick(PortalCollision.Side.SERVER);
        assertEquals(otherFloor, ArrivalGuard.guardedCenter(second), "Une nouvelle arrivee au sol remplace la garde.");

        second.setPosition(otherFloor.x + 5.0, otherFloor.y, otherFloor.z);
        ArrivalGuard.tick(PortalCollision.Side.SERVER);
        assertNull(ArrivalGuard.guardedCenter(second), "Plus au-dessus de l'ouverture : la garde tombe au tick.");
    }
}
