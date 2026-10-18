package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.interaction;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.RemoteWorld;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.PortalBlockActionPayload;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.sync.PortalEntityActionPayload;

/**
 * Clics du joueur quand il vise a travers un portail : frapper, miner, utiliser.
 *
 * <p>Branche en tete de {@code MinecraftClient.doAttack}, {@code handleBlockBreaking} et {@code doItemUse}.
 * Quand une cible existe de l'autre cote, l'action part au serveur et vanilla ne fait rien ; sinon vanilla
 * garde la main (un coup dans le vide, manger, lancer). Le minage n'est pas predit : le client annonce le
 * debut et l'abandon, le serveur compte la progression et casse le bloc.
 */
public final class RemoteActions {

    /** Delais vanilla : quatre ticks entre deux utilisations, cinq entre deux blocs casses en creatif. */
    private static final int USE_COOLDOWN_TICKS = 4;
    private static final int BREAK_COOLDOWN_TICKS = 5;

    private static int useCooldown;
    private static int breakCooldown;
    /** Bloc en cours de minage a travers un portail, ou {@code null}. */
    private static @Nullable BlockHitResult miningHit;
    private static @Nullable UUID miningPortal;
    private static @Nullable Identifier miningDimension;

    private RemoteActions() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Tick client : les delais s'ecoulent. */
    public static void onClientTick() {
        if (useCooldown > 0) {
            useCooldown--;
        }
        if (breakCooldown > 0) {
            breakCooldown--;
        }
    }

    /** Deconnexion : plus rien en cours. */
    public static void clear() {
        useCooldown = 0;
        breakCooldown = 0;
        miningHit = null;
        miningPortal = null;
        miningDimension = null;
    }

    /**
     * Clic gauche.
     *
     * @return {@code true} si le coup est porte a travers un portail et que vanilla ne doit rien faire
     */
    public static boolean attack(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BackroomsPortalEntity portal = RemoteTargeting.portal();
        if (player == null || portal == null || client.world == null) {
            return false;
        }
        // Les gardes de vanilla avant tout coup : delai d'attaque, monture, objet desactive. Refuse, vanilla refuse aussi.
        if (client.attackCooldown > 0 || player.isRiding() || !player.getMainHandStack().isItemEnabled(client.world.getEnabledFeatures())) {
            return false;
        }
        EntityHitResult entityHit = RemoteTargeting.entityHit();
        if (entityHit != null) {
            ClientPlayNetworking.send(new PortalEntityActionPayload(portal.getUuid(), portal.destinationDim().getValue(),
                    entityHit.getEntity().getId(), PortalEntityActionPayload.Action.ATTACK));
            player.resetLastAttackedTicks();
            player.swingHand(Hand.MAIN_HAND);
            return true;
        }
        BlockHitResult blockHit = RemoteTargeting.blockHit();
        if (blockHit == null) {
            // Le regard traverse le cadre sans rien toucher : coup dans le vide, vanilla s'en charge.
            return false;
        }
        stopVanillaMining(client);
        startMining(player, portal, blockHit);
        return true;
    }

    /**
     * Bouton gauche maintenu (ou relache) a chaque tick.
     *
     * @return {@code true} si le minage se fait a travers un portail et que vanilla ne doit rien faire
     */
    public static boolean handleBlockBreaking(MinecraftClient client, boolean breaking) {
        ClientPlayerEntity player = client.player;
        BackroomsPortalEntity portal = RemoteTargeting.portal();
        BlockHitResult blockHit = RemoteTargeting.blockHit();
        if (player == null || portal == null || blockHit == null) {
            // La cible a quitte le portail : vanilla reprend la main, le serveur efface les fissures.
            abortMining();
            return false;
        }
        // Comme vanilla : bouton relache, delai d'attaque ou usage en cours (manger, bouclier leve), rien ne se mine. Vanilla
        // remet le delai a zero au relachement et arrete son propre minage.
        if (!breaking || client.attackCooldown > 0 || player.isUsingItem()) {
            abortMining();
            return false;
        }
        stopVanillaMining(client);
        if (miningHit == null || !miningHit.getBlockPos().equals(blockHit.getBlockPos()) || !portal.getUuid().equals(miningPortal)) {
            startMining(player, portal, blockHit);
        } else {
            player.swingHand(Hand.MAIN_HAND);
        }
        return true;
    }

