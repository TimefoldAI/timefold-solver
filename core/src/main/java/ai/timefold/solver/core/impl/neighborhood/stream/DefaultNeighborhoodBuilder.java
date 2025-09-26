package ai.timefold.solver.core.impl.neighborhood.stream;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveDefinition;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.NeighborhoodBuilder;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public final class DefaultNeighborhoodBuilder<Solution_> implements NeighborhoodBuilder<Solution_> {

    private final PlanningSolutionMetaModel<Solution_> solutionMetaModel;
    private final Set<MoveDefinition<Solution_>> moveDefinitionSet = new LinkedHashSet<>();

    public DefaultNeighborhoodBuilder(PlanningSolutionMetaModel<Solution_> solutionMetaModel) {
        this.solutionMetaModel = Objects.requireNonNull(solutionMetaModel);
    }

    @Override
    public PlanningSolutionMetaModel<Solution_> getSolutionMetaModel() {
        return solutionMetaModel;
    }

    @Override
    public NeighborhoodBuilder<Solution_> add(MoveDefinition<Solution_> moveDefinition) {
        moveDefinitionSet.add(moveDefinition);
        return this;
    }

    @Override
    public DefaultNeighborhood<Solution_> build() {
        return new DefaultNeighborhood<>(List.copyOf(moveDefinitionSet));
    }

}
