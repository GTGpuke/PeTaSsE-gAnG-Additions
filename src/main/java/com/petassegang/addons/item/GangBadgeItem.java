package com.petassegang.addons.item;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import com.petassegang.addons.network.ModNetworking;
import com.petassegang.addons.network.packet.GangBadgeActivatePacket;

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
    private static final Component TOOLTIP_MEMBER = Component.translatable("tooltip.petasse_gang_additions.gang_badge.member")
            .setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));

    private static final Component TOOLTIP_FLAVOUR = Component.translatable("tooltip.petasse_gang_additions.gang_badge.flavour")
            .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true));

    /**
     * Crée une instance du Gang Badge avec les propriétés spécifiées.
     *
     * @param properties les propriétés de l'item (stack size, rareté, id)
     */
    public GangBadgeItem(Properties properties) {
        super(properties);
    }

    /**
     * Clic droit : joue un son de chat et déclenche l'animation du totem.
     * Aucune consommation de l'item.
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            // Son de chat (pitch abaissé pour sonner adulte).
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CAT_AMBIENT_BABY, SoundSource.PLAYERS, 1.0F, 0.6F);
            // Packet custom : affiche l'animation d'activation avec la texture du badge,
            // pas celle du totem vanilla (event 35 utilise findTotem() hardcodé côté client).
            ModNetworking.CHANNEL.send(
                    new GangBadgeActivatePacket(),
                    PacketDistributor.PLAYER.with((ServerPlayer) player));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Ajoute les lignes de tooltip personnalisées à l'item survolé.
     *
     * <p>Méthode marquée {@code @Deprecated} en MC 26.1 sans remplacement stable —
     * les items vanilla (ex. {@code SmithingTemplateItem}) l'utilisent encore.
     * À migrer vers {@link net.minecraft.world.item.component.TooltipProvider} quand l'API sera stabilisée.
     *
     * @param stack          la pile d'items survolée
     * @param context        contexte du tooltip (accès au monde)
     * @param display        paramètres d'affichage du tooltip
     * @param tooltipConsumer consumer auquel passer chaque ligne de tooltip
     * @param flag           indique si les tooltips avancés sont activés
     */
    @SuppressWarnings("deprecation") // Méthode dépréciée en MC 26.1 sans remplacement stable — les items vanilla l'utilisent encore.
    @Override
    public void appendHoverText(ItemStack stack,
                                TooltipContext context,
                                TooltipDisplay display,
                                Consumer<Component> tooltipConsumer,
                                TooltipFlag flag) {
        tooltipConsumer.accept(TOOLTIP_MEMBER);
        tooltipConsumer.accept(TOOLTIP_FLAVOUR);
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
