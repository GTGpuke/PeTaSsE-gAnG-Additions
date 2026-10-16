package com.petassegang.addons.backrooms.portal.interaction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.petassegang.addons.MinecraftTestBootstrap;

@DisplayName("Suivi du minage a travers un portail")
class RemoteMiningTrackerTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    @DisplayName("Le suivi se branche une seule fois, meme rappele")
    void initIsIdempotent() {
        assertDoesNotThrow(() -> {
            RemoteMiningTracker.init();
            RemoteMiningTracker.init();
        }, "Un second branchement est ignore.");
    }
}
