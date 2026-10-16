package com.petassegang.addons.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.world.ServerChunkLoadingManager;

/**
 * Acces au suivi vanilla des entities, pour y brancher les joueurs qui regardent une dimension a travers un portail.
 */
@Mixin(ServerChunkLoadingManager.class)
public interface ServerChunkLoadingManagerAccessor {

    /** Suivis par identifiant d'entity ; les valeurs implementent {@link EntityTrackerAccessor}. */
    @Accessor("entityTrackers")
    Int2ObjectMap<?> backrooms$getEntityTrackers();
}
