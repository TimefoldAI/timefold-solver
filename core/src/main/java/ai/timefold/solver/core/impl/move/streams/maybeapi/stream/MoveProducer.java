package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.preview.api.move.Move;

public interface MoveProducer<Solution_> {

    Iterable<Move<Solution_>> getMoveIterable(MoveDirector<Solution_> moveDirector);

}
