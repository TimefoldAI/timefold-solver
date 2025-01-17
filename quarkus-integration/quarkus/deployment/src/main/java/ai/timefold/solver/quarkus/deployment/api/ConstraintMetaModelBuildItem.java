package ai.timefold.solver.quarkus.deployment.api;

import java.util.Map;

import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Represents a {@link ai.timefold.solver.core.api.score.stream.ConstraintMetaModel} at the build time for the purpose
 * of Quarkus augmentation.
 */
public final class ConstraintMetaModelBuildItem extends SimpleBuildItem {

    private final Map<String, ConstraintMetaModel> constraintMetaModelsBySolverNames;

    public ConstraintMetaModelBuildItem(Map<String, ConstraintMetaModel> constraintMetaModelsBySolverNames) {
        this.constraintMetaModelsBySolverNames = Map.copyOf(constraintMetaModelsBySolverNames);
    }

    public Map<String, ConstraintMetaModel> constraintMetaModelsBySolverNames() {
        return constraintMetaModelsBySolverNames;
    }
}
