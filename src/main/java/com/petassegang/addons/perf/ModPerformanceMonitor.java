package com.petassegang.addons.perf;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import com.sun.management.OperatingSystemMXBean;
import org.jetbrains.annotations.Nullable;

import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.core.ModConstants;

/**
 * Moniteur de performance opt-in ({@code -Dpetassegang.debugPerformanceMonitor=true}).
 *
 * <p>Publie periodiquement dans les logs le temps de tick serveur, le top des
 * sections profilees, la memoire JVM et la charge CPU, et complete le F3 du
 * Level 0. Desactive, chaque appel se reduit a un test de booleen.
 */
public final class ModPerformanceMonitor {

    private static final Scope NO_OP_SCOPE = new Scope(null, 0L);
    private static final int TOP_SECTIONS = 5;
    private static final ConcurrentHashMap<String, SectionStats> SECTION_STATS = new ConcurrentHashMap<>();
    private static final LongAdder SERVER_TICK_COUNT = new LongAdder();
    private static final LongAdder SERVER_TICK_TOTAL_NANOS = new LongAdder();
    private static final AtomicLong SERVER_TICK_MAX_NANOS = new AtomicLong();
    private static final AtomicLong LAST_SERVER_LOG_NANOS = new AtomicLong();
    private static final AtomicLong LAST_CLIENT_LOG_NANOS = new AtomicLong();
    private static final OperatingSystemMXBean OPERATING_SYSTEM_MX_BEAN = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

    private static volatile boolean enabled = ModConfig.DEBUG_PERFORMANCE_MONITOR;
    private static volatile long currentServerTickStartNanos;
    private static volatile String lastServerSummary = "serveur: aucun echantillon";
    private static volatile String lastClientSummary = "client: aucun echantillon";

    private ModPerformanceMonitor() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /** {@code true} si le monitoring est actif. */
    public static boolean isEnabled() {
        return enabled;
    }

    /** Active ou desactive le monitoring a chaud (tests, commandes de debug). */
    static void setEnabled(boolean value) {
        enabled = value;
    }

    /** Oublie la date du dernier resume : le prochain tick en publie un (tests). */
    static void resetLogTimers() {
        LAST_SERVER_LOG_NANOS.set(0L);
        LAST_CLIENT_LOG_NANOS.set(0L);
    }

    /**
     * Ouvre une section profilee ; a utiliser en try-with-resources.
     *
     * @param sectionName nom stable de la section (constante, pas de concatenation par appel)
     * @return scope auto-closeable, sans effet si le monitoring est inactif
     */
    public static Scope scope(String sectionName) {
        return isEnabled() ? new Scope(sectionName, System.nanoTime()) : NO_OP_SCOPE;
    }

    /** Debut de tick serveur. */
    public static void onServerTickStart() {
        if (isEnabled()) {
            currentServerTickStartNanos = System.nanoTime();
        }
    }

    /** Fin de tick serveur. */
    public static void onServerTickEnd() {
        if (!isEnabled() || currentServerTickStartNanos == 0L) {
            return;
        }
        long elapsedNanos = System.nanoTime() - currentServerTickStartNanos;
        currentServerTickStartNanos = 0L;
        SERVER_TICK_COUNT.increment();
        SERVER_TICK_TOTAL_NANOS.add(elapsedNanos);
        updateMax(SERVER_TICK_MAX_NANOS, elapsedNanos);
        if (shouldLog(LAST_SERVER_LOG_NANOS)) {
            logServerSummary();
        }
    }

    /**
     * Echantillonne l'etat client en fin de tick.
     *
     * @param fps FPS courant
     */
    public static void onClientTick(int fps) {
        if (!isEnabled() || !shouldLog(LAST_CLIENT_LOG_NANOS)) {
            return;
        }
        Snapshot snapshot = Snapshot.capture(OPERATING_SYSTEM_MX_BEAN);
        lastClientSummary = String.format(Locale.ROOT, "client: fps=%d %s", fps, snapshot.format());
        ModConstants.LOGGER.info("[perf] {}", lastClientSummary);
    }

    /**
     * Ajoute une synthese courte au F3.
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

    /** Dernier resume serveur publie. */
    public static String lastServerSummary() {
        return lastServerSummary;
    }

    private static boolean shouldLog(AtomicLong lastLogNanos) {
        long now = System.nanoTime();
        long last = lastLogNanos.get();
        if (last != 0L && now - last < intervalNanos()) {
            return false;
        }
        return lastLogNanos.compareAndSet(last, now);
    }

