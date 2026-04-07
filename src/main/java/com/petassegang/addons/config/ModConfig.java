package com.petassegang.addons.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Définitions de configuration du mod via {@link ForgeConfigSpec}.
 *
 * <p>Enregistrer les deux specs dans la classe principale du mod :
 * <pre>{@code
 * ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ModConfig.SERVER_SPEC);
 * ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModConfig.CLIENT_SPEC);
 * }</pre>
 */
public final class ModConfig {

    // ── Configuration serveur ─────────────────────────────────────────────────

    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GANG_BADGE;

    // ── Configuration client ──────────────────────────────────────────────────

    public static final ForgeConfigSpec CLIENT_SPEC;

    static {
        // --- Serveur ---
        ForgeConfigSpec.Builder serverBuilder = new ForgeConfigSpec.Builder();
        serverBuilder.comment("PeTaSsE_gAnG_Additions — Configuration Serveur")
                     .push("items");

        ENABLE_GANG_BADGE = serverBuilder
                .comment("Mettre à false pour désactiver complètement le Badge de la Gang.")
                .define("enableGangBadge", true);

        serverBuilder.pop();
        SERVER_SPEC = serverBuilder.build();

        // --- Client ---
        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        clientBuilder.comment("PeTaSsE_gAnG_Additions — Configuration Client")
                     .push("display");
        // Réservé pour les options client futures (effets de particules, HUD, etc.)
        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();
    }

    private ModConfig() {
        throw new UnsupportedOperationException("Classe de configuration.");
    }
}
