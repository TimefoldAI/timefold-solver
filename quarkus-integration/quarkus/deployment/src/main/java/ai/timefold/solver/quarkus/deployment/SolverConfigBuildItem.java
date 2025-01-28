package ai.timefold.solver.quarkus.deployment;

import java.util.Map;

import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.quarkus.deployment.config.TimefoldBuildTimeConfig;

import io.quarkus.builder.item.SimpleBuildItem;

public final class SolverConfigBuildItem extends SimpleBuildItem {
    private final Map<String, SolverConfig> solverConfigurations;
    private final GeneratedGizmoClasses generatedGizmoClasses;

    /**
     * Constructor for multiple solver configurations.
     */
    public SolverConfigBuildItem(Map<String, SolverConfig> solverConfig, GeneratedGizmoClasses generatedGizmoClasses) {
        // Defensive copy to avoid changing the map in dependent build items.
        this.solverConfigurations = Map.copyOf(solverConfig);
        this.generatedGizmoClasses = generatedGizmoClasses;
    }

    public boolean isDefaultSolverConfig(String solverName) {
        return solverConfigurations.size() <= 1 || TimefoldBuildTimeConfig.DEFAULT_SOLVER_NAME.equals(solverName);
    }

    public Map<String, SolverConfig> getSolverConfigMap() {
        return solverConfigurations;
    }

    public GeneratedGizmoClasses getGeneratedGizmoClasses() {
        return generatedGizmoClasses;
    }
}
