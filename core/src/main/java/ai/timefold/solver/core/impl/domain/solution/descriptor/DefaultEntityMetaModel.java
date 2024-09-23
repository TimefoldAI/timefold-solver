package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.metamodel.EntityMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.SolutionMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.VariableMetaModel;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;

final class DefaultEntityMetaModel<Solution_, Entity_>
        implements EntityMetaModel<Solution_, Entity_> {

    private final EntityDescriptor<Solution_> entityDescriptor;
    private final SolutionMetaModel<Solution_> solution;
    private final Class<Entity_> type;
    private final List<VariableMetaModel<Solution_, Entity_, ?>> variables = new ArrayList<>();

    @SuppressWarnings("unchecked")
    DefaultEntityMetaModel(SolutionMetaModel<Solution_> solution, EntityDescriptor<Solution_> entityDescriptor) {
        this.solution = Objects.requireNonNull(solution);
        this.entityDescriptor = Objects.requireNonNull(entityDescriptor);
        this.type = (Class<Entity_>) entityDescriptor.getEntityClass();
    }

    public EntityDescriptor<Solution_> getEntityDescriptor() {
        return entityDescriptor;
    }

    @Override
    public SolutionMetaModel<Solution_> solution() {
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
            throw new IllegalArgumentException("The variable (" + variable + ") is not part of this entity (" + this + ").");
        }
        variables.add(variable);
    }

}
