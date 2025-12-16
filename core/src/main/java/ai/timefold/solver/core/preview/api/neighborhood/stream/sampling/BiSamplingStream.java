package ai.timefold.solver.core.preview.api.neighborhood.stream.sampling;

import ai.timefold.solver.core.preview.api.neighborhood.BiMoveConstructor;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BiSamplingStream<Solution_, A, B> extends SamplingStream {

    MoveStream<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor);

}
