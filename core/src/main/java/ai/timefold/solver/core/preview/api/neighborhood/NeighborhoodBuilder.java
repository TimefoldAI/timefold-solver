package ai.timefold.solver.core.preview.api.neighborhood;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface NeighborhoodBuilder<Solution_> {

    PlanningSolutionMetaModel<Solution_> getSolutionMetaModel();

    NeighborhoodBuilder<Solution_> add(MoveProvider<Solution_> moveProvider);

    Neighborhood build();

}
