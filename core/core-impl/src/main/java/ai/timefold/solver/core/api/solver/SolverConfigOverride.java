package ai.timefold.solver.core.api.solver;

import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

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
     * @return this, never null
     */
    public SolverConfigOverride<Solution_> withTerminationConfig(TerminationConfig terminationConfig) {
        this.terminationConfig =
                Objects.requireNonNull(terminationConfig, "Invalid terminationConfig (null) given to SolverConfigOverride.");
        return this;
    }
}
