package ai.timefold.solver.core.impl.score.stream.collector.bi;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.impl.score.stream.collector.ConnectedRangesCalculator;
import ai.timefold.solver.core.impl.score.stream.collector.connected_ranges.Range;

final class ConnectedRangesBiConstraintCollector<A, B, Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        extends
        ObjectCalculatorBiCollector<A, B, Interval_, ConnectedRangeChain<Interval_, Point_, Difference_>, Range<Interval_, Point_>, ConnectedRangesCalculator<Interval_, Point_, Difference_>> {

    private final Function<? super Interval_, ? extends Point_> startMap;
    private final Function<? super Interval_, ? extends Point_> endMap;
    private final BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction;

    public ConnectedRangesBiConstraintCollector(BiFunction<? super A, ? super B, ? extends Interval_> mapper,
            Function<? super Interval_, ? extends Point_> startMap, Function<? super Interval_, ? extends Point_> endMap,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        super(mapper);
        this.startMap = startMap;
        this.endMap = endMap;
        this.differenceFunction = differenceFunction;
    }

    @Override
    public Supplier<ConnectedRangesCalculator<Interval_, Point_, Difference_>> supplier() {
        return () -> new ConnectedRangesCalculator<>(startMap, endMap, differenceFunction);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ConnectedRangesBiConstraintCollector<?, ?, ?, ?, ?> that))
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
