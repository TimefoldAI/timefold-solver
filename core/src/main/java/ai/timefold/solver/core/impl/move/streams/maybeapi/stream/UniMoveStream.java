package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import java.util.function.BiPredicate;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UniMoveStream<Solution_, A> extends MoveStream<Solution_> {

    default <B> BiMoveStream<Solution_, A, B> pick(UniDataStream<Solution_, B> uniDataStream) {
        return pick(uniDataStream, (a, b) -> true);
    }

    <B> BiMoveStream<Solution_, A, B> pick(UniDataStream<Solution_, B> uniDataStream, BiPredicate<A, B> filter);

}
