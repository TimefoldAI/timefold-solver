package ai.timefold.solver.quarkus.devui;

import java.util.Map;

public class SolverConfigText {

    private final Map<String, String> solverConfigurations;

    /**
     * Constructor for multiple solver configurations.
     */
    public SolverConfigText(Map<String, String> solverConfigurations) {
        this.solverConfigurations = solverConfigurations;
    }

    public Map<String, String> getSolverConfigurations() {
        return solverConfigurations;
    }
}
