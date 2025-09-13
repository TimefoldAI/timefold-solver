package ai.timefold.solver.core.api.solver;

import java.time.Duration;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

import org.jspecify.annotations.NonNull;

/**
 * Includes settings to override default {@link ai.timefold.solver.core.api.solver.Solver} configuration.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public final class SolverConfigOverride<Solution_> {

    private TerminationConfig terminationConfig = null;

    public TerminationConfig getTerminationConfig() {
        return terminationConfig;
    }

    /**
     * Sets the solver {@link TerminationConfig}.
     *
     * @param terminationConfig allows overriding the default termination config of {@link Solver}
     * @return this
     */
    public @NonNull SolverConfigOverride<Solution_> withTerminationConfig(@NonNull TerminationConfig terminationConfig) {
        this.terminationConfig =
                Objects.requireNonNull(terminationConfig, "Invalid terminationConfig (null) given to SolverConfigOverride.");
        return this;
    }

    /**
     * Sets a time limit for the solver to run, overriding the default termination configuration.
     *
     * This is equivalent to calling {@link TerminationConfig#withSpentLimit(Duration)}.
     *
     * @param spentLimit the maximum duration the solver is allowed to run
     * @return this
     */
    public @NonNull SolverConfigOverride<Solution_> withTerminationSpentLimit(@NonNull Duration spentLimit) {
        if (this.terminationConfig == null) {
            this.terminationConfig = new TerminationConfig();
        }
        this.terminationConfig.setSpentLimit(spentLimit);
        return this;
    }

    /**
     * Sets a time limit for the solver to run without finding an improved solution, overriding the default termination configuration.
     *
     * This is equivalent to calling {@link TerminationConfig#withUnimprovedSpentLimit(Duration)}.
     *
     * @param unimprovedSpentLimit the maximum duration the solver is allowed to run without improvement
     * @return this
     */
    public @NonNull SolverConfigOverride<Solution_> withTerminationUnimprovedSpentLimit(@NonNull Duration unimprovedSpentLimit) {
        if (this.terminationConfig == null) {
            this.terminationConfig = new TerminationConfig();
        }
        this.terminationConfig.setUnimprovedSpentLimit(unimprovedSpentLimit);
        return this;
    }
}
