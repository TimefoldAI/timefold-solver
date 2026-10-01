package ai.timefold.solver.core.preview.api.neighborhood.stream.picking;

import ai.timefold.solver.core.preview.api.neighborhood.BiMoveConstructor;
import ai.timefold.solver.core.preview.api.neighborhood.UniMoveConstructor;
import ai.timefold.solver.core.preview.api.neighborhood.stream.MoveStream;

import org.jspecify.annotations.NullMarked;

/**
 * A two-value variant of {@link PickingStream}.
 *
 * @param <Solution_>
 * @param <A>
 */
@NullMarked
public interface BiPickingStream<Solution_, A, B> extends PickingStream {

    /**
     * As defined by {@link UniPickingStream#asMove(UniMoveConstructor)}.
     */
    MoveStream<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor);

}
