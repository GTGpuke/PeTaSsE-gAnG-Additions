package com.petassegang.addons.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.render.BackgroundRenderer;

/**
 * Couleur du brouillard gardee par {@link BackgroundRenderer} : nuages et meteo la redonnent aux shaders apres les vues
 * de portail, qui l'avaient remplacee par celle de leur monde.
 */
@Mixin(BackgroundRenderer.class)
public interface BackgroundRendererAccessor {

    @Accessor("red")
    static void backrooms$setRed(float red) {
        throw new AssertionError("Accesseur remplace par Mixin.");
    }

    @Accessor("green")
    static void backrooms$setGreen(float green) {
        throw new AssertionError("Accesseur remplace par Mixin.");
    }

    @Accessor("blue")
    static void backrooms$setBlue(float blue) {
        throw new AssertionError("Accesseur remplace par Mixin.");
    }
}
