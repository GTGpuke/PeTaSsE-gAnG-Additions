package com.gtgpuke.gtgsbackrooms.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.util.Identifier;

@DisplayName("Moniteur de performance")
class ModPerformanceMonitorTest {

    @AfterEach
    void disable() {
        ModPerformanceMonitor.setEnabled(false);
    }

    @Test
    @DisplayName("Desactive par defaut, le moniteur ne fait rien et n'alloue rien")
    void disabledIsNoOp() {
        assertFalse(ModPerformanceMonitor.isEnabled(), "Inactif par defaut.");
        ModPerformanceMonitor.Scope first = ModPerformanceMonitor.scope("test.section");
        ModPerformanceMonitor.Scope second = ModPerformanceMonitor.scope("other.section");
        assertSame(first, second, "Le scope inactif est une instance partagee.");
        first.close();

        ModPerformanceMonitor.onServerTickStart();
        ModPerformanceMonitor.onServerTickEnd();
        ModPerformanceMonitor.onClientTick(60);
        List<String> hud = new ArrayList<>();
        ModPerformanceMonitor.appendDebugHudText(hud);
        assertTrue(hud.isEmpty(), "Rien n'est ajoute au F3 quand le moniteur est inactif.");
    }

    @Test
    @DisplayName("Active, le moniteur mesure les sections et publie des resumes serveur et client")
    void enabledRecordsAndSummarizes() {
        ModPerformanceMonitor.setEnabled(true);
        assertTrue(ModPerformanceMonitor.isEnabled(), "Actif a chaud.");

        ModPerformanceMonitor.Scope scope = ModPerformanceMonitor.scope("test.generation");
        assertNotSame(ModPerformanceMonitor.scope("test.other"), scope, "Chaque scope actif est distinct.");
        scope.close();

        ModPerformanceMonitor.onServerTickStart();
        ModPerformanceMonitor.onServerTickEnd();
        ModPerformanceMonitor.onClientTick(120);

        String server = ModPerformanceMonitor.lastServerSummary();
        assertTrue(server.startsWith("serveur: mspt="), "Resume serveur publie : " + server);
        assertTrue(server.contains("test.generation"), "La section mesuree apparait dans le top : " + server);
        assertTrue(server.contains("heap="), "La memoire est rapportee.");

        List<String> hud = new ArrayList<>();
        ModPerformanceMonitor.appendDebugHudText(hud);
        assertEquals(4, hud.size(), "Quatre lignes au F3.");
        assertEquals("Perf monitor : actif", hud.get(0), "Entete du F3.");
        assertTrue(hud.get(2).startsWith("client: fps=120"), "Resume client avec les FPS : " + hud.get(2));
    }

    @Test
    @DisplayName("Un resume est du la premiere fois, puis une fois l'intervalle ecoule")
    void summaryDueDates() {
        assertTrue(ModPerformanceMonitor.isDue(0L, 5L, 100L), "Jamais publie : du.");
        assertFalse(ModPerformanceMonitor.isDue(100L, 199L, 100L), "Dans l'intervalle : pas encore.");
        assertTrue(ModPerformanceMonitor.isDue(100L, 200L, 100L), "Intervalle ecoule : du.");
    }

    @Test
    @DisplayName("Un second resume dans l'intervalle est ignore")
    void summariesAreRateLimited() {
        ModPerformanceMonitor.setEnabled(true);
        ModPerformanceMonitor.onServerTickStart();
        ModPerformanceMonitor.onServerTickEnd();
        String first = ModPerformanceMonitor.lastServerSummary();
        ModPerformanceMonitor.scope("test.late").close();
        ModPerformanceMonitor.onServerTickStart();
        ModPerformanceMonitor.onServerTickEnd();
        assertEquals(first, ModPerformanceMonitor.lastServerSummary(), "Pas de nouveau resume avant l'intervalle.");
        assertFalse(first.contains("test.late"), "La section tardive attend le prochain resume.");
    }

    @Test
    @DisplayName("Sans section mesuree le resume le dit ; a plusieurs, elles sont classees et separees")
    void topSectionsFormatting() {
        ModPerformanceMonitor.setEnabled(true);
        ModPerformanceMonitor.resetLogTimers();
        ModPerformanceMonitor.onServerTickStart();
        ModPerformanceMonitor.onServerTickEnd();
        ModPerformanceMonitor.resetLogTimers();
        ModPerformanceMonitor.onServerTickStart();
        ModPerformanceMonitor.onServerTickEnd();
        assertTrue(ModPerformanceMonitor.lastServerSummary().endsWith("top=aucune section"),
                "Les sections ont ete videes par le resume precedent : " + ModPerformanceMonitor.lastServerSummary());
        ModPerformanceMonitor.scope("test.alpha").close();
        ModPerformanceMonitor.scope("test.beta").close();
        ModPerformanceMonitor.scope("test.beta").close();
        ModPerformanceMonitor.resetLogTimers();
        ModPerformanceMonitor.onServerTickStart();
        ModPerformanceMonitor.onServerTickEnd();
        String summary = ModPerformanceMonitor.lastServerSummary();
        assertTrue(summary.contains("test.alpha:1x/") && summary.contains("test.beta:2x/"), "Les deux sections et leurs appels : " + summary);
        assertTrue(summary.contains(" | "), "Les sections sont separees : " + summary);
    }

