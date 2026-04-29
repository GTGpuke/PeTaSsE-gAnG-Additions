package com.petassegang.addons.perf.section;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.core.ModConstants;
import com.sun.management.OperatingSystemMXBean;

/**
 * Moniteur de performance opt-in pour le mod.
 *
 * <p>Ce moniteur donne un premier niveau d'inspection directement en jeu :
 * temps serveur, top des sections profilees, memoire JVM et charge CPU.
 *
 * <p>Le GPU n'est pas exposable de facon fiable via l'API Java/Fabric seule.
 * Le monitoring client suit donc les FPS comme proxy simple de la sante de
 * rendu, en plus de l'etat CPU/RAM du processus.
 */
public final class ModPerformanceMonitor {

    private static final Scope NO_OP_SCOPE = new Scope(null, 0L);
    private static final ConcurrentHashMap<String, SectionStats> SECTION_STATS = new ConcurrentHashMap<>();
    private static final LongAdder SERVER_TICK_COUNT = new LongAdder();
    private static final LongAdder SERVER_TICK_TOTAL_NANOS = new LongAdder();
    private static final AtomicLong SERVER_TICK_MAX_NANOS = new AtomicLong();
    private static final AtomicLong LAST_SERVER_LOG_NANOS = new AtomicLong();
    private static final AtomicLong LAST_CLIENT_LOG_NANOS = new AtomicLong();
    private static final OperatingSystemMXBean OPERATING_SYSTEM_MX_BEAN =
            ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean bean ? bean : null;

    private static volatile long currentServerTickStartNanos;
    private static volatile String lastServerSummary = "serveur: aucun echantillon";
    private static volatile String lastClientSummary = "client: aucun echantillon";

    private ModPerformanceMonitor() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Indique si le monitoring est actif.
     */
    public static boolean isEnabled() {
        return ModConfig.DEBUG_PERFORMANCE_MONITOR;
    }

    /**
     * Ouvre une section profilee.
     *
     * @param sectionName nom stable de la section
     * @return scope auto-closeable
     */
    public static Scope scope(String sectionName) {
        if (!isEnabled()) {
            return NO_OP_SCOPE;
        }
        return new Scope(sectionName, System.nanoTime());
    }

    /**
     * Debut de tick serveur.
     */
    public static void onServerTickStart() {
        if (!isEnabled()) {
            return;
        }
        currentServerTickStartNanos = System.nanoTime();
    }

    /**
     * Fin de tick serveur.
     */
    public static void onServerTickEnd() {
        if (!isEnabled() || currentServerTickStartNanos == 0L) {
            return;
        }
        long elapsedNanos = System.nanoTime() - currentServerTickStartNanos;
        currentServerTickStartNanos = 0L;
        SERVER_TICK_COUNT.increment();
        SERVER_TICK_TOTAL_NANOS.add(elapsedNanos);
        updateMax(SERVER_TICK_MAX_NANOS, elapsedNanos);
        maybeLogServerSummary();
    }

    /**
     * Echantillonne l'etat client courant.
     *
     * @param fps FPS courant
     */
    public static void onClientTick(int fps) {
        if (!isEnabled()) {
            return;
        }
        long now = System.nanoTime();
        long last = LAST_CLIENT_LOG_NANOS.get();
        if (last != 0L && now - last < intervalNanos()) {
            return;
        }
        if (!LAST_CLIENT_LOG_NANOS.compareAndSet(last, now)) {
            return;
        }

        Snapshot snapshot = snapshot();
        lastClientSummary = String.format(
                Locale.ROOT,
                "client: fps=%d heap=%s cpuProc=%s cpuSys=%s",
                fps,
                formatMebiBytes(snapshot.heapUsedBytes) + "/" + formatMebiBytes(snapshot.heapMaxBytes),
                formatPercent(snapshot.processCpuLoad),
                formatPercent(snapshot.systemCpuLoad));
        ModConstants.LOGGER.info("[perf] {}", lastClientSummary);
    }

    /**
     * Ajoute une synthese courte au F3/debug HUD.
     *
     * @param text lignes a completer
     */
    public static void appendDebugHudText(List<String> text) {
        if (!isEnabled()) {
            return;
        }
        text.add("Perf monitor : actif");
        text.add(lastServerSummary);
        text.add(lastClientSummary);
    }

    private static void recordSection(String sectionName, long elapsedNanos) {
        SECTION_STATS.computeIfAbsent(sectionName, ignored -> new SectionStats()).record(elapsedNanos);
    }

