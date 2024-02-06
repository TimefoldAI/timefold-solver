package ai.timefold.solver.quarkus.config;

import java.util.Optional;

import ai.timefold.solver.core.config.solver.SolverManagerConfig;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * During build time, this is translated into Timefold's {@link SolverManagerConfig}.
 */
@ConfigGroup
public interface SolverManagerRuntimeConfig {

    /**
     * The number of solvers that run in parallel. This directly influences CPU consumption.
     * Defaults to {@value SolverManagerConfig#PARALLEL_SOLVER_COUNT_AUTO}.
     * Other options include a number or formula based on the available processor count.
     */
    @WithDefault("AUTO")
    Optional<String> parallelSolverCount();

}
