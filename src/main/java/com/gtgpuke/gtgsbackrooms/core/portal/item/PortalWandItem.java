package com.gtgpuke.gtgsbackrooms.core.portal.item;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.gtgpuke.gtgsbackrooms.core.ModConstants;
import com.gtgpuke.gtgsbackrooms.core.portal.block.BackroomsPortalBlockEntity;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalExit;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalSettings;
import com.gtgpuke.gtgsbackrooms.core.portal.block.PortalWayBack;
import com.gtgpuke.gtgsbackrooms.core.portal.sync.PortalWandSettingsPayload;
import com.gtgpuke.gtgsbackrooms.core.registry.ModBlockEntities;
import com.gtgpuke.gtgsbackrooms.core.registry.ModBlocks;
import com.gtgpuke.gtgsbackrooms.core.registry.ModDataComponents;
import com.gtgpuke.gtgsbackrooms.core.registry.ModItems;
import com.gtgpuke.gtgsbackrooms.core.registry.ModSounds;

/**
 * Baguette de portail, seule facon de creer un portail. Clic droit sur un bloc, elle pose a cote un bloc portail aux
 * reglages qu'elle porte (composant {@code portal_settings}), sans s'user ; accroupi, clic droit sur un portail (qui ne se
 * vise qu'accroupi), elle prend ses reglages. Clic droit dans le vide, le client ouvre son ecran de reglages, qui renvoie au
 * serveur les reglages choisis ({@link PortalWandSettingsPayload}) ; le serveur les pose sur la baguette tenue.
 */
public final class PortalWandItem extends Item {

    private static final String TOOLTIP = "tooltip." + ModConstants.MOD_ID + ".portal_wand.";
    private static final String TOOLTIP_PLACE = TOOLTIP + "place";
    private static final String TOOLTIP_CONFIGURE = TOOLTIP + "configure";
    private static final String TOOLTIP_SNEAK = TOOLTIP + "sneak";
    /** Cles des lignes qui decrivent les reglages portes, composees une fois et non a chaque image ou l'info-bulle s'affiche. */
    private static final String SETTINGS = "tooltip." + ModConstants.MOD_ID + ".portal_settings.";
    private static final String SETTINGS_SIZE = SETTINGS + "size";
    private static final String SETTINGS_LIGHT = SETTINGS + "light";
    private static final String SETTINGS_NO_LIGHT = SETTINGS + "no_light";
    private static final String SETTINGS_DOUBLE_SIDED = SETTINGS + "double_sided";
    private static final String SETTINGS_SINGLE_SIDED = SETTINGS + "single_sided";
    private static final String SETTINGS_EXIT = SETTINGS + "exit";
    private static final String SETTINGS_EXIT_SCATTER = SETTINGS + "exit_scatter";
    private static final String SETTINGS_FLUSH = SETTINGS + "flush";
    /** Cle de la ligne de chaque retour, par ordinal. */
    private static final String[] SETTINGS_WAY_BACK_KEYS = wayBackKeys();
    /** Cle du nom de chaque sens de sortie, par ordinal. */
    private static final String[] SETTINGS_EXIT_KEYS = exitKeys();
    /** Message de la barre d'action quand la baguette prend les reglages d'un portail. */
    private static final String COPIED = "message." + ModConstants.MOD_ID + ".portal_wand.copied";
    private static boolean initialized;

    /** Baguette aux reglages de l'item (ceux par defaut). */
    public PortalWandItem(Settings settings) {
        super(settings);
    }

    /** Recoit les reglages choisis dans l'ecran de la baguette ; un second appel ne fait rien. */
    public static void init() {
        if (initialized) {
            return;
        }
        ServerPlayNetworking.registerGlobalReceiver(PortalWandSettingsPayload.ID, (payload, context) -> applySettings(context.player(), payload));
        initialized = true;
    }

    /**
     * Pose sur la baguette tenue dans la main indiquee les reglages choisis dans son ecran ; rien si cette main ne la tient
     * plus (changee de main, lachee entre-temps).
     *
     * @param player joueur qui a choisi les reglages
     * @param payload main et reglages choisis
     */
    static void applySettings(PlayerEntity player, PortalWandSettingsPayload payload) {
        ItemStack stack = player.getStackInHand(payload.hand());
        if (stack.isOf(ModItems.PORTAL_WAND)) {
            stack.set(ModDataComponents.PORTAL_SETTINGS, payload.settings());
        }
    }

