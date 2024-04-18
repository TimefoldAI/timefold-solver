package ai.timefold.solver.core.impl.score.stream.collector.tri;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.function.TriFunction;
import ai.timefold.solver.core.api.score.stream.common.ConcurrentUsageInfo;
import ai.timefold.solver.core.impl.score.stream.collector.ConcurrentUsageCalculator;

final class ConcurrentUsageTriConstraintCollector<A, B, C, Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        extends
        ObjectCalculatorTriCollector<A, B, C, Interval_, ConcurrentUsageInfo<Interval_, Point_, Difference_>, ConcurrentUsageCalculator<Interval_, Point_, Difference_>> {

    private final Function<? super Interval_, ? extends Point_> startMap;
    private final Function<? super Interval_, ? extends Point_> endMap;
    private final BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction;

    public ConcurrentUsageTriConstraintCollector(TriFunction<? super A, ? super B, ? super C, ? extends Interval_> mapper,
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
        if (!(o instanceof ConcurrentUsageTriConstraintCollector<?, ?, ?, ?, ?, ?> that))
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
