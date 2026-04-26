package com.petassegang.addons.item;

import java.util.List;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import com.petassegang.addons.network.ModNetworking;
import com.petassegang.addons.network.packet.GangBadgeActivatePayload;

/**
 * Le Badge de la Gang — jeton officiel d'appartenance a la PetasseGang.
 */
public class GangBadgeItem extends Item {

    /** Composants de tooltip pre-alloues — jamais reecrees sur le hot-path de rendu. */
    private static final MutableText TOOLTIP_MEMBER =
            Text.translatable("tooltip.petasse_gang_additions.gang_badge.member")
                .formatted(Formatting.GOLD, Formatting.BOLD);

    private static final MutableText TOOLTIP_FLAVOUR =
            Text.translatable("tooltip.petasse_gang_additions.gang_badge.flavour")
                .formatted(Formatting.GRAY, Formatting.ITALIC);

    public GangBadgeItem(Settings settings) {
        super(settings);
    }

    /**
     * Clic droit : joue un son de chat et declenche l'animation du totem.
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient()) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_CAT_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.6f);
            ModNetworking.send((ServerPlayerEntity) user, new GangBadgeActivatePayload());
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(TOOLTIP_MEMBER);
        tooltip.add(TOOLTIP_FLAVOUR);
    }

    /** Affiche toujours la brillance d'enchantement. */
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
