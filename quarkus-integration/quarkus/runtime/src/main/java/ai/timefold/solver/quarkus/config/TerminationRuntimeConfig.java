package ai.timefold.solver.quarkus.config;

import java.time.Duration;
import java.util.Optional;

import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * During build time, this is translated into Timefold's {@link TerminationConfig}.
 */
@ConfigGroup
public interface TerminationRuntimeConfig {

    /**
     * How long the solver can run.
     * For example: "30s" is 30 seconds. "5m" is 5 minutes. "2h" is 2 hours. "1d" is 1 day.
     * Also supports ISO-8601 format, see {@link Duration}.
     */
    Optional<Duration> spentLimit();

    /**
     * How long the solver can run without finding a new best solution after finding a new best solution.
     * For example: "30s" is 30 seconds. "5m" is 5 minutes. "2h" is 2 hours. "1d" is 1 day.
     * Also supports ISO-8601 format, see {@link Duration}.
     */
    Optional<Duration> unimprovedSpentLimit();

    /**
     * Terminates the solver when a specific or higher score has been reached.
     * For example: "0hard/-1000soft" terminates when the best score changes from "0hard/-1200soft" to "0hard/-900soft".
     * Wildcards are supported to replace numbers.
     * For example: "0hard/*soft" to terminate when any feasible score is reached.
     */
    Optional<String> bestScoreLimit();

}
