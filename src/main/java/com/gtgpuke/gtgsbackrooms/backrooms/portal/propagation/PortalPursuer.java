package com.gtgpuke.gtgsbackrooms.backrooms.portal.propagation;

import org.jetbrains.annotations.Nullable;

/**
 * Mob capable de poursuivre une cible a travers un portail : la poursuite en cours est portee par le mob lui-meme
 * (interface ajoutee a {@code MobEntity} par mixin).
 */
public interface PortalPursuer {

    /** Poursuite en cours, ou {@code null}. */
    @Nullable PortalPursuit.Pursuit backrooms$getPursuit();

    /** Commence, remplace ou arrete ({@code null}) la poursuite. */
    void backrooms$setPursuit(@Nullable PortalPursuit.Pursuit pursuit);
}
