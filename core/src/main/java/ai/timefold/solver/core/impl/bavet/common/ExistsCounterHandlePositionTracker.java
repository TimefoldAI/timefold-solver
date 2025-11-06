package ai.timefold.solver.core.impl.bavet.common;

import ai.timefold.solver.core.impl.bavet.common.index.ElementPositionTracker;
import ai.timefold.solver.core.impl.bavet.common.tuple.AbstractTuple;

import org.jspecify.annotations.NullMarked;

@SuppressWarnings({ "rawtypes", "unchecked" })
@NullMarked
record ExistsCounterHandlePositionTracker<Tuple_ extends AbstractTuple>(PositionGetter<Tuple_> positionGetter,
        PositionClearer<Tuple_> positionClearer,
        PositionSetter<Tuple_> positionSetter)
        implements
            ElementPositionTracker<ExistsCounterHandle<Tuple_>> {

    private static final ExistsCounterHandlePositionTracker LEFT = new ExistsCounterHandlePositionTracker(
            tracker -> tracker.leftPosition,
            tracker -> {
                var result = tracker.leftPosition;
                tracker.leftPosition = -1;
                return result;
            },
            (tracker, position) -> {
                var oldValue = tracker.leftPosition;
                tracker.leftPosition = position;
                return oldValue;
            });
    private static final ExistsCounterHandlePositionTracker RIGHT = new ExistsCounterHandlePositionTracker(
            tracker -> tracker.rightPosition,
            tracker -> {
                var result = tracker.rightPosition;
                tracker.rightPosition = -1;
                return result;
            },
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
    public int getPosition(ExistsCounterHandle<Tuple_> element) {
        return positionGetter.apply(element);
    }

    @Override
    public int setPosition(ExistsCounterHandle<Tuple_> element, int position) {
        return positionSetter.apply(element, position);
    }

    @Override
    public int clearPosition(ExistsCounterHandle<Tuple_> element) {
        return positionClearer.apply(element);
    }

    @FunctionalInterface
    @NullMarked
    interface PositionGetter<Tuple_ extends AbstractTuple> {

        int apply(ExistsCounterHandle<Tuple_> tracker);

    }

    @FunctionalInterface
    @NullMarked
    interface PositionClearer<Tuple_ extends AbstractTuple> {

        int apply(ExistsCounterHandle<Tuple_> tracker);

    }

    @FunctionalInterface
    @NullMarked
    interface PositionSetter<Tuple_ extends AbstractTuple> {

        int apply(ExistsCounterHandle<Tuple_> tracker, int position);

    }

}
