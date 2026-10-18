package com.petassegang.addons.backrooms.portal.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import com.petassegang.addons.core.ModConstants;

/**
 * Item du bloc portail : il porte les reglages du portail a poser (composant {@code portal_settings}) et les affiche.
 */
public final class BackroomsPortalItem extends BlockItem {

    private static final String TOOLTIP = "tooltip." + ModConstants.MOD_ID + ".backrooms_portal.";
    /** Cles de l'info-bulle, composees une fois et non a chaque image ou elle s'affiche. */
    private static final String TOOLTIP_SIZE = TOOLTIP + "size";
    private static final String TOOLTIP_LIGHT = TOOLTIP + "light";
    private static final String TOOLTIP_NO_LIGHT = TOOLTIP + "no_light";
    private static final String TOOLTIP_DOUBLE_SIDED = TOOLTIP + "double_sided";
    private static final String TOOLTIP_SINGLE_SIDED = TOOLTIP + "single_sided";
    private static final String TOOLTIP_ONE_WAY = TOOLTIP + "one_way";
    private static final String TOOLTIP_ROUND_TRIP = TOOLTIP + "round_trip";
    private static final String TOOLTIP_BREAK = TOOLTIP + "break";

    /** Item du bloc portail ; les reglages par defaut viennent des reglages de l'item. */
    public BackroomsPortalItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        PortalSettings settings = PortalSettings.of(stack);
        tooltip.add(Text.translatable(TOOLTIP_SIZE, settings.width(), settings.height()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(settings.transmitsLight() ? TOOLTIP_LIGHT : TOOLTIP_NO_LIGHT).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(settings.doubleSided() ? TOOLTIP_DOUBLE_SIDED : TOOLTIP_SINGLE_SIDED).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(settings.oneWay() ? TOOLTIP_ONE_WAY : TOOLTIP_ROUND_TRIP).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(TOOLTIP_BREAK).formatted(Formatting.DARK_GRAY));
    }
}
