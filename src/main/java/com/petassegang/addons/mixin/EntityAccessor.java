package com.petassegang.addons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraft.world.entity.EntityChangeListener;

/**
 * Deplace une entity d'un monde a l'autre sans passer par le changement de dimension vanilla :
 * le joueur qui traverse un portail, cote serveur comme cote client.
 */
@Mixin(Entity.class)
public interface EntityAccessor {

    /** Change le monde de l'entity sans aucun autre effet. */
    @Invoker("setWorld")
    void backrooms$setWorld(World world);

    /** Annule le retrait de l'entity : elle redevient vivante pour son nouveau monde. */
    @Invoker("unsetRemoved")
    void backrooms$unsetRemoved();

    /** Ecouteur de deplacement du gestionnaire d'entities, a remettre apres une transposition temporaire. */
    @Accessor("changeListener")
    EntityChangeListener backrooms$getChangeListener();
}