    /**
     * Clic droit.
     *
     * @return {@code true} si l'utilisation vise une cible a travers un portail et que vanilla ne doit rien faire
     */
    public static boolean use(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        BackroomsPortalEntity portal = RemoteTargeting.portal();
        HitResult hit = RemoteTargeting.hit();
        if (player == null || portal == null || hit == null) {
            return false;
        }
        if (useCooldown > 0) {
            return true;
        }
        useCooldown = USE_COOLDOWN_TICKS;
        Identifier dimension = portal.destinationDim().getValue();
        if (hit instanceof EntityHitResult entityHit) {
            ClientPlayNetworking.send(new PortalEntityActionPayload(portal.getUuid(), dimension, entityHit.getEntity().getId(),
                    PortalEntityActionPayload.Action.INTERACT));
        } else if (hit instanceof BlockHitResult blockHit) {
            ClientPlayNetworking.send(new PortalBlockActionPayload(portal.getUuid(), dimension, blockHit, PortalBlockActionPayload.Action.USE));
        }
        player.swingHand(Hand.MAIN_HAND);
        return true;
    }

    /**
     * Clic molette. Si le regard ressort de l'autre cote d'un portail sur une cible, le choix vanilla s'execute avec
     * cette cible et le monde ou elle se trouve a la place de la cible et du monde du joueur : bloc choisi, donnees de
     * block entity en creatif (Ctrl), oeuf d'une creature en creatif, comme si elle etait devant lui.
     *
     * @param client client du joueur
     * @param vanilla choix vanilla, execute une fois dans tous les cas
     */
    public static void pick(MinecraftClient client, Runnable vanilla) {
        HitResult hit = RemoteTargeting.hit();
        RemoteWorld view = RemoteTargeting.view();
        ClientWorld world = client.world;
        if (hit == null || view == null || world == null) {
            vanilla.run();
            return;
        }
        HitResult crosshair = client.crosshairTarget;
        client.crosshairTarget = hit;
        client.world = view.world();
        try {
            vanilla.run();
        } finally {
            client.world = world;
            client.crosshairTarget = crosshair;
        }
    }

    /** Le minage vanilla d'un bloc du monde du joueur ne doit pas continuer en aveugle pendant qu'il vise a travers un portail. */
    private static void stopVanillaMining(MinecraftClient client) {
        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        if (interactionManager != null && interactionManager.isBreakingBlock()) {
            interactionManager.cancelBlockBreaking();
        }
    }

    private static void startMining(ClientPlayerEntity player, BackroomsPortalEntity portal, BlockHitResult hit) {
        abortMining();
        if (player.isCreative()) {
            if (breakCooldown > 0) {
                return;
            }
            breakCooldown = BREAK_COOLDOWN_TICKS;
        } else {
            miningHit = hit;
            miningPortal = portal.getUuid();
            miningDimension = portal.destinationDim().getValue();
        }
        ClientPlayNetworking.send(new PortalBlockActionPayload(portal.getUuid(), portal.destinationDim().getValue(), hit,
                PortalBlockActionPayload.Action.START_MINING));
        player.swingHand(Hand.MAIN_HAND);
    }

    private static void abortMining() {
        if (miningHit == null || miningPortal == null || miningDimension == null) {
            return;
        }
        ClientPlayNetworking.send(new PortalBlockActionPayload(miningPortal, miningDimension, miningHit, PortalBlockActionPayload.Action.ABORT_MINING));
        miningHit = null;
        miningPortal = null;
        miningDimension = null;
    }
}
