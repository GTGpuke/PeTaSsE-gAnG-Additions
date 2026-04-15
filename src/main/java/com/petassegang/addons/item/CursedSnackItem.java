package com.petassegang.addons.item;

import java.util.List;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

/**
 * Casse-croute Maudit — consommable qui retire 2 points de nourriture au joueur.
 */
public class CursedSnackItem extends Item {

    /** Tooltip de mise en garde pre-alloue. */
    private static final MutableText TOOLTIP_WARNING =
            Text.translatable("tooltip.petasse_gang_additions.cursed_snack.warning")
                .formatted(Formatting.DARK_RED, Formatting.ITALIC);

    public CursedSnackItem(Settings settings) {
        super(settings);
    }

    /**
     * Fin de consommation : soustrait 2 points de faim au joueur (min 0).
     */
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);
        if (!world.isClient() && user instanceof PlayerEntity player) {
            int current = player.getHungerManager().getFoodLevel();
            player.getHungerManager().setFoodLevel(Math.max(0, current - 2));
        }
        return result;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(TOOLTIP_WARNING);
    }
}
