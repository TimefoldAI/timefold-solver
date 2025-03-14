package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.VariableMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultPlanningEntityMetaModel<Solution_, Entity_>
        implements PlanningEntityMetaModel<Solution_, Entity_> {

    private final EntityDescriptor<Solution_> entityDescriptor;
    private final PlanningSolutionMetaModel<Solution_> solution;
    private final Class<Entity_> type;
    private final List<VariableMetaModel<Solution_, Entity_, ?>> variables = new ArrayList<>();

    @SuppressWarnings("unchecked")
    DefaultPlanningEntityMetaModel(PlanningSolutionMetaModel<Solution_> solution,
            EntityDescriptor<Solution_> entityDescriptor) {
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
    public List<VariableMetaModel<Solution_, Entity_, ?>> variables() {
        return Collections.unmodifiableList(variables);
    }

    void addVariable(VariableMetaModel<Solution_, Entity_, ?> variable) {
        if (variable.entity() != this) {
            throw new IllegalArgumentException("The variable (%s) is not part of this entity (%s)."
                    .formatted(variable.name(), type.getSimpleName()));
        }
        variables.add(variable);
    }

    @Override
    public String toString() {
        return "%s Entity (%s) with variables (%s)"
                .formatted(isGenuine() ? "Genuine" : "Shadow", type,
                        variables().stream()
                                .map(VariableMetaModel::name)
                                .toList());
    }

}
