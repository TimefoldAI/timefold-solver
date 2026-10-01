package ai.timefold.solver.service.definition.impl.executionprofile;

import java.util.Map;

import ai.timefold.solver.service.definition.internal.executionprofile.ExecutionProfile;

/**
 * Writes a thread dump and a heap dump when the solver runs short of memory, before the container is {@code OOMKilled}.
 * <p>
 * The profile enables the solver worker's memory watchdog, which checks the process memory against the container limit every
 * second. The first time it reaches the threshold, the watchdog writes both dumps into the execution-profile artifacts
 * directory and fails the run, which uploads them. A memory spike that kills the container between two checks is not
 * captured.
 * <p>
 * The profile deliberately leaves the heap size alone, since lowering it could cause out-of-memory errors that would not
 * otherwise occur. Instead, the watchdog fails the run early, leaving room below the container limit to write the dumps.
 */
public final class ThreadDumpOnOomExecutionProfile implements ExecutionProfile {

    /** Environment-variable form of {@code ai.timefold.solver.monitoring.memory.enabled}. */
    static final String ENV_MEMORY_WATCHDOG_ENABLED = "AI_TIMEFOLD_SOLVER_MONITORING_MEMORY_ENABLED";

    @Override
    public String id() {
        return "thread-dump-on-oom";
    }

    @Override
    public String name() {
        return "Thread and heap dump on high memory";
    }

    @Override
    public String description() {
        return "Writes a thread dump and a heap dump to the run's execution artifacts and fails the run when memory usage "
                + "gets close to the limit, before the container is killed.";
    }

    @Override
    public Map<String, String> toEnvironment(Map<String, String> options) {
        return Map.of(ENV_MEMORY_WATCHDOG_ENABLED, "true");
    }
}
