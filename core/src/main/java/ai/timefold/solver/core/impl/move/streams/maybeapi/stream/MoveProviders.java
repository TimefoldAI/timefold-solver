package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import java.util.List;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

public interface MoveProviders<Solution_> {

    List<MoveProvider<Solution_>> defineMoves(PlanningSolutionMetaModel<Solution_> solutionMetaModel);

}
