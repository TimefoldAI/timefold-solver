package ai.timefold.solver.core.impl.neighborhood.maybeapi.stream.sampling;

import ai.timefold.solver.core.impl.neighborhood.maybeapi.MoveStream;
import ai.timefold.solver.core.impl.neighborhood.maybeapi.move.BiMoveConstructor;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BiSamplingStream<Solution_, A, B> extends SamplingStream {

    MoveStream<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor);

}
