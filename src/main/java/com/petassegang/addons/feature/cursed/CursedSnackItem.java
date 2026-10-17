package com.petassegang.addons.feature.cursed;

import java.util.List;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * Casse-croute Maudit : consommable qui retire {@value #HUNGER_PENALTY} points de faim.
 */
public class CursedSnackItem extends Item {

    /** Points de faim retires a la consommation. */
    public static final int HUNGER_PENALTY = 2;

    static final MutableText TOOLTIP_WARNING =
            Text.translatable("tooltip.petasse_gang_additions.cursed_snack.warning")
                    .formatted(Formatting.DARK_RED, Formatting.ITALIC);

    public CursedSnackItem(Settings settings) {
        super(settings);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);
        // La faim se decide cote serveur, qui la synchronise ensuite au client.
        if (user instanceof ServerPlayerEntity player) {
            player.getHungerManager().setFoodLevel(penalizedFoodLevel(player.getHungerManager().getFoodLevel()));
        }
        return result;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(TOOLTIP_WARNING);
    }

    /**
     * Niveau de faim apres consommation.
     *
     * @param currentFoodLevel niveau de faim courant
     * @return niveau de faim penalise, jamais negatif
     */
    public static int penalizedFoodLevel(int currentFoodLevel) {
        return Math.max(0, currentFoodLevel - HUNGER_PENALTY);
    }
}