    @Test
    @DisplayName("Sans extension JVM pour la charge CPU, l'instantane l'indique")
    void snapshotWithoutOperatingSystemBean() {
        ModPerformanceMonitor.Snapshot snapshot = ModPerformanceMonitor.Snapshot.capture(null);
        assertTrue(snapshot.heapMaxBytes() >= snapshot.heapUsedBytes(), "La memoire reste lue.");
        assertTrue(snapshot.format().endsWith("cpuProc=n/a cpuSys=n/a"), "Charge CPU inconnue : " + snapshot.format());
        assertEquals("n/a", ModPerformanceMonitor.Snapshot.formatPercent(-1.0D), "Charge negative : inconnue.");
        assertEquals("50.0%", ModPerformanceMonitor.Snapshot.formatPercent(0.5D), "Charge connue en pourcentage.");
    }

    @Test
    @DisplayName("Une fin de tick sans debut est ignoree, et un second echantillon client dans l'intervalle aussi")
    void endWithoutStartAndRateLimitedClient() {
        ModPerformanceMonitor.setEnabled(true);
        ModPerformanceMonitor.resetLogTimers();
        ModPerformanceMonitor.onServerTickStart();
        ModPerformanceMonitor.onServerTickEnd();
        String summary = ModPerformanceMonitor.lastServerSummary();
        ModPerformanceMonitor.resetLogTimers();
        ModPerformanceMonitor.onServerTickEnd();
        assertEquals(summary, ModPerformanceMonitor.lastServerSummary(), "Sans debut de tick, rien n'est mesure ni publie.");
        ModPerformanceMonitor.onClientTick(30);
        ModPerformanceMonitor.onClientTick(31);
        List<String> hud = new ArrayList<>();
        ModPerformanceMonitor.appendDebugHudText(hud);
        assertTrue(hud.get(2).startsWith("client: fps=30"), "Le second echantillon client, dans l'intervalle, est ignore : " + hud.get(2));
    }

    @Test
    @DisplayName("Une traversee mesure la bascule puis les images qui suivent, et compte les chunks recus")
    void crossingMeasurement() throws InterruptedException {
        ModPerformanceMonitor.setEnabled(true);
        String before = ModPerformanceMonitor.lastCrossingSummary();
        ModPerformanceMonitor.onFrameRendered();
        ModPerformanceMonitor.onChunkReceived();
        ModPerformanceMonitor.onCrossingSwitched(Identifier.ofVanilla("the_nether"));
        assertEquals(before, ModPerformanceMonitor.lastCrossingSummary(), "Sans debut de mesure, une bascule est ignoree.");
        ModPerformanceMonitor.onCrossingStart();
        ModPerformanceMonitor.onCrossingSwitched(Identifier.ofVanilla("the_nether"));
        ModPerformanceMonitor.onChunkReceived();
        ModPerformanceMonitor.onChunkReceived();
        ModPerformanceMonitor.onFrameRendered();
        Thread.sleep(60L);
        ModPerformanceMonitor.onFrameRendered();
        for (int frame = 2; frame < ModPerformanceMonitor.CROSSING_FRAMES; frame++) {
            ModPerformanceMonitor.onFrameRendered();
        }
        String summary = ModPerformanceMonitor.lastCrossingSummary();
        assertTrue(summary.startsWith("traversee: vers minecraft:the_nether bascule="), "Bilan publie : " + summary + ".");
        // Au moins l'image de soixante millisecondes : une pause de la JVM pendant la boucle en ajouterait d'autres.
        assertTrue(summary.contains("/" + (ModPerformanceMonitor.CROSSING_FRAMES - 1)) && !summary.contains("lentes=0/"),
                "L'image de soixante millisecondes est lente : " + summary + ".");
        assertTrue(summary.endsWith("chunks=2"), "Les chunks de la fenetre sont comptes : " + summary + ".");
        List<String> hud = new ArrayList<>();
        ModPerformanceMonitor.appendDebugHudText(hud);
        assertEquals(summary, hud.get(3), "Le bilan de traversee est au F3.");
    }

    @Test
    @DisplayName("Inactif, le moniteur ignore les traversees ; coupe en pleine fenetre, il l'abandonne")
    void crossingIgnoredWhenDisabled() {
        String before = ModPerformanceMonitor.lastCrossingSummary();
        ModPerformanceMonitor.onCrossingStart();
        ModPerformanceMonitor.onCrossingSwitched(Identifier.ofVanilla("the_end"));
        ModPerformanceMonitor.onFrameRendered();
        ModPerformanceMonitor.onChunkReceived();
        assertEquals(before, ModPerformanceMonitor.lastCrossingSummary(), "Inactif : rien n'est mesure.");
        ModPerformanceMonitor.setEnabled(true);
        ModPerformanceMonitor.onCrossingStart();
        ModPerformanceMonitor.onCrossingSwitched(Identifier.ofVanilla("the_end"));
        ModPerformanceMonitor.onFrameRendered();
        ModPerformanceMonitor.setEnabled(false);
        for (int frame = 0; frame <= ModPerformanceMonitor.CROSSING_FRAMES; frame++) {
            ModPerformanceMonitor.onFrameRendered();
        }
        assertEquals(before, ModPerformanceMonitor.lastCrossingSummary(), "Coupe en pleine fenetre, la mesure est abandonnee sans bilan.");
    }
}
