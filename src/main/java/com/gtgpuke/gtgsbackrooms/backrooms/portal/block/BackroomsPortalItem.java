package com.gtgpuke.gtgsbackrooms.backrooms.portal.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;

/**
 * Item du bloc portail : il porte les reglages du portail a poser (composant {@code portal_settings}) et les affiche, dans
 * son nom et dans son info-bulle.
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
    private static final String TOOLTIP_FLEETING_EXIT = TOOLTIP + "fleeting_exit";
    private static final String TOOLTIP_ROUND_TRIP = TOOLTIP + "round_trip";
    private static final String TOOLTIP_PLACE = TOOLTIP + "place";
    private static final String TOOLTIP_BREAK = TOOLTIP + "break";
    private static final String TOOLTIP_EXIT = TOOLTIP + "exit";
    private static final String TOOLTIP_EXIT_SCATTER = TOOLTIP + "exit_scatter";
    /** Cle du nom de chaque sens de sortie, par ordinal. */
    private static final String[] EXIT_KEYS = exitKeys(TOOLTIP_EXIT);
    /** Cles du nom d'une pile reglee, sous celle du bloc comme le nom lui-meme : ce qui l'ecarte du portail par defaut. */
    private static final String VARIANT = "block." + ModConstants.MOD_ID + ".backrooms_portal.variant";
    private static final String VARIANT_SIZE = VARIANT + ".size";
    private static final String VARIANT_EXIT_SCATTER = VARIANT + ".exit_scatter";
    private static final String VARIANT_ONE_WAY = VARIANT + ".one_way";
    private static final String VARIANT_FLEETING_EXIT = VARIANT + ".fleeting_exit";
    private static final String VARIANT_SINGLE_SIDED = VARIANT + ".single_sided";
    private static final String VARIANT_NO_LIGHT = VARIANT + ".no_light";
    private static final String[] VARIANT_EXIT_KEYS = exitKeys(VARIANT + ".exit");
    private static final Text VARIANT_SEPARATOR = Text.literal(", ");

    /** Item du bloc portail ; les reglages par defaut viennent des reglages de l'item. */
    public BackroomsPortalItem(Block block, Settings settings) {
        super(block, settings);
    }

    /**
     * Nom de la pile : celui du bloc, suivi entre parentheses de ce qui l'ecarte du portail par defaut (taille, sens de
     * sortie, tirage au sort, retour, faces, lumiere). Les reglages proposes dans l'onglet creatif se distinguent ainsi
     * sans ouvrir leur info-bulle. Une pile renommee garde son nom : vanilla le lit avant celui-ci. Comme l'info-bulle, le
     * nom se compose a chaque appel : vanilla le demande a chaque image ou il l'affiche, deux secondes apres un changement
     * d'objet en main ou au survol d'une pile, un evenement borne ; rien n'est garde d'un appel a l'autre.
     */
    @Override
    public Text getName(ItemStack stack) {
        Text name = super.getName(stack);
        PortalSettings settings = PortalSettings.of(stack);
        List<Text> traits = new ArrayList<>();
        if (settings.width() != PortalSettings.DEFAULT.width() || settings.height() != PortalSettings.DEFAULT.height()) {
            traits.add(Text.translatable(VARIANT_SIZE, settings.width(), settings.height()));
        }
        if (settings.exit() != PortalExit.STRAIGHT) {
            traits.add(Text.translatable(VARIANT_EXIT_KEYS[settings.exit().ordinal()]));
        }
        if (settings.exitScatter() > 0) {
            traits.add(Text.translatable(VARIANT_EXIT_SCATTER, settings.exitScatter()));
        }
        if (settings.oneWay()) {
            traits.add(Text.translatable(VARIANT_ONE_WAY));
        } else if (settings.fleetingExit()) {
            traits.add(Text.translatable(VARIANT_FLEETING_EXIT));
        }
        if (!settings.doubleSided()) {
            traits.add(Text.translatable(VARIANT_SINGLE_SIDED));
        }
        if (!settings.transmitsLight()) {
            traits.add(Text.translatable(VARIANT_NO_LIGHT));
        }
        return traits.isEmpty() ? name : Text.translatable(VARIANT, name, Texts.join(traits, VARIANT_SEPARATOR));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        PortalSettings settings = PortalSettings.of(stack);
        tooltip.add(Text.translatable(TOOLTIP_SIZE, settings.width(), settings.height()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(settings.transmitsLight() ? TOOLTIP_LIGHT : TOOLTIP_NO_LIGHT).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(settings.doubleSided() ? TOOLTIP_DOUBLE_SIDED : TOOLTIP_SINGLE_SIDED).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(returnTooltip(settings)).formatted(Formatting.GRAY));
        if (settings.exit() != PortalExit.STRAIGHT) {
            tooltip.add(Text.translatable(TOOLTIP_EXIT, Text.translatable(EXIT_KEYS[settings.exit().ordinal()])).formatted(Formatting.GRAY));
        }
        if (settings.exitScatter() > 0) {
            tooltip.add(Text.translatable(TOOLTIP_EXIT_SCATTER, settings.exitScatter()).formatted(Formatting.GRAY));
        }
        tooltip.add(Text.translatable(TOOLTIP_PLACE).formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable(TOOLTIP_BREAK).formatted(Formatting.DARK_GRAY));
    }

    private static String[] exitKeys(String prefix) {
        PortalExit[] exits = PortalExit.values();
        String[] keys = new String[exits.length];
        for (int i = 0; i < exits.length; i++) {
            keys[i] = prefix + "." + exits[i].asString();
        }
        return keys;
    }

    /** Ligne du retour : aucun (aller simple), tant qu'on le voit (sortie fugace), ou toujours. */
    private static String returnTooltip(PortalSettings settings) {
        if (settings.oneWay()) {
            return TOOLTIP_ONE_WAY;
        }
        return settings.fleetingExit() ? TOOLTIP_FLEETING_EXIT : TOOLTIP_ROUND_TRIP;
    }
}
