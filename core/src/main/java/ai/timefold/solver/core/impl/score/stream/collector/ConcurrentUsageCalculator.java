package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.common.ConcurrentUsageInfo;
import ai.timefold.solver.core.impl.score.stream.collector.concurrentUsage.IntervalTree;

public final class ConcurrentUsageCalculator<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements ObjectCalculator<Interval_, ConcurrentUsageInfo<Interval_, Point_, Difference_>> {

    private final IntervalTree<Interval_, Point_, Difference_> context;

    public ConcurrentUsageCalculator(Function<? super Interval_, ? extends Point_> startMap,
            Function<? super Interval_, ? extends Point_> endMap,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        this.context = new IntervalTree<>(
                startMap,
                endMap,
                differenceFunction);
    }

    @Override
    public void insert(Interval_ result) {
        context.add(context.getInterval(result));
    }

    @Override
    public void retract(Interval_ result) {
        context.remove(context.getInterval(result));
    }

    @Override
    public ConcurrentUsageInfo<Interval_, Point_, Difference_> result() {
        return context.getConsecutiveIntervalData();
    }

}
