package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import java.util.function.BiPredicate;
import java.util.function.Function;

public interface UniMoveStream<Solution_, A> extends MoveStream<Solution_> {

    default <B> BiMoveStream<Solution_, A, B> pick(Class<B> clz) {
        return pick(getMoveFactory().enumerate(clz));
    }

    default <B> BiMoveStream<Solution_, A, B> pick(Class<B> clz, BiPredicate<A, B> filter) {
        return pick(getMoveFactory().enumerate(clz), filter);
    }

    <B> BiMoveStream<Solution_, A, B> pick(UniDataStream<Solution_, B> uniDataStream);

    <B> BiMoveStream<Solution_, A, B> pick(UniDataStream<Solution_, B> uniDataStream, BiPredicate<A, B> filter);

    <B> BiMoveStream<Solution_, A, B> pick(Function<A, UniDataStream<Solution_, B>> uniDataStreamFunction);

    default BiMoveStream<Solution_, A, A> pickOther(Class<A> clz) {
        return pickOther(getMoveFactory().enumerate(clz));
    }

    default BiMoveStream<Solution_, A, A> pickOther(Class<A> clz, BiPredicate<A, A> filter) {
        return pickOther(getMoveFactory().enumerate(clz), filter);
    }

    // TODO identity or equality?
    default BiMoveStream<Solution_, A, A> pickOther(UniDataStream<Solution_, A> uniDataStream) {
        return pick(uniDataStream, (a, b) -> !a.equals(b));
    }

    // TODO identity or equality?
    default BiMoveStream<Solution_, A, A> pickOther(UniDataStream<Solution_, A> uniDataStream, BiPredicate<A, A> filter) {
        return pick(uniDataStream, filter);
    }

}
