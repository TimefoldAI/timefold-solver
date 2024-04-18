package ai.timefold.solver.core.impl.score.stream.collector.uni;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.common.ConcurrentUsageInfo;
import ai.timefold.solver.core.impl.score.stream.collector.ConcurrentUsageCalculator;

final class ConcurrentUsageUniConstraintCollector<A, Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        extends
        ObjectCalculatorUniCollector<A, Interval_, ConcurrentUsageInfo<Interval_, Point_, Difference_>, ConcurrentUsageCalculator<Interval_, Point_, Difference_>> {

    private final Function<? super Interval_, ? extends Point_> startMap;
    private final Function<? super Interval_, ? extends Point_> endMap;
    private final BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction;

    public ConcurrentUsageUniConstraintCollector(Function<? super A, ? extends Interval_> mapper,
            Function<? super Interval_, ? extends Point_> startMap, Function<? super Interval_, ? extends Point_> endMap,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        super(mapper);
        this.startMap = startMap;
        this.endMap = endMap;
        this.differenceFunction = differenceFunction;
    }

    @Override
    public Supplier<ConcurrentUsageCalculator<Interval_, Point_, Difference_>> supplier() {
        return () -> new ConcurrentUsageCalculator<>(startMap, endMap, differenceFunction);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ConcurrentUsageUniConstraintCollector<?, ?, ?, ?> that))
            return false;
        if (!super.equals(o))
            return false;
        return Objects.equals(startMap, that.startMap) && Objects.equals(endMap,
                that.endMap) && Objects.equals(differenceFunction, that.differenceFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), startMap, endMap, differenceFunction);
    }
}
