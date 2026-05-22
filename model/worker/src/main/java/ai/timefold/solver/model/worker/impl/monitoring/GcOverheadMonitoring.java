package ai.timefold.solver.model.worker.impl.monitoring;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import ai.timefold.solver.model.definition.internal.error.ErrorCodes;
import ai.timefold.solver.model.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.model.definition.internal.platform.EnvironmentVars;
import ai.timefold.solver.model.worker.impl.SolverWorker;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
@Unremovable
public class GcOverheadMonitoring {

    public static final int JVM_MONITORING_EXIST_STATUS_CODE = 111;
    private static final Logger LOGGER = LoggerFactory.getLogger(GcOverheadMonitoring.class);

    private final int delaySeconds;
    private final SolverWorker solverWorker;
    private final List<GarbageCollectorMXBean> gcBeans;
    private final int gcThresholdPct;

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
            @ConfigProperty(name = "ai.timefold.solver.monitoring.gc.delaySeconds", defaultValue = "10") Integer delaySeconds) {
        this.solverWorker = solverWorker;
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.prevGcTime = getTotalGcTime();
        this.gcThresholdPct = gcThresholdPct;
        this.delaySeconds = delaySeconds;
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
