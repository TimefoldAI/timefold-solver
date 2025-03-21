package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BiMoveStream<Solution_, A, B> extends MoveStream<Solution_> {

    MoveProducer<Solution_> asMove(BiMoveConstructor<Solution_, A, B> moveConstructor);

}
