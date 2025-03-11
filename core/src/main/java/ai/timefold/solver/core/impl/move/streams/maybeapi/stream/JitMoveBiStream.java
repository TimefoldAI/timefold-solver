package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.impl.move.streams.maybeapi.BiMoveConstructor;
import ai.timefold.solver.core.preview.api.move.Move;

public interface JitMoveBiStream<Solution_, A, B> extends JitMoveStream<Solution_> {

    BiMoveConstructor<Solution_, A, B> asMove(TriFunction<Solution_, A, B, Move<Solution_>> moveFactory);

}
