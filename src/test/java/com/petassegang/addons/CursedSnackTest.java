package com.petassegang.addons;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.item.CursedSnackItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Teste les propriétés de CursedSnackItem.
 */
@DisplayName("Propriétés de CursedSnackItem")
class CursedSnackTest {

    private CursedSnackItem item;

    @BeforeEach
    void setUp() {
        item = new CursedSnackItem(
                new Item.Settings()
                        .maxCount(16)
                        .food(new FoodComponent.Builder()
                                .nutrition(0)
                                .saturationModifier(0f)
                                .alwaysEdible()
                                .build())
        );
    }

    @Test
    @DisplayName("CursedSnackItem n'est pas null après construction")
    void testConstructionNotNull() {
        assertNotNull(item,
                "Le constructeur de CursedSnackItem ne doit pas retourner null.");
    }

    @Test
    @DisplayName("La taille de pile est 16")
    void testStackSizeIsSixteen() {
        assertEquals(16, item.getMaxCount(),
                "Le Casse-croûte Maudit doit avoir un stack size de 16.");
    }

    @Test
    @DisplayName("CursedSnackItem surcharge la méthode finishUsing()")
    void testFinishUsingOverridden() throws NoSuchMethodException {
        var method = CursedSnackItem.class.getDeclaredMethod("finishUsing",
                net.minecraft.item.ItemStack.class,
                net.minecraft.world.World.class,
                net.minecraft.entity.LivingEntity.class);
        assertNotNull(method,
                "CursedSnackItem doit surcharger finishUsing() pour modifier la faim.");
    }
}
