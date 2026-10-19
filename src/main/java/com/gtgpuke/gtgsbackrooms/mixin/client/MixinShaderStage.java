package com.gtgpuke.gtgsbackrooms.mixin.client;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.gl.ShaderStage;
import net.minecraft.resource.ResourceFactory;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.ShaderClipInjector;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view.ShaderStageOrigin;
import com.gtgpuke.gtgsbackrooms.config.ModConfig;

/**
 * Au chargement d'un shader, ajoute le plan de decoupe des vues de portail aux vertex shaders
 * ({@link ShaderClipInjector}). La source arrive ici deja debarrassee de ses {@code #moj_import}. Retient aussi la
 * fabrique qui a fourni la source ({@link ShaderStageOrigin}).
 */
@Mixin(ShaderStage.class)
public abstract class MixinShaderStage implements ShaderStageOrigin {

    @Unique
    private @Nullable ResourceFactory backrooms$factory;

    @Override
    public @Nullable ResourceFactory backrooms$getFactory() {
        return this.backrooms$factory;
    }

    @Override
    public void backrooms$setFactory(ResourceFactory factory) {
        this.backrooms$factory = factory;
    }

    @ModifyArg(
            method = "load",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;glShaderSource(ILjava/util/List;)V"),
            index = 1)
    private static List<String> backrooms$injectClipPlane(List<String> sources) {
        if (!ModConfig.ENABLE_PORTAL_FEATURE) {
            return sources;
        }
        String joined = String.join("", sources);
        String injected = ShaderClipInjector.inject(joined);
        // Meme instance : shader non concerne, la liste d'origine repart telle quelle.
        return injected == joined ? sources : List.of(injected);
    }
}
