package ai.timefold.solver.core.api.move.stream;

import java.util.function.BiPredicate;
import java.util.function.Function;

public interface JitMoveUniStream<Solution_, A> extends JitMoveStream<Solution_> {

    default <B> JitMoveBiStream<Solution_, A, B> pick(Class<B> clz) {
        return pick(getMoveFactory().enumerate(clz));
    }

    default <B> JitMoveBiStream<Solution_, A, B> pick(Class<B> clz, BiPredicate<A, B> filter) {
        return pick(getMoveFactory().enumerate(clz), filter);
    }

    <B> JitMoveBiStream<Solution_, A, B> pick(CachedMoveUniStream<Solution_, B> cachedMoveUniStream);

    <B> JitMoveBiStream<Solution_, A, B> pick(CachedMoveUniStream<Solution_, B> cachedMoveUniStream, BiPredicate<A, B> filter);

    <B> JitMoveBiStream<Solution_, A, B> pick(Function<A, CachedMoveUniStream<Solution_, B>> cachedMoveUniStreamFunction);

    default JitMoveBiStream<Solution_, A, A> pickOther(Class<A> clz) {
        return pickOther(getMoveFactory().enumerate(clz));
    }

    default JitMoveBiStream<Solution_, A, A> pickOther(Class<A> clz, BiPredicate<A, A> filter) {
        return pickOther(getMoveFactory().enumerate(clz), filter);
    }

    // TODO identity or equality?
    default JitMoveBiStream<Solution_, A, A> pickOther(CachedMoveUniStream<Solution_, A> cachedMoveUniStream) {
        return pick(cachedMoveUniStream, (a, b) -> !a.equals(b));
    }

    // TODO identity or equality?
    default JitMoveBiStream<Solution_, A, A> pickOther(CachedMoveUniStream<Solution_, A> cachedMoveUniStream,
            BiPredicate<A, A> filter) {
        return pick(cachedMoveUniStream, filter);
    }

}
