package ai.timefold.solver.quarkus.devui;

import java.util.Map;
import java.util.Objects;

public class SolverConfigText {

    private static final String DEFAULT_SOLVER_NAME = "default";
    private final Map<String, String> solverConfigurations;

    /**
     * Constructor for multiple solver configurations.
     */
    public SolverConfigText(Map<String, String> solverConfigurations) {
        this.solverConfigurations = solverConfigurations;
    }

    /**
     * Returns the solver configuration of the default solver.
     */
    public String getSolverConfigText() {
        return solverConfigurations.get(DEFAULT_SOLVER_NAME);
    }

    /**
     * Returns the configuration of a given solver name.
     *
     * @param solverName never null, the solver name
     */
    public String getSolverConfigText(String solverName) {
        return this.solverConfigurations
                .get(Objects.requireNonNull(solverName, "Invalid solverName (null) given to SolverConfigText."));
    }

    /**
     * Sets the configuration of the default solver.
     *
     * @param solverConfigText may be empty, the solver configuration
     */
    public void setSolverConfigText(String solverConfigText) {
        this.solverConfigurations.put(DEFAULT_SOLVER_NAME, solverConfigText);
    }

    /**
     * Sets the configuration of a given solver name.
     * 
     * @param solverName never null, the solver name
     * @param solverConfigText may be empty, the solver configuration
     */
    public void setSolverConfigText(String solverName, String solverConfigText) {
        this.solverConfigurations.put(
                Objects.requireNonNull(solverName, "Invalid solverName (null) given to SolverConfigText."),
                solverConfigText);
    }
}
