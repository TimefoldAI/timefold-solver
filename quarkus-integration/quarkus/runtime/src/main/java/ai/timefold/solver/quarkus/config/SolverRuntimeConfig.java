package ai.timefold.solver.quarkus.config;

import java.util.Optional;

import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * During run time, this overrides some of Timefold's {@link SolverConfig}
 * properties.
 */
@ConfigGroup
public interface SolverRuntimeConfig {
    /**
     * Enable multithreaded solving for a single problem, which increases CPU consumption.
     * Defaults to {@value SolverConfig#MOVE_THREAD_COUNT_NONE}.
     * Other options include {@value SolverConfig#MOVE_THREAD_COUNT_AUTO}, a number
     * or formula based on the available processor count.
     */
    Optional<String> moveThreadCount();

    /**
     * Configuration properties that overwrite {@link TerminationConfig}.
     */
    TerminationRuntimeConfig termination();
}
