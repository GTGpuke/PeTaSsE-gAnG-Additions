package com.petassegang.addons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.World;

/**
 * Acces a la meteo brute d'un monde, telle que vanilla l'envoie aux clients : {@code getThunderGradient} rend l'orage
 * deja multiplie par la pluie.
 */
@Mixin(World.class)
public interface WorldAccessor {

    /** Pluie, de 0 a 1. */
    @Accessor("rainGradient")
    float backrooms$getRainGradient();

    /** Orage, de 0 a 1, avant sa multiplication par la pluie. */
    @Accessor("thunderGradient")
    float backrooms$getThunderGradient();
}
