package com.petassegang.addons.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Le Badge de la Gang — jeton officiel d'appartenance à la PétasseGang.
 *
 * <p>Propriétés :
 * <ul>
 *   <li>Taille de pile : 1 (badge unique)</li>
 *   <li>Rareté         : EPIC (effet de brillance)</li>
 *   <li>Tooltip        : nom du membre + texte de saveur</li>
 * </ul>
 */
public class GangBadgeItem extends Item {

    /** Composants de tooltip pré-alloués — jamais recréés sur le hot-path de rendu. */
    private static final Component TOOLTIP_MEMBER = Component.translatable("tooltip.petassegang_addons.gang_badge.member")
            .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));

    private static final Component TOOLTIP_FLAVOUR = Component.translatable("tooltip.petassegang_addons.gang_badge.flavour")
            .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true));

    public GangBadgeItem(Properties properties) {
        super(properties);
    }

    /**
     * Ajoute les lignes de tooltip personnalisées à l'item survolé.
     *
     * @param stack    la pile d'items survolée
     * @param context  contexte du tooltip (accès au monde)
     * @param tooltip  liste mutable de lignes à compléter
     * @param flag     indique si les tooltips avancés sont activés
     */
    @Override
    public void appendHoverText(ItemStack stack,
                                TooltipContext context,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(TOOLTIP_MEMBER);
        tooltip.add(TOOLTIP_FLAVOUR);
    }

    /**
     * Retourne {@code true} pour que le badge affiche toujours la brillance d'enchantement,
     * qu'il soit enchanté ou non.
     *
     * @param stack la pile d'items à vérifier
     * @return toujours {@code true}
     */
    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
