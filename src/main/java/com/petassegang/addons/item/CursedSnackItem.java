package com.petassegang.addons.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Casse-croûte Maudit — consommable qui retire 2 points de nourriture au joueur.
 *
 * <p>Propriétés :
 * <ul>
 *   <li>Toujours mangeable ({@code canAlwaysEat}), même le ventre plein.</li>
 *   <li>Consommation : retire 2 points de faim (min 0).</li>
 *   <li>Taille de pile : 16.</li>
 * </ul>
 */
public class CursedSnackItem extends Item {

    /** Tooltip de mise en garde pré-alloué. */
    private static final Component TOOLTIP_WARNING = Component.translatable(
            "tooltip.petasse_gang_additions.cursed_snack.warning")
            .setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withItalic(true));

    /**
     * Crée une instance du Casse-croûte Maudit avec les propriétés spécifiées.
     *
     * @param properties les propriétés de l'item (doit inclure {@code food} avec {@code alwaysEdible})
     */
    public CursedSnackItem(Properties properties) {
        super(properties);
    }

    /**
     * Fin de consommation : appelle le super (animation + statistiques),
     * puis soustrait 2 points de faim au joueur (min 0).
     *
     * @param stack  la pile d'items consommée
     * @param level  le monde courant
     * @param entity l'entité qui a consommé l'item
     * @return la pile résultante après consommation
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof Player player) {
            // Retire 2 points de nourriture — eat(-2, 0) applique Mth.clamp(foodLevel - 2, 0, 20).
            player.getFoodData().eat(-2, 0.0f);
        }
        return result;
    }

    /**
     * Ajoute la ligne de mise en garde au tooltip.
     *
     * @param stack           la pile survolée
     * @param context         contexte du tooltip
     * @param display         paramètres d'affichage
     * @param tooltipConsumer consumer de lignes de tooltip
     * @param flag            tooltips avancés activés ou non
     */
    // Méthode dépréciée en MC 26.1 sans remplacement stable — les items vanilla l'utilisent encore.
    @SuppressWarnings("deprecation") // Methode deprecatee en MC 26.1 sans remplacement stable cote item.
    @Override
    public void appendHoverText(ItemStack stack,
                                TooltipContext context,
                                TooltipDisplay display,
                                Consumer<Component> tooltipConsumer,
                                TooltipFlag flag) {
        tooltipConsumer.accept(TOOLTIP_WARNING);
    }
}
