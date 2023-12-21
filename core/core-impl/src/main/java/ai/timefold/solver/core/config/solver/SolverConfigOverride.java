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
    private boolean singleThread = true; // TODO - Understand how to use this variable
    private boolean multiThread = false; // TODO - Understand how to use this variable
    private String moveThreadCount;

    // ************************************************************************
    // Constructors and simple getters/setters
    // ************************************************************************

    public SolverConfigOverride() {
    }

    public SolverConfigOverride(SolverExecutionConfig<Solution_, ?> executionConfig) {
        this.terminationConfig = executionConfig.getTerminationConfig();
        this.singleThread = executionConfig.isSingleThread();
        this.multiThread = executionConfig.isMultiThread();
        this.moveThreadCount = executionConfig.getMoveThreadCount();
    }

    public TerminationConfig getTerminationConfig() {
        return terminationConfig;
    }

    public boolean isSingleThread() {
        return singleThread;
    }

    public boolean isMultiThread() {
        return multiThread;
    }

    public String getMoveThreadCount() {
        return moveThreadCount;
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public SolverConfigOverride<Solution_> withTerminationConfig(TerminationConfig terminationConfig) {
        this.terminationConfig = terminationConfig;
        return this;
    }

    public SolverConfigOverride<Solution_> multiThreaded() {
        this.multiThread = true;
        this.singleThread = false;
        return this;
    }

    public SolverConfigOverride<Solution_> multiThreaded(String moveThreadCount) {
        multiThreaded();
        this.moveThreadCount = moveThreadCount;
        return this;
    }

    public SolverConfigOverride<Solution_> singleThreaded() {
        this.multiThread = false;
        this.moveThreadCount = null;
        this.singleThread = true;
        return this;
    }
}
