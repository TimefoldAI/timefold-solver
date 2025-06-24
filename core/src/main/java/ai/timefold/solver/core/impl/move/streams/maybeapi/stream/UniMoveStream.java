package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UniMoveStream<Solution_, A> extends MoveStream<Solution_> {

    @SuppressWarnings("unchecked")
    default <B> BiMoveStream<Solution_, A, B> pick(UniDataStream<Solution_, B> uniDataStream) {
        return pick(uniDataStream, SolutionViewTriPredicate.TRUE);
    }

    <B> BiMoveStream<Solution_, A, B> pick(UniDataStream<Solution_, B> uniDataStream,
            SolutionViewTriPredicate<Solution_, A, B> filter);

}
