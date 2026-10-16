package com.petassegang.addons;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;

/**
 * Initialise Minecraft et enregistre le contenu du mod une seule fois pour toute la JVM de test.
 *
 * <p>A appeler dans un {@code @BeforeAll} de chaque test qui touche des blocs,
 * items, registres ou block states. Les tests qui n'utilisent que de la logique
 * pure (coordonnees, hash, layout) n'en ont pas besoin.
 */
public final class MinecraftTestBootstrap {

    private static boolean initialized;

    private MinecraftTestBootstrap() {
    }

    /** Demarre Minecraft et enregistre le contenu du mod ; idempotent. */
    public static synchronized void ensure() {
        if (initialized) {
            return;
        }
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        // Le point d'entree reel : registres, hooks serveur et flag portail.
        new PeTaSsEgAnGAdditionsMod().onInitialize();
        initialized = true;
    }
}
