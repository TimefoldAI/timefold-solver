package ai.timefold.solver.core.preview.api.neighborhood.stream;

import ai.timefold.solver.core.preview.api.neighborhood.NeighborhoodSession;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MoveStream<Solution_> {

    MoveIterable<Solution_> getMoveIterable(NeighborhoodSession neighborhoodSession);

}
