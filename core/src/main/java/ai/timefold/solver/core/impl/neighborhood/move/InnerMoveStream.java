package ai.timefold.solver.core.impl.neighborhood.move;

import java.util.Set;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.NeighborhoodSession;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractEnumeratingStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface InnerMoveStream<Solution_> extends MoveStream<Solution_> {

    @Override
    MoveIterable<Solution_> getMoveIterable(NeighborhoodSession neighborhoodSession);

    void collectActiveEnumeratingStreams(Set<AbstractEnumeratingStream<Solution_>> enumeratingStreamSet);

}
