package ai.timefold.solver.core.impl.move.streams.maybeapi.stream;

import java.util.function.BiPredicate;

import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.pickers.BiPicker;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface UniMoveStream<Solution_, A> extends MoveStream<Solution_> {

    default <B> BiMoveStream<Solution_, A, B> pick(UniDataStream<Solution_, B> uniDataStream) {
        return pick(uniDataStream, (a, b) -> true);
    }

    <B> BiMoveStream<Solution_, A, B> pick(UniDataStream<Solution_, B> uniDataStream, BiPredicate<A, B> filter);

    @SuppressWarnings("unchecked")
    default <B> BiMoveStream<Solution_, A, B> pick(UniDataStream<Solution_, B> uniDataStream, BiPicker<A, B> picker) {
        return pick(uniDataStream, new BiPicker[] { picker });
    }

    @SuppressWarnings("unchecked")
    default <B> BiMoveStream<Solution_, A, B> pick(UniDataStream<Solution_, B> uniDataStream, BiPicker<A, B> picker1,
            BiPicker<A, B> picker2) {
        return pick(uniDataStream, new BiPicker[] { picker1, picker2 });
    }

    @SuppressWarnings("unchecked")
    default <B> BiMoveStream<Solution_, A, B> pick(UniDataStream<Solution_, B> uniDataStream, BiPicker<A, B> picker1,
            BiPicker<A, B> picker2, BiPicker<A, B> picker3) {
        return pick(uniDataStream, new BiPicker[] { picker1, picker2, picker3 });
    }

    <B> BiMoveStream<Solution_, A, B> pick(UniDataStream<Solution_, B> uniDataStream, BiPicker<A, B>... pickers);

}
