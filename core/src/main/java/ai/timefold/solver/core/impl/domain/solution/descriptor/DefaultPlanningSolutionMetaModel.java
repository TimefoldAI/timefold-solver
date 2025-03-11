package ai.timefold.solver.core.impl.domain.solution.descriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class DefaultPlanningSolutionMetaModel<Solution_> implements PlanningSolutionMetaModel<Solution_> {

    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final Class<Solution_> type;
    private final List<PlanningEntityMetaModel<Solution_, ?>> entities = new ArrayList<>();

    DefaultPlanningSolutionMetaModel(SolutionDescriptor<Solution_> solutionDescriptor) {
        this.solutionDescriptor = Objects.requireNonNull(solutionDescriptor);
        this.type = solutionDescriptor.getSolutionClass();
    }

    public SolutionDescriptor<Solution_> solutionDescriptor() {
        return solutionDescriptor;
    }

    @Override
    public Class<Solution_> type() {
        return type;
    }

    @Override
    public List<PlanningEntityMetaModel<Solution_, ?>> entities() {
        return Collections.unmodifiableList(entities);
    }

    void addEntity(PlanningEntityMetaModel<Solution_, ?> planningEntityMetaModel) {
        if (planningEntityMetaModel.solution() != this) {
            throw new IllegalArgumentException("The entityMetaModel (%s) must be created by this solutionMetaModel (%s)."
                    .formatted(planningEntityMetaModel, this));
        }
        entities.add(planningEntityMetaModel);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DefaultPlanningSolutionMetaModel<?> that))
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
