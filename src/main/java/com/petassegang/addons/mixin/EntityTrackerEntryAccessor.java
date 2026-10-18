package com.petassegang.addons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.network.EntityTrackerEntry;

/**
 * Acces a l'entree d'un suivi d'entity vanilla : apres un saut par un portail de son monde, le prochain deplacement de
 * l'entity part aux clients en position absolue.
 */
@Mixin(EntityTrackerEntry.class)
public interface EntityTrackerEntryAccessor {

    /** Vrai apres une monture : au prochain envoi, vanilla donne la position absolue au lieu d'un deplacement relatif. */
    @Accessor("hadVehicle")
    void backrooms$setHadVehicle(boolean hadVehicle);
}
