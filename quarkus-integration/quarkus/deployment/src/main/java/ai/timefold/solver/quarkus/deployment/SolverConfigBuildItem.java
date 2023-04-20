package ai.timefold.solver.quarkus.deployment;

import ai.timefold.solver.core.config.solver.SolverConfig;

import io.quarkus.builder.item.SimpleBuildItem;

public final class SolverConfigBuildItem extends SimpleBuildItem {
    SolverConfig solverConfig;

    public SolverConfigBuildItem(SolverConfig solverConfig) {
        this.solverConfig = solverConfig;
    }

    public SolverConfig getSolverConfig() {
        return solverConfig;
    }
}
