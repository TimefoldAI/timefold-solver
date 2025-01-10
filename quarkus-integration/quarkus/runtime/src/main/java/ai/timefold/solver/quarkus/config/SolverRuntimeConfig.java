package ai.timefold.solver.quarkus.config;

import java.util.Optional;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
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
     * Enable runtime assertions to detect common bugs in your implementation during development.
     * Defaults to {@link EnvironmentMode#REPRODUCIBLE}.
     */
    Optional<EnvironmentMode> environmentMode();

    /**
     * Enable daemon mode. In daemon mode, non-early termination pauses the solver instead of stopping it,
     * until the next problem fact change arrives.
     * This is often useful for real-time planning.
     * Defaults to "false".
     */
    Optional<Boolean> daemon();

    /**
     * Note: this setting is only available
     * for <a href="https://timefold.ai/docs/timefold-solver/latest/enterprise-edition/enterprise-edition">Timefold Solver
     * Enterprise Edition</a>.
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
