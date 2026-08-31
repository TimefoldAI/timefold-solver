package ai.timefold.solver.core.preview.api.neighborhood.stream.picking;

import ai.timefold.solver.core.preview.api.neighborhood.BiMoveConstructor;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BiPickingStream<Solution_, A, B> extends PickingStream {

    MoveStream<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor);

}
