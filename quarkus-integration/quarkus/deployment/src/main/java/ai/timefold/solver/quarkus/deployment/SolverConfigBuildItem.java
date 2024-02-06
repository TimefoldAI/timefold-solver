package ai.timefold.solver.quarkus.deployment;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        this.solverConfigurations = solverConfig;
        this.generatedGizmoClasses = generatedGizmoClasses;
    }

    /**
     * Returns the configuration of a given solver name.
     *
     * @param solverName never null, the solver name
     */
    public SolverConfig getSolverConfig(String solverName) {
        return this.solverConfigurations
                .get(Objects.requireNonNull(solverName, "Invalid solverName (null) given to SolverConfigBuildItem."));
    }

    /**
     * Returns the configuration of the default solver.
     */
    public SolverConfig getSolverConfig() {
        return this.solverConfigurations.get(TimefoldBuildTimeConfig.DEFAULT_SOLVER_NAME);
    }

    public Set<String> getSolverNames() {
        return this.solverConfigurations.keySet();
    }

    public Map<String, SolverConfig> getSolvetConfigMap() {
        return solverConfigurations;
    }

    public GeneratedGizmoClasses getGeneratedGizmoClasses() {
        return generatedGizmoClasses;
    }
}
