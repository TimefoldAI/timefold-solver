package ai.timefold.solver.core.impl.neighborhood.maybeapi;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface NeighborhoodBuilder<Solution_> {

    PlanningSolutionMetaModel<Solution_> getSolutionMetaModel();

    NeighborhoodBuilder<Solution_> add(MoveDefinition<Solution_> moveDefinition);

    Neighborhood build();

}
