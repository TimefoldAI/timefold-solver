package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.preview.api.move.Move;

public interface BiMoveStream<Solution_, A, B> extends MoveStream<Solution_> {

    BiMoveConstructor<Solution_, A, B> asMove(TriFunction<Solution_, A, B, Move<Solution_>> moveFactory);

}
