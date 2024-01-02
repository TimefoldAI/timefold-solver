package ai.timefold.solver.core.config.solver;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

/**
 * Includes settings to override default {@link ai.timefold.solver.core.api.solver.Solver} configuration.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class SolverConfigOverride<Solution_> {

    private TerminationConfig terminationConfig = null;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public SolverConfigOverride() {
    }

    public SolverConfigOverride(SolverJobConfig<Solution_, ?> executionConfig) {
        this.terminationConfig = executionConfig.getTerminationConfig();
    }

    public TerminationConfig getTerminationConfig() {
        return terminationConfig;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public SolverConfigOverride<Solution_> withTerminationConfig(TerminationConfig terminationConfig) {
        this.terminationConfig = terminationConfig;
        return this;
    }
}
