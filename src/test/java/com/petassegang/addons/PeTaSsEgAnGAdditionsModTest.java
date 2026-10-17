package com.petassegang.addons;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.backrooms.portal.debug.DebugPortalCommand;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntityRegistry;
import com.petassegang.addons.backrooms.portal.interaction.RemoteInteractionHandler;
import com.petassegang.addons.backrooms.portal.sync.PortalViewTracker;
import com.petassegang.addons.backrooms.portal.transition.DestinationLoader;
import com.petassegang.addons.backrooms.portal.transition.TeleportExecutor;
import com.petassegang.addons.config.ModConfig;

@DisplayName("Point d'entree du mod")
class PeTaSsEgAnGAdditionsModTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("Portails desactives par configuration : seul le registre des portails se branche, pour retirer les orphelins")
    void portalsCanBeDisabled() {
        try {
            assertDoesNotThrow(() -> PeTaSsEgAnGAdditionsMod.initPortals(false, true), "Sans portails, rien d'autre ne se branche.");
        } finally {
            PeTaSsEgAnGAdditionsMod.initPortals(ModConfig.ENABLE_PORTAL_FEATURE, true);
        }
    }

    @Test
    @DisplayName("Le runtime des portails se branche une seule fois, meme rappele, avec ou sans commande de debug")
    void portalRuntimeInitIsIdempotent() {
        assertDoesNotThrow(() -> {
            PeTaSsEgAnGAdditionsMod.initPortals(true, false);
            PeTaSsEgAnGAdditionsMod.initPortals(ModConfig.ENABLE_PORTAL_FEATURE, true);
            BackroomsPortalEntityRegistry.init();
            DestinationLoader.init();
            PortalViewTracker.init();
            TeleportExecutor.init();
            RemoteInteractionHandler.init();
            DebugPortalCommand.register();
        }, "Un second branchement est ignore.");
    }
}
