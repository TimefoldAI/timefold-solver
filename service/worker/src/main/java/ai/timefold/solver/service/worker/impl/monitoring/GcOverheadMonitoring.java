package ai.timefold.solver.service.worker.impl.monitoring;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.service.definition.internal.platform.EnvironmentVars;
import ai.timefold.solver.service.worker.impl.SolverWorker;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.HotSpotDiagnosticMXBean.ThreadDumpFormat;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;

/**
 * Fails the run when the JVM spends too much time in garbage collection.
 * <p>
 * When the memory watchdog is enabled ({@code ai.timefold.solver.monitoring.memory.enabled}, set by an execution profile), it
 * also checks the process memory against the container memory limit at a short interval. The first time it reaches the
 * threshold, it writes a thread dump and a heap dump into the execution-profile artifacts directory and fails the run, which
 * uploads the artifacts. This happens before the container would be {@code OOMKilled}, so there is still room to write the
 * dumps.
 */
@ApplicationScoped
@Unremovable
public class GcOverheadMonitoring {

    public static final int JVM_MONITORING_EXIST_STATUS_CODE = 111;
    private static final Logger LOGGER = LoggerFactory.getLogger(GcOverheadMonitoring.class);

    private final int delaySeconds;
    private final SolverWorker solverWorker;
    private final List<GarbageCollectorMXBean> gcBeans;
    private final int gcThresholdPct;
    private final boolean memoryWatchdogEnabled;
    private final int memoryThresholdPct;
    private final int memoryIntervalMillis;
    private final AtomicBoolean memoryWatchdogTriggered = new AtomicBoolean(false);

