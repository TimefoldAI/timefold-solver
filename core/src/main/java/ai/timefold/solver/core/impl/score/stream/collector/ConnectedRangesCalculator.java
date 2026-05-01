package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.impl.score.stream.collector.connected_ranges.ConnectedRangeTracker;
import ai.timefold.solver.core.impl.score.stream.collector.connected_ranges.Range;

public final class ConnectedRangesCalculator<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements ObjectCalculator<Interval_> {

    public static final class State<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>> {
        private final ConnectedRangeTracker<Interval_, Point_, Difference_> context;

        public State(Function<? super Interval_, ? extends Point_> startMap,
                Function<? super Interval_, ? extends Point_> endMap,
                BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
            this.context = new ConnectedRangeTracker<>(startMap, endMap, differenceFunction);
        }

        public ConnectedRangeChain<Interval_, Point_, Difference_> result() {
            return context.getConnectedRangeChain();
        }
    }

    private final State<Interval_, Point_, Difference_> state;
    private Range<Interval_, Point_> cachedRange;

    public ConnectedRangesCalculator(State<Interval_, Point_, Difference_> state) {
        this.state = state;
    }

    @Override
    public void insert(Interval_ result) {
        final var saved = state.context.getRange(result);
        cachedRange = saved;
        state.context.add(saved);
    }

    @Override
    public void update(Interval_ input) {
        state.context.remove(cachedRange);
        cachedRange = state.context.getRange(input);
        state.context.add(cachedRange);
    }

    @Override
    public void retract() {
        state.context.remove(cachedRange);
    }
}