    private static void maybeLogServerSummary() {
        long now = System.nanoTime();
        long last = LAST_SERVER_LOG_NANOS.get();
        if (last != 0L && now - last < intervalNanos()) {
            return;
        }
        if (!LAST_SERVER_LOG_NANOS.compareAndSet(last, now)) {
            return;
        }

        long tickCount = SERVER_TICK_COUNT.sumThenReset();
        long totalTickNanos = SERVER_TICK_TOTAL_NANOS.sumThenReset();
        long maxTickNanos = SERVER_TICK_MAX_NANOS.getAndSet(0L);
        double averageMspt = tickCount > 0L ? nanosToMillis(totalTickNanos) / tickCount : 0.0D;
        double maxMspt = nanosToMillis(maxTickNanos);
        Snapshot snapshot = snapshot();
        String topSections = formatTopSections();

        lastServerSummary = String.format(
                Locale.ROOT,
                "serveur: mspt=%.3f/%.3f heap=%s cpuProc=%s cpuSys=%s top=%s",
                averageMspt,
                maxMspt,
                formatMebiBytes(snapshot.heapUsedBytes) + "/" + formatMebiBytes(snapshot.heapMaxBytes),
                formatPercent(snapshot.processCpuLoad),
                formatPercent(snapshot.systemCpuLoad),
                topSections);
        ModConstants.LOGGER.info("[perf] {}", lastServerSummary);
    }

    private static String formatTopSections() {
        List<SectionSnapshot> snapshots = new ArrayList<>();
        for (Map.Entry<String, SectionStats> entry : SECTION_STATS.entrySet()) {
            SectionSnapshot snapshot = entry.getValue().snapshotAndReset(entry.getKey());
            if (snapshot.calls > 0L) {
                snapshots.add(snapshot);
            }
        }
        snapshots.sort(Comparator.comparingLong(SectionSnapshot::totalNanos).reversed());
        if (snapshots.isEmpty()) {
            return "aucune section";
        }

        int limit = Math.min(5, snapshots.size());
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < limit; index++) {
            if (index > 0) {
                builder.append(" | ");
            }
            SectionSnapshot snapshot = snapshots.get(index);
            builder.append(snapshot.name())
                    .append(":")
                    .append(snapshot.calls())
                    .append("x/")
                    .append(formatMillis(snapshot.totalNanos()))
                    .append("ms")
                    .append("/max ")
                    .append(formatMillis(snapshot.maxNanos()))
                    .append("ms");
        }
        return builder.toString();
    }

    private static Snapshot snapshot() {
        Runtime runtime = Runtime.getRuntime();
        long heapMaxBytes = runtime.maxMemory();
        long heapUsedBytes = runtime.totalMemory() - runtime.freeMemory();
        double processCpuLoad = OPERATING_SYSTEM_MX_BEAN != null ? OPERATING_SYSTEM_MX_BEAN.getProcessCpuLoad() : -1.0D;
        double systemCpuLoad = OPERATING_SYSTEM_MX_BEAN != null ? OPERATING_SYSTEM_MX_BEAN.getCpuLoad() : -1.0D;
        return new Snapshot(heapUsedBytes, heapMaxBytes, processCpuLoad, systemCpuLoad);
    }

    private static long intervalNanos() {
        return Math.max(1, ModConfig.PERFORMANCE_LOG_INTERVAL_SECONDS) * 1_000_000_000L;
    }

    private static void updateMax(AtomicLong max, long candidate) {
        long current = max.get();
        while (candidate > current && !max.compareAndSet(current, candidate)) {
            current = max.get();
        }
    }

    private static String formatMebiBytes(long bytes) {
        return String.format(Locale.ROOT, "%.1f MiB", bytes / (1024.0D * 1024.0D));
    }

    private static String formatPercent(double load) {
        if (load < 0.0D) {
            return "n/a";
        }
        return String.format(Locale.ROOT, "%.1f%%", load * 100.0D);
    }

    private static String formatMillis(long nanos) {
        return String.format(Locale.ROOT, "%.3f", nanosToMillis(nanos));
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0D;
    }

    /**
     * Scope auto-closeable de mesure.
     */
    public static final class Scope implements AutoCloseable {

        private final String sectionName;
        private final long startNanos;

        private Scope(String sectionName, long startNanos) {
            this.sectionName = sectionName;
            this.startNanos = startNanos;
        }

        @Override
        public void close() {
            if (sectionName == null) {
                return;
            }
            recordSection(sectionName, System.nanoTime() - startNanos);
        }
    }

    private static final class SectionStats {

        private final LongAdder calls = new LongAdder();
        private final LongAdder totalNanos = new LongAdder();
        private final AtomicLong maxNanos = new AtomicLong();

        private void record(long elapsedNanos) {
            calls.increment();
            totalNanos.add(elapsedNanos);
            updateMax(maxNanos, elapsedNanos);
        }

        private SectionSnapshot snapshotAndReset(String name) {
            return new SectionSnapshot(
                    name,
                    calls.sumThenReset(),
                    totalNanos.sumThenReset(),
                    maxNanos.getAndSet(0L));
        }
    }

    private record SectionSnapshot(String name, long calls, long totalNanos, long maxNanos) {
    }

    private record Snapshot(long heapUsedBytes, long heapMaxBytes, double processCpuLoad, double systemCpuLoad) {
    }
}