    private long prevGcTime = 0;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "timefold-gc-monitor");
        thread.setPriority(Thread.MAX_PRIORITY);
        return thread;
    });

    @Inject
    public GcOverheadMonitoring(SolverWorker solverWorker,
            @ConfigProperty(name = "ai.timefold.solver.monitoring.gc.thresholdPct",
                    defaultValue = "100") Integer gcThresholdPct,
            @ConfigProperty(name = "ai.timefold.solver.monitoring.gc.delaySeconds", defaultValue = "10") Integer delaySeconds,
            @ConfigProperty(name = "ai.timefold.solver.monitoring.memory.enabled",
                    defaultValue = "false") Boolean memoryWatchdogEnabled,
            @ConfigProperty(name = "ai.timefold.solver.monitoring.memory.thresholdPct",
                    defaultValue = "90") Integer memoryThresholdPct,
            @ConfigProperty(name = "ai.timefold.solver.monitoring.memory.intervalMillis",
                    defaultValue = "1000") Integer memoryIntervalMillis) {
        this.solverWorker = solverWorker;
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.prevGcTime = getTotalGcTime();
        this.gcThresholdPct = gcThresholdPct;
        this.delaySeconds = delaySeconds;
        this.memoryWatchdogEnabled = memoryWatchdogEnabled;
        this.memoryThresholdPct = memoryThresholdPct;
        this.memoryIntervalMillis = memoryIntervalMillis;
    }

    public void onStart(@Observes StartupEvent event) {
        String id = System.getenv(EnvironmentVars.ENV_TIMEFOLD_JOB_ID);

        if (System.getenv(EnvironmentVars.ENV_TIMEFOLD_JOB_ID) != null) {

            executor.scheduleAtFixedRate(() -> {

                long currGcTime = getTotalGcTime();
                long gcDelta = currGcTime - prevGcTime;
                double intervalPct = 100.0 * gcDelta / (delaySeconds * 1000);

                LOGGER.debug("GC overhead: interval= {}", intervalPct);
                if (intervalPct > gcThresholdPct) {
                    LOGGER.error("GC overhead exceeded {}% - shutting down due to high risk of becoming unresponsive",
                            gcThresholdPct);
                    solverWorker.notifyOnFailure(id, new TimefoldRuntimeException(ErrorCodes.SOLVER_UNKNOWN,
                            "Not enough memory available to solve dataset - configure memory via configuration profile",
                            new OutOfMemoryError(
                                    "GC overhead exceeded " + gcThresholdPct
                                            + "% - shutting down due to high risk of becoming unresponsive"),
                            false));
                    System.exit(JVM_MONITORING_EXIST_STATUS_CODE);
                    return;
                }

                prevGcTime = currGcTime;
            }, delaySeconds, delaySeconds, TimeUnit.SECONDS);

            if (memoryWatchdogEnabled) {
                LOGGER.info("Memory watchdog enabled: dumping and failing the run at {}% memory usage", memoryThresholdPct);
                executor.scheduleAtFixedRate(() -> {
                    try {
                        checkMemory(id);
                    } catch (Throwable e) {
                        // An exception would cancel the scheduled task, so keep the watchdog alive.
                        LOGGER.warn("Memory watchdog check failed due to {}", e.getMessage());
                    }
                }, memoryIntervalMillis, memoryIntervalMillis, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void checkMemory(String id) {
        OptionalLong rss = MemoryPressure.readRssBytes();
        OptionalLong containerLimit = MemoryPressure.containerMemoryLimitBytes();

        String reason = MemoryPressure.exceededReason(rss, containerLimit, memoryThresholdPct);
        if (reason == null || !memoryWatchdogTriggered.compareAndSet(false, true)) {
            return;
        }
        LOGGER.error("Memory usage exceeded {}% ({}) - writing thread and heap dumps and shutting down",
                memoryThresholdPct, reason);
        Path dumpDir = createDumpDirectory();
        if (dumpDir != null) {
            writeDumps(dumpDir);
        }
        solverWorker.notifyOnFailure(id, new TimefoldRuntimeException(ErrorCodes.SOLVER_UNKNOWN,
                "Not enough memory available to solve dataset - configure memory via configuration profile",
                new OutOfMemoryError("Memory usage exceeded " + memoryThresholdPct + "% (" + reason + ")"),
                false));
        System.exit(JVM_MONITORING_EXIST_STATUS_CODE);
    }

    private static Path createDumpDirectory() {
        String dir = System.getenv(EnvironmentVars.ENV_TIMEFOLD_EXECUTION_PROFILE_DIR);
        Path dumpDir = Path.of(dir != null && !dir.isBlank() ? dir : EnvironmentVars.DEFAULT_EXECUTION_PROFILE_DIR)
                .toAbsolutePath();
        try {
            return Files.createDirectories(dumpDir);
        } catch (Exception e) {
            LOGGER.warn("Unable to create dump directory {} due to {}", dumpDir, e.getMessage());
            return null;
        }
    }

    private static void writeDumps(Path dumpDir) {
        long timestamp = System.currentTimeMillis();
        HotSpotDiagnosticMXBean diagnostics = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
        // Thread dump first: it is cheap and captures the threads before the heap dump pauses the JVM.
        Path threadDump = dumpDir.resolve("thread-dump-" + timestamp + ".txt");
        try {
            diagnostics.dumpThreads(threadDump.toString(), ThreadDumpFormat.TEXT_PLAIN);
            LOGGER.info("Thread dump written to {}", threadDump);
        } catch (Exception e) {
            LOGGER.warn("Unable to write thread dump to {} due to {}", threadDump, e.getMessage());
        }
        Path heapDump = dumpDir.resolve("heap-dump-" + timestamp + ".hprof");
        try {
            diagnostics.dumpHeap(heapDump.toString(), true);
            LOGGER.info("Heap dump written to {}", heapDump);
        } catch (Exception e) {
            LOGGER.warn("Unable to write heap dump to {} due to {}", heapDump, e.getMessage());
        }
    }

    private long getTotalGcTime() {
        long total = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            long time = gc.getCollectionTime();
            if (time >= 0) { // -1 means undefined
                total += time;
            }
        }
        return total;
    }

    @PreDestroy
    public void shutdown() {
        this.executor.shutdownNow();
    }
}