    /**
     * Sur un portail, prend ses reglages ; ailleurs, pose un bloc portail comme vanilla pose un bloc : face avant vers le
     * joueur, ou couche selon le regard, memes refus (place occupee, cadre qui ne tient pas, hors du monde), meme
     * evenement de monde, et le son d'un portail qui s'ouvre. Un refus rend un echec : vanilla ne passe pas alors a l'usage
     * dans le vide, et l'ecran de reglages ne s'ouvre jamais quand on vise un bloc.
     */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        ItemStack wand = context.getStack();
        if (world.getBlockEntity(context.getBlockPos()) instanceof BackroomsPortalBlockEntity portal) {
            wand.set(ModDataComponents.PORTAL_SETTINGS, portal.settings());
            // Le serveur seul previent le joueur : le client, qui joue aussi le clic, afficherait le message deux fois.
            if (context.getPlayer() instanceof ServerPlayerEntity player) {
                player.sendMessage(Text.translatable(COPIED), true);
            }
            return ActionResult.success(world.isClient);
        }
        ItemPlacementContext placement = new ItemPlacementContext(context);
        // L'etat de pose verifie deja que chaque bloc du cadre, celui vise compris, est libre ou remplacable.
        BlockState state = ModBlocks.BACKROOMS_PORTAL.getPlacementState(placement);
        BlockPos pos = placement.getBlockPos();
        if (state == null || !world.setBlockState(pos, state, Block.NOTIFY_ALL_AND_REDRAW)) {
            return ActionResult.FAIL;
        }
        world.getBlockEntity(pos, ModBlockEntities.BACKROOMS_PORTAL).ifPresent(portal -> {
            portal.readComponents(wand);
            portal.markDirty();
        });
        world.playSound(context.getPlayer(), pos, ModSounds.PORTAL_WAND_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        world.emitGameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Emitter.of(context.getPlayer(), state));
        return ActionResult.success(world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        appendSettings(PortalSettings.of(stack), tooltip);
        tooltip.add(Text.translatable(TOOLTIP_PLACE).formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable(TOOLTIP_CONFIGURE).formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable(TOOLTIP_SNEAK).formatted(Formatting.DARK_GRAY));
    }

    /**
     * Lignes qui decrivent des reglages de portail : taille, lumiere, faces, retour, puis ce qui n'est pas par defaut (sens
     * de sortie, tirage au sort, plan au bord du bloc).
     */
    private static void appendSettings(PortalSettings settings, List<Text> tooltip) {
        tooltip.add(Text.translatable(SETTINGS_SIZE, settings.width(), settings.height()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(settings.transmitsLight() ? SETTINGS_LIGHT : SETTINGS_NO_LIGHT).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(settings.doubleSided() ? SETTINGS_DOUBLE_SIDED : SETTINGS_SINGLE_SIDED).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(SETTINGS_WAY_BACK_KEYS[PortalWayBack.of(settings).ordinal()]).formatted(Formatting.GRAY));
        if (settings.exit() != PortalExit.STRAIGHT) {
            tooltip.add(Text.translatable(SETTINGS_EXIT, Text.translatable(SETTINGS_EXIT_KEYS[settings.exit().ordinal()])).formatted(Formatting.GRAY));
        }
        if (settings.exitScatter() > 0) {
            tooltip.add(Text.translatable(SETTINGS_EXIT_SCATTER, settings.exitScatter()).formatted(Formatting.GRAY));
        }
        if (settings.flush()) {
            tooltip.add(Text.translatable(SETTINGS_FLUSH).formatted(Formatting.GRAY));
        }
    }

    private static String[] wayBackKeys() {
        PortalWayBack[] ways = PortalWayBack.values();
        String[] keys = new String[ways.length];
        for (int i = 0; i < ways.length; i++) {
            keys[i] = SETTINGS + ways[i].key();
        }
        return keys;
    }

    private static String[] exitKeys() {
        PortalExit[] exits = PortalExit.values();
        String[] keys = new String[exits.length];
        for (int i = 0; i < exits.length; i++) {
            keys[i] = SETTINGS_EXIT + "." + exits[i].asString();
        }
        return keys;
    }
}