    private static void logServerSummary() {
        long tickCount = SERVER_TICK_COUNT.sumThenReset();
        long totalTickNanos = SERVER_TICK_TOTAL_NANOS.sumThenReset();
        long maxTickNanos = SERVER_TICK_MAX_NANOS.getAndSet(0L);
        // Toujours au moins le tick qui vient d'etre compte.
        double averageMspt = nanosToMillis(totalTickNanos) / tickCount;
        lastServerSummary = String.format(
                Locale.ROOT,
                "serveur: mspt=%.3f/%.3f %s top=%s",
                averageMspt,
                nanosToMillis(maxTickNanos),
                Snapshot.capture(OPERATING_SYSTEM_MX_BEAN).format(),
                formatTopSections());
        ModConstants.LOGGER.info("[perf] {}", lastServerSummary);
    }

    private static String formatTopSections() {
        List<SectionSnapshot> snapshots = new ArrayList<>();
        for (Map.Entry<String, SectionStats> entry : SECTION_STATS.entrySet()) {
            SectionSnapshot snapshot = entry.getValue().snapshotAndReset(entry.getKey());
            if (snapshot.calls() > 0L) {
                snapshots.add(snapshot);
            }
        }
        if (snapshots.isEmpty()) {
            return "aucune section";
        }
        snapshots.sort(Comparator.comparingLong(SectionSnapshot::totalNanos).reversed());
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < Math.min(TOP_SECTIONS, snapshots.size()); index++) {
            SectionSnapshot snapshot = snapshots.get(index);
            if (index > 0) {
                builder.append(" | ");
            }
            builder.append(snapshot.name())
                    .append(':').append(snapshot.calls()).append("x/")
                    .append(String.format(Locale.ROOT, "%.3f", nanosToMillis(snapshot.totalNanos()))).append("ms/max ")
                    .append(String.format(Locale.ROOT, "%.3f", nanosToMillis(snapshot.maxNanos()))).append("ms");
        }
        return builder.toString();
    }

    private static long intervalNanos() {
        return Math.max(1, ModConfig.PERFORMANCE_LOG_INTERVAL_SECONDS) * 1_000_000_000L;
    }

    private static void updateMax(AtomicLong max, long candidate) {
        max.accumulateAndGet(candidate, Math::max);
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0D;
    }

    /** Scope de mesure auto-closeable. */
    public static final class Scope implements AutoCloseable {

        private final String sectionName;
        private final long startNanos;

        private Scope(String sectionName, long startNanos) {
            this.sectionName = sectionName;
            this.startNanos = startNanos;
        }

        @Override
        public void close() {
            if (sectionName != null) {
                SECTION_STATS.computeIfAbsent(sectionName, ignored -> new SectionStats())
                        .record(System.nanoTime() - startNanos);
            }
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
            return new SectionSnapshot(name, calls.sumThenReset(), totalNanos.sumThenReset(), maxNanos.getAndSet(0L));
        }
    }

    private record SectionSnapshot(String name, long calls, long totalNanos, long maxNanos) {
    }

    /** Memoire et charge CPU a un instant ; {@code bean} absent (JVM sans extension com.sun) : charge inconnue. */
    record Snapshot(long heapUsedBytes, long heapMaxBytes, double processCpuLoad, double systemCpuLoad) {

        static Snapshot capture(@Nullable OperatingSystemMXBean bean) {
            Runtime runtime = Runtime.getRuntime();
            return new Snapshot(
                    runtime.totalMemory() - runtime.freeMemory(),
                    runtime.maxMemory(),
                    bean != null ? bean.getProcessCpuLoad() : -1.0D,
                    bean != null ? bean.getCpuLoad() : -1.0D);
        }

        String format() {
            return String.format(
                    Locale.ROOT,
                    "heap=%.1f/%.1f MiB cpuProc=%s cpuSys=%s",
                    heapUsedBytes / (1024.0D * 1024.0D),
                    heapMaxBytes / (1024.0D * 1024.0D),
                    formatPercent(processCpuLoad),
                    formatPercent(systemCpuLoad));
        }

        static String formatPercent(double load) {
            return load < 0.0D ? "n/a" : String.format(Locale.ROOT, "%.1f%%", load * 100.0D);
        }
    }
}
