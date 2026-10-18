package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.PortalClipping;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.ShaderClipInjector;

/**
 * A chaque activation d'un shader pendant le dessin d'une vue de portail, lui transmet le plan de decoupe courant
 * ({@link PortalClipping}). L'emplacement de l'uniforme est resolu une fois par shader.
 */
@Mixin(ShaderProgram.class)
public abstract class MixinShaderProgram {

    @Unique
    private boolean backrooms$clipPlaneResolved;
    @Unique
    private int backrooms$clipPlaneLocation;

    @Shadow
    public abstract int getGlRef();

    @Inject(method = "bind", at = @At("TAIL"))
    private void backrooms$uploadClipPlane(CallbackInfo ci) {
        if (!PortalClipping.isActive()) {
            return;
        }
        if (!this.backrooms$clipPlaneResolved) {
            this.backrooms$clipPlaneLocation = GlUniform.getUniformLocation(this.getGlRef(), ShaderClipInjector.UNIFORM);
            this.backrooms$clipPlaneResolved = true;
        }
        PortalClipping.onProgramBound(this.backrooms$clipPlaneLocation);
    }
}
