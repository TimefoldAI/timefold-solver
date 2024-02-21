package ai.timefold.solver.quarkus.deployment;

import java.util.Map;

import ai.timefold.solver.core.config.solver.SolverConfig;

import io.quarkus.builder.item.SimpleBuildItem;

public final class SolverConfigBuildItem extends SimpleBuildItem {
    private final Map<String, SolverConfig> solverConfigurations;
    private final GeneratedGizmoClasses generatedGizmoClasses;

    /**
     * Constructor for multiple solver configurations.
     */
    public SolverConfigBuildItem(Map<String, SolverConfig> solverConfig, GeneratedGizmoClasses generatedGizmoClasses) {
        this.solverConfigurations = solverConfig;
        this.generatedGizmoClasses = generatedGizmoClasses;
    }

    public Map<String, SolverConfig> getSolverConfigMap() {
        return solverConfigurations;
    }

    public GeneratedGizmoClasses getGeneratedGizmoClasses() {
        return generatedGizmoClasses;
    }
}
