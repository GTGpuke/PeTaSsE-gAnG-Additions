package com.gtgpuke.gtgsbackrooms.feature.gang;

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

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/**
 * Badge de la Gang : jeton officiel d'appartenance au groupe.
 *
 * <p>Clic droit : miaulement et animation de totem cote client. L'item n'est jamais consomme.
 */
public class GangBadgeItem extends Item {

    /** Tooltips pre-alloues : jamais recrees sur le chemin de rendu. */
    static final MutableText TOOLTIP_MEMBER =
            Text.translatable("tooltip.gtgs_backrooms.gang_badge.member")
                    .formatted(Formatting.GOLD, Formatting.BOLD);
    static final MutableText TOOLTIP_FLAVOUR =
            Text.translatable("tooltip.gtgs_backrooms.gang_badge.flavour")
                    .formatted(Formatting.GRAY, Formatting.ITALIC);

    public GangBadgeItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (user instanceof ServerPlayerEntity serverPlayer) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_CAT_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.6f);
            ServerPlayNetworking.send(serverPlayer, GangBadgeActivatePayload.INSTANCE);
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(TOOLTIP_MEMBER);
        tooltip.add(TOOLTIP_FLAVOUR);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}
