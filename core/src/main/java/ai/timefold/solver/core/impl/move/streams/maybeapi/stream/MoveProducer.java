package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import ai.timefold.solver.core.impl.move.streams.MoveIterable;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MoveProducer<Solution_> {

    MoveIterable<Solution_> getMoveIterable(MoveStreamSession<Solution_> moveStreamSession);

}
