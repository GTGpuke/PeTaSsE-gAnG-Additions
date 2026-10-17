package com.petassegang.addons.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.PlayerAssociatedNetworkHandler;

/**
 * Acces aux auditeurs et a l'entree d'un suivi d'entity vanilla (classe interne de {@code ServerChunkLoadingManager}).
 */
@Mixin(targets = "net.minecraft.server.world.ServerChunkLoadingManager$EntityTracker")
public interface EntityTrackerAccessor {

    /** Entree du suivi, qui sait construire les paquets d'apparition de l'entity. */
    @Accessor("entry")
    EntityTrackerEntry backrooms$getEntry();

    /** Connexions qui recoivent les paquets de l'entity ; une vue distante s'y ajoute. */
    @Accessor("listeners")
    Set<PlayerAssociatedNetworkHandler> backrooms$getListeners();
}
