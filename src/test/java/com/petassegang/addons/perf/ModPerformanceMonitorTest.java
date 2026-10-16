package com.petassegang.addons.perf;

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
        assertEquals(3, hud.size(), "Trois lignes au F3.");
        assertEquals("Perf monitor : actif", hud.get(0), "Entete du F3.");
        assertTrue(hud.get(2).startsWith("client: fps=120"), "Resume client avec les FPS : " + hud.get(2));
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
}
