package com.petassegang.addons.feature.cursed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import com.petassegang.addons.MinecraftTestBootstrap;
import com.petassegang.addons.init.ModItems;

@DisplayName("Casse-croute Maudit")
class CursedSnackItemTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("Le casse-croute est une nourriture sans apport, toujours mangeable, par piles de 16")
    void foodComponent() {
        FoodComponent food = ModItems.CURSED_SNACK.getComponents().get(DataComponentTypes.FOOD);
        assertNotNull(food, "Composant nourriture present.");
        assertEquals(0, food.nutrition(), "Aucun apport de faim.");
        assertEquals(0.0f, food.saturation(), 1e-6f, "Aucune saturation.");
        assertTrue(food.canAlwaysEat(), "Mangeable le ventre plein.");
        assertEquals(16, ModItems.CURSED_SNACK.getMaxCount(), "Piles de seize.");
    }

    @Test
    @DisplayName("La penalite retire deux points de faim sans passer sous zero")
    void hungerPenalty() {
        assertEquals(2, CursedSnackItem.HUNGER_PENALTY, "Deux points retires.");
        assertEquals(18, CursedSnackItem.penalizedFoodLevel(20), "20 devient 18.");
        assertEquals(0, CursedSnackItem.penalizedFoodLevel(1), "1 devient 0.");
        assertEquals(0, CursedSnackItem.penalizedFoodLevel(0), "0 reste 0.");
    }

    @Test
    @DisplayName("Le tooltip affiche l'avertissement traduit")
    void tooltip() {
        List<Text> tooltip = new ArrayList<>();
        ModItems.CURSED_SNACK.appendTooltip(new ItemStack(ModItems.CURSED_SNACK), Item.TooltipContext.DEFAULT, tooltip, TooltipType.BASIC);
        assertEquals(List.of(CursedSnackItem.TOOLTIP_WARNING), tooltip, "Une ligne d'avertissement.");
    }
}
