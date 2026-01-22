package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.ShadowEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.ShadowVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultShadowEntityMetaModel<Solution_, Entity_>
        implements ShadowEntityMetaModel<Solution_, Entity_>, InnerPlanningEntityMetaModel<Solution_, Entity_> {

    private final EntityDescriptor<Solution_> entityDescriptor;
    private final PlanningSolutionMetaModel<Solution_> solution;
    private final Class<Entity_> type;
    private final List<ShadowVariableMetaModel<Solution_, Entity_, ?>> variables = new ArrayList<>();

    @SuppressWarnings("unchecked")
    DefaultShadowEntityMetaModel(PlanningSolutionMetaModel<Solution_> solution, EntityDescriptor<Solution_> entityDescriptor) {
        this.solution = Objects.requireNonNull(solution);
        this.entityDescriptor = Objects.requireNonNull(entityDescriptor);
        this.type = (Class<Entity_>) entityDescriptor.getEntityClass();
    }

    public EntityDescriptor<Solution_> entityDescriptor() {
        return entityDescriptor;
    }

    @Override
    public PlanningSolutionMetaModel<Solution_> solution() {
        return solution;
    }

    @Override
    public Class<Entity_> type() {
        return type;
    }

    @Override
    public List<ShadowVariableMetaModel<Solution_, Entity_, ?>> variables() {
        return Collections.unmodifiableList(variables);
    }

    @Override
    public void addVariable(VariableMetaModel<Solution_, Entity_, ?> variable) {
        if (variable.entity() != this) {
            throw new IllegalArgumentException("The entity (%s) does not have the given variable (%s)."
                    .formatted(type.getCanonicalName(), variable.name()));
        }
        if (!(variable instanceof ShadowVariableMetaModel<Solution_, Entity_, ?> shadowVariable)) {
            throw new IllegalArgumentException("The variable (%s) is not a shadow variable on the given entity (%s)."
                    .formatted(variable.name(), type.getCanonicalName()));
        }
        variables.add(shadowVariable);
    }

    @Override
    public String toString() {
        return "Shadow entity (%s) with shadow variables (%s)"
                .formatted(type.getCanonicalName(),
                        variables().stream()
                                .map(VariableMetaModel::name)
                                .toList());
    }

}
