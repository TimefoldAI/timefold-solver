package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface MoveProducer<Solution_> {

    Iterable<Move<Solution_>> getMoveIterable(MoveStreamSession<Solution_> moveStreamSession);

}
