package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.impl.score.stream.collector.connected_ranges.ConnectedRangeTracker;
import ai.timefold.solver.core.impl.score.stream.collector.connected_ranges.Range;

public final class ConnectedRangesCalculator<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements
        ObjectCalculator<Interval_, ConnectedRangeChain<Interval_, Point_, Difference_>, Range<Interval_, Point_>> {

    private final ConnectedRangeTracker<Interval_, Point_, Difference_> context;

    public ConnectedRangesCalculator(Function<? super Interval_, ? extends Point_> startMap,
            Function<? super Interval_, ? extends Point_> endMap,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        this.context = new ConnectedRangeTracker<>(
                startMap,
                endMap,
                differenceFunction);
    }

    @Override
    public Range<Interval_, Point_> insert(Interval_ result) {
        final var saved = context.getRange(result);
        context.add(saved);
        return saved;
    }

    @Override
    public void retract(Range<Interval_, Point_> range) {
        context.remove(range);
    }

    @Override
    public ConnectedRangeChain<Interval_, Point_, Difference_> result() {
        return context.getConnectedRangeChain();
    }

}
