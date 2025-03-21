package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MoveStream<Solution_> {

    MoveStreamFactory<Solution_> getMoveStreamFactory();

}
