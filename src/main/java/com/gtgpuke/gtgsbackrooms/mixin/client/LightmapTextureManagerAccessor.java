package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * Lightmap des vues de portail : le gestionnaire vanilla porte, le temps d'une vue, la texture du monde vu
 * ({@code ViewLightmaps}), qu'il calcule et que chaque couche de rendu lie en l'activant, puis reprend la sienne.
 */
@Mixin(LightmapTextureManager.class)
public interface LightmapTextureManagerAccessor {

    @Accessor("dirty")
    void backrooms$setDirty(boolean dirty);

    @Accessor("texture")
    NativeImageBackedTexture backrooms$getTexture();

    @Accessor("texture")
    @Mutable
    void backrooms$setTexture(NativeImageBackedTexture texture);

    @Accessor("image")
    NativeImage backrooms$getImage();

    @Accessor("image")
    @Mutable
    void backrooms$setImage(NativeImage image);

    @Accessor("textureIdentifier")
    Identifier backrooms$getTextureIdentifier();

    @Accessor("textureIdentifier")
    @Mutable
    void backrooms$setTextureIdentifier(Identifier identifier);
}
