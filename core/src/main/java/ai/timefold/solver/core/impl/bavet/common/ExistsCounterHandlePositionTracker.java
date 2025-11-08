package ai.timefold.solver.core.impl.bavet.common;

import java.util.function.ToIntFunction;

import ai.timefold.solver.core.impl.bavet.common.index.ElementPositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

import org.jspecify.annotations.NullMarked;

@SuppressWarnings({ "rawtypes", "unchecked" })
@NullMarked
record ExistsCounterHandlePositionTracker<Tuple_ extends AbstractTuple>(
        ToIntFunction<ExistsCounterHandle<Tuple_>> positionGetter,
        PositionSetter<Tuple_> positionSetter)
        implements
            ElementPositionTracker<ExistsCounterHandle<Tuple_>> {

    private static final ExistsCounterHandlePositionTracker LEFT = new ExistsCounterHandlePositionTracker(
            (ToIntFunction<ExistsCounterHandle>) tracker -> tracker.leftPosition,
            (tracker, position) -> {
                var oldValue = tracker.leftPosition;
                tracker.leftPosition = position;
                return oldValue;
            });
    private static final ExistsCounterHandlePositionTracker RIGHT = new ExistsCounterHandlePositionTracker(
            (ToIntFunction<ExistsCounterHandle>) tracker -> tracker.rightPosition,
            (tracker, position) -> {
                var oldValue = tracker.rightPosition;
                tracker.rightPosition = position;
                return oldValue;
            });

    public static <Tuple_ extends AbstractTuple> ExistsCounterHandlePositionTracker<Tuple_> left() {
        return LEFT;
    }

    public static <Tuple_ extends AbstractTuple> ExistsCounterHandlePositionTracker<Tuple_> right() {
        return RIGHT;
    }

    @Override
    public void setPosition(ExistsCounterHandle<Tuple_> element, int position) {
        positionSetter.apply(element, position);
    }

    @Override
    public int clearPosition(ExistsCounterHandle<Tuple_> element) {
        var oldPosition = positionGetter.applyAsInt(element);
        positionSetter.apply(element, -1);
        return oldPosition;
    }

    @FunctionalInterface
    @NullMarked
    interface PositionSetter<Tuple_ extends AbstractTuple> {

        int apply(ExistsCounterHandle<Tuple_> tracker, int position);

    }

}
