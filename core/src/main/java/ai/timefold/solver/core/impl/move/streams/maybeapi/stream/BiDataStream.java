package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import java.util.function.BiPredicate;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface BiDataStream<Solution_, A, B> extends DataStream<Solution_> {

    /**
     * Exhaustively test each fact against the {@link BiPredicate}
     * and match if {@link BiPredicate#test(Object, Object)} returns true.
     */
    BiDataStream<Solution_, A, B> filter(BiPredicate<A, B> predicate);

}
