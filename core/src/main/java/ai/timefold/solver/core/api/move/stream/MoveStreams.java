package ai.timefold.solver.core.api.move.stream;

import java.util.Collection;
import java.util.function.Function;

public interface MoveStreams<Solution_> {

    <A> CachedMoveUniStream<Solution_, A> enumerate(Class<A> clz);

    <A> CachedMoveUniStream<Solution_, A> enumerate(Function<Solution_, Collection<A>> collectionFunction);

    <A> CachedMoveUniStream<Solution_, A> enumerate(Collection<A> collection);

    default <A> JitMoveUniStream<Solution_, A> pick(Class<A> clz) {
        return pick(enumerate(clz));
    }

    <A> JitMoveUniStream<Solution_, A> pick(CachedMoveUniStream<Solution_, A> cachedMoveUniStream);

}
