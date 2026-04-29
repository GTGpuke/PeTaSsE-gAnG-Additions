package com.petassegang.addons;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.feature.cursed.item.cursed_snack.CursedSnackItem;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Teste les proprietes de CursedSnackItem.
 */
@DisplayName("Proprietes de CursedSnackItem")
class CursedSnackTest {

    @Test
    @DisplayName("CursedSnackItem expose un constructeur avec Item.Settings")
    void testConstructorExists() throws NoSuchMethodException {
        assertNotNull(CursedSnackItem.class.getDeclaredConstructor(net.minecraft.item.Item.Settings.class),
                "CursedSnackItem doit exposer un constructeur prenant Item.Settings.");
    }

    @Test
    @DisplayName("CursedSnackItem surcharge la methode finishUsing()")
    void testFinishUsingOverridden() throws NoSuchMethodException {
        var method = CursedSnackItem.class.getDeclaredMethod("finishUsing",
                net.minecraft.item.ItemStack.class,
                net.minecraft.world.World.class,
                net.minecraft.entity.LivingEntity.class);
        assertNotNull(method,
                "CursedSnackItem doit surcharger finishUsing() pour modifier la faim.");
    }
}
