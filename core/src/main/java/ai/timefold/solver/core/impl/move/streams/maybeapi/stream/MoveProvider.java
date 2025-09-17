package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import java.util.List;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MoveProvider<Solution_> {

    List<MoveDefinition<Solution_>> defineMoves(PlanningSolutionMetaModel<Solution_> solutionMetaModel);

}
