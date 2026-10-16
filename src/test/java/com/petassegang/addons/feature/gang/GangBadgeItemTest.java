package com.petassegang.addons.feature.gang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;

import com.petassegang.addons.MinecraftTestBootstrap;
import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.init.ModItems;

@DisplayName("Badge de la Gang")
class GangBadgeItemTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("Le badge est unique par pile, EPIC et toujours brillant")
    void properties() {
        ItemStack stack = new ItemStack(ModItems.GANG_BADGE);
        assertEquals(1, ModItems.GANG_BADGE.getMaxCount(), "Pile de un.");
        assertEquals(Rarity.EPIC, stack.get(DataComponentTypes.RARITY), "Rarete EPIC.");
        assertTrue(ModItems.GANG_BADGE.hasGlint(stack), "Brillance permanente.");
    }

    @Test
    @DisplayName("Le tooltip ajoute exactement les deux lignes traduites pre-allouees")
    void tooltip() {
        List<Text> tooltip = new ArrayList<>();
        ModItems.GANG_BADGE.appendTooltip(new ItemStack(ModItems.GANG_BADGE), Item.TooltipContext.DEFAULT, tooltip, TooltipType.BASIC);
        assertEquals(List.of(GangBadgeItem.TOOLTIP_MEMBER, GangBadgeItem.TOOLTIP_FLAVOUR), tooltip, "Deux lignes, dans l'ordre.");
    }

    @Test
    @DisplayName("Le payload d'activation est enregistre sous l'identifiant du mod et ne transporte rien")
    void payload() {
        assertEquals(ModConstants.MOD_ID, GangBadgeActivatePayload.ID.id().getNamespace(), "Namespace du payload.");
        assertEquals("gang_badge_activate", GangBadgeActivatePayload.ID.id().getPath(), "Chemin du payload.");
        assertEquals(GangBadgeActivatePayload.ID, GangBadgeActivatePayload.INSTANCE.getId(), "L'instance expose son identifiant.");
    }
}
