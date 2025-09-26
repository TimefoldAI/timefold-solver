package ai.timefold.solver.core.impl.neighborhood.maybeapi;

import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MoveStream<Solution_> {

    Iterable<Move<Solution_>> getMoveIterable(NeighborhoodSession neighborhoodSession);

}
