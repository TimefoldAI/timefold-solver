package ai.timefold.solver.core.impl.neighborhood.move;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.NeighborhoodSession;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerMoveStream<Solution_> extends MoveStream<Solution_> {

    @Override
    MoveIterable<Solution_> getMoveIterable(NeighborhoodSession neighborhoodSession);

}
