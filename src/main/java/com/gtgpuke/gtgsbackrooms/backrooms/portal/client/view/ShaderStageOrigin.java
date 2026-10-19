package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resource.ResourceFactory;

/**
 * Fabrique de ressources qui a fourni la source d'un etage de shader compile (interface ajoutee a {@code ShaderStage}
 * par mixin) : vanilla ne redonne un etage deja compile qu'aux programmes charges par la meme fabrique
 * ({@code MixinShaderProgram}).
 */
public interface ShaderStageOrigin {

    /** Fabrique de la source, {@code null} pour un etage compile hors du chargement d'un programme. */
    @Nullable ResourceFactory backrooms$getFactory();

    /** Retient la fabrique qui vient de fournir la source de l'etage. */
    void backrooms$setFactory(ResourceFactory factory);
}
