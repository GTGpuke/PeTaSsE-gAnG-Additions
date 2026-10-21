package com.gtgpuke.gtgsbackrooms;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.gtgpuke.gtgsbackrooms.core.ModConfig;
import com.gtgpuke.gtgsbackrooms.core.portal.debug.DebugPortalCommand;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.BackroomsPortalEntityRegistry;
import com.gtgpuke.gtgsbackrooms.core.portal.interaction.RemoteInteractionHandler;
import com.gtgpuke.gtgsbackrooms.core.portal.sync.PortalViewTracker;
import com.gtgpuke.gtgsbackrooms.core.portal.transition.DestinationLoader;
import com.gtgpuke.gtgsbackrooms.core.portal.transition.TeleportExecutor;

@DisplayName("Point d'entree du mod")
class GtgsBackroomsModTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("Portails desactives par configuration : seul le registre des portails se branche, pour retirer les orphelins")
    void portalsCanBeDisabled() {
        try {
            assertDoesNotThrow(() -> GtgsBackroomsMod.initPortals(false, true), "Sans portails, rien d'autre ne se branche.");
        } finally {
            GtgsBackroomsMod.initPortals(ModConfig.ENABLE_PORTAL_FEATURE, true);
        }
    }

    @Test
    @DisplayName("Le runtime des portails se branche une seule fois, meme rappele, avec ou sans commande de debug")
    void portalRuntimeInitIsIdempotent() {
        assertDoesNotThrow(() -> {
            GtgsBackroomsMod.initPortals(true, false);
            GtgsBackroomsMod.initPortals(ModConfig.ENABLE_PORTAL_FEATURE, true);
            BackroomsPortalEntityRegistry.init();
            DestinationLoader.init();
            PortalViewTracker.init();
            TeleportExecutor.init();
            RemoteInteractionHandler.init();
            DebugPortalCommand.register();
        }, "Un second branchement est ignore.");
    }
}
