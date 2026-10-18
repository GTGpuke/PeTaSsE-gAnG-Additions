package com.petassegang.addons.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.World;

import com.petassegang.addons.backrooms.portal.light.VirtualLightSources;
import com.petassegang.addons.backrooms.portal.light.VirtualLightView;

/**
 * Chaque monde (serveur, client, monde distant du client) porte ses sources de lumiere virtuelles : elles disparaissent
 * avec lui, sans registre a vider.
 */
@Mixin(World.class)
public abstract class MixinWorld implements VirtualLightView {

    /** Publiees par le thread principal, lues par le moteur de lumiere sur le sien. */
    @Unique
    private volatile VirtualLightSources backrooms$virtualLight = VirtualLightSources.EMPTY;

    @Override
    public VirtualLightSources backrooms$virtualLight() {
        return this.backrooms$virtualLight;
    }

    @Override
    public void backrooms$setVirtualLight(VirtualLightSources sources) {
        this.backrooms$virtualLight = sources;
    }
}
