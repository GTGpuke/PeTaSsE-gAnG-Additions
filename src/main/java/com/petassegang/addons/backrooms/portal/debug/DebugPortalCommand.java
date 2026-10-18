package com.petassegang.addons.backrooms.portal.debug;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntityRegistry;
import com.petassegang.addons.backrooms.portal.sync.PortalViewTracker;

/**
 * Commande de developpement : {@code /backrooms portal list} et {@code /backrooms portal clear}.
 *
 * <p>Les portails se posent avec le bloc portail ; cette commande ne sert qu'a inspecter et nettoyer. Ceux d'un bloc
 * portail sont reposes par leur bloc : {@code clear} ne retire durablement que les portails libres.
 */
public final class DebugPortalCommand {

    private static boolean registered;

    private DebugPortalCommand() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** Enregistre la commande ; idempotent. */
    public static void register() {
        if (registered) {
            return;
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("backrooms")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("portal")
                                .then(CommandManager.literal("list").executes(DebugPortalCommand::list))
                                .then(CommandManager.literal("clear").executes(DebugPortalCommand::clear)))));
        registered = true;
    }

    private static int list(CommandContext<ServerCommandSource> context) {
        List<BackroomsPortalEntity> portals = BackroomsPortalEntityRegistry.all();
        for (BackroomsPortalEntity portal : portals) {
            String line = portal.getWorld().getRegistryKey().getValue() + " " + portal.getBlockPos().toShortString()
                    + " " + portal.facingSource() + " -> " + portal.destinationDim().getValue() + " "
                    + portal.destinationPos() + " " + portal.facingDestination()
                    + (portal.owner() == null ? " (libre)" : " (bloc " + portal.owner().pos().toShortString() + ")");
            context.getSource().sendFeedback(() -> Text.literal(line), false);
        }
        int count = portals.size();
        int watched = PortalViewTracker.watchedChunkCount();
        context.getSource().sendFeedback(
                () -> Text.literal(count + " portail(s) charge(s), " + watched + " chunk(s) distant(s) chez les clients."), false);
        return count;
    }

    private static int clear(CommandContext<ServerCommandSource> context) {
        List<BackroomsPortalEntity> portals = new ArrayList<>(BackroomsPortalEntityRegistry.all());
        for (BackroomsPortalEntity portal : portals) {
            portal.discard();
        }
        int removed = portals.size();
        context.getSource().sendFeedback(() -> Text.literal(removed + " portail(s) supprime(s)."), false);
        return removed;
    }
}
