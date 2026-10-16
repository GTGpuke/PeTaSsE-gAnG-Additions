package com.petassegang.addons.mixin.client;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gl.Framebuffer;

import com.petassegang.addons.backrooms.portal.client.view.PortalRenderState;
import com.petassegang.addons.config.ModConfig;

/**
 * Remplace l'attachement de profondeur des framebuffers par un attachement profondeur + stencil,
 * necessaire au decoupage des portails. Sans effet si la feature portail est desactivee ; si le
 * pilote refuse l'attachement, l'ancien est retabli et le rendu distant est coupe pour la session.
 */
@Mixin(Framebuffer.class)
public abstract class MixinFramebuffer {

    @Shadow @Final public boolean useDepthAttachment;
    @Shadow public int fbo;
    @Shadow protected int depthAttachment;

    @Inject(method = "initFbo", at = @At("RETURN"))
    private void backrooms$attachStencil(int width, int height, boolean getError, CallbackInfo ci) {
        if (!ModConfig.ENABLE_PORTAL_FEATURE || !this.useDepthAttachment || this.depthAttachment <= 0
                || !PortalRenderState.remoteRenderingAvailable()) {
            return;
        }
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, this.fbo);
            GlStateManager._bindTexture(this.depthAttachment);
            GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, 0, GL30.GL_DEPTH24_STENCIL8, width, height, 0,
                    GL30.GL_DEPTH_STENCIL, GL30.GL_UNSIGNED_INT_24_8, null);
            GlStateManager._glFramebufferTexture2D(GlConst.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT,
                    GlConst.GL_TEXTURE_2D, this.depthAttachment, 0);
            int status = GlStateManager.glCheckFramebufferStatus(GlConst.GL_FRAMEBUFFER);
            if (status != GlConst.GL_FRAMEBUFFER_COMPLETE) {
                // Retour a la profondeur seule, comme vanilla.
                GlStateManager._texImage2D(GlConst.GL_TEXTURE_2D, 0, GlConst.GL_DEPTH_COMPONENT, width, height, 0,
                        GlConst.GL_DEPTH_COMPONENT, GlConst.GL_FLOAT, null);
                GlStateManager._glFramebufferTexture2D(GlConst.GL_FRAMEBUFFER, GlConst.GL_DEPTH_ATTACHMENT,
                        GlConst.GL_TEXTURE_2D, this.depthAttachment, 0);
                PortalRenderState.markStencilUnavailable(status);
            }
        } finally {
            GlStateManager._bindTexture(previousTexture);
            GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, previousFramebuffer);
        }
    }
}
