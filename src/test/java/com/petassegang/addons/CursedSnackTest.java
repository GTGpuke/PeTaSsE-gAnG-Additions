package com.petassegang.addons;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
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
                new Item.Properties()
                        .setId(ResourceKey.create(Registries.ITEM,
                                Identifier.fromNamespaceAndPath("test", "cursed_snack")))
                        .stacksTo(16)
                        .food(new FoodProperties.Builder()
                                .nutrition(0)
                                .saturationModifier(0)
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
        assertEquals(16, item.getDefaultMaxStackSize(),
                "Le Casse-croûte Maudit doit avoir un stack size de 16.");
    }

    @Test
    @DisplayName("CursedSnackItem surcharge la méthode finishUsingItem()")
    void testFinishUsingItemOverridden() throws NoSuchMethodException {
        var method = CursedSnackItem.class.getDeclaredMethod("finishUsingItem",
                net.minecraft.world.item.ItemStack.class,
                net.minecraft.world.level.Level.class,
                net.minecraft.world.entity.LivingEntity.class);
        assertNotNull(method,
                "CursedSnackItem doit surcharger finishUsingItem() pour modifier la faim.");
    }
}
