package com.petassegang.addons;

import net.minecraft.util.TypedActionResult;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.feature.gang.item.gang_badge.GangBadgeItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Teste les proprietes de GangBadgeItem.
 */
@DisplayName("Proprietes de GangBadgeItem")
class ItemTest {

    @Test
    @DisplayName("GangBadgeItem expose un constructeur avec Item.Settings")
    void testConstructorExists() throws NoSuchMethodException {
        assertNotNull(GangBadgeItem.class.getDeclaredConstructor(net.minecraft.item.Item.Settings.class),
                "GangBadgeItem doit exposer un constructeur prenant Item.Settings.");
    }

    @Test
    @DisplayName("GangBadgeItem override hasGlint")
    void testHasGlintMethodOverridden() throws NoSuchMethodException {
        var method = GangBadgeItem.class.getDeclaredMethod("hasGlint", net.minecraft.item.ItemStack.class);
        assertNotNull(method, "GangBadgeItem doit surcharger hasGlint.");
        assertEquals(boolean.class, method.getReturnType(),
                "hasGlint doit retourner un booleen.");
    }

    @Test
    @DisplayName("GangBadgeItem override la methode use")
    void testUseMethodOverridden() throws NoSuchMethodException {
        var method = GangBadgeItem.class.getDeclaredMethod("use",
                net.minecraft.world.World.class,
                net.minecraft.entity.player.PlayerEntity.class,
                net.minecraft.util.Hand.class);
        assertNotNull(method,
                "GangBadgeItem doit surcharger use pour gerer le clic droit.");
        assertEquals(TypedActionResult.class, method.getReturnType(),
                "use doit retourner TypedActionResult.");
    }
}
