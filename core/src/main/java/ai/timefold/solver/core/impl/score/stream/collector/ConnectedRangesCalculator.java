package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.impl.score.stream.collector.connected_ranges.ConnectedRangeTracker;

public final class ConnectedRangesCalculator<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements ObjectCalculator<Interval_, ConnectedRangeChain<Interval_, Point_, Difference_>> {

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
    public void insert(Interval_ result) {
        context.add(context.getRange(result));
    }

    @Override
    public void retract(Interval_ result) {
        context.remove(context.getRange(result));
    }

    @Override
    public ConnectedRangeChain<Interval_, Point_, Difference_> result() {
        return context.getConnectedRangeChain();
    }

}
