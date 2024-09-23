package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.metamodel.EntityMetaModel;
import ai.timefold.solver.core.api.domain.metamodel.SolutionMetaModel;

public final class DefaultSolutionMetaModel<Solution_> implements SolutionMetaModel<Solution_> {

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final Class<Solution_> type;
    private final List<EntityMetaModel<Solution_, ?>> entities = new ArrayList<>();

    public DefaultSolutionMetaModel(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = Objects.requireNonNull(solutionDescriptor);
        this.type = solutionDescriptor.getSolutionClass();
    }

    public SolutionDescriptor<Solution_> getSolutionDescriptor() {
        return solutionDescriptor;
    }

    @Override
    public Class<Solution_> type() {
        return type;
    }

    @Override
    public List<EntityMetaModel<Solution_, ?>> entities() {
        return Collections.unmodifiableList(entities);
    }

    void addEntity(EntityMetaModel<Solution_, ?> entityMetaModel) {
        if (entityMetaModel.solution() != this) {
            throw new IllegalArgumentException("The entityMetaModel (" + entityMetaModel
                    + ") must be created by this solutionMetaModel (" + this + ").");
        }
        entities.add(entityMetaModel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DefaultSolutionMetaModel<?> that))
            return false;
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type);
    }

    @Override
    public String toString() {
        return "Planning Solution (%s) with entities (%s)"
                .formatted(type.getSimpleName(),
                        entities.stream()
                                .map(e -> e.type().getSimpleName())
                                .toList());
    }

}
