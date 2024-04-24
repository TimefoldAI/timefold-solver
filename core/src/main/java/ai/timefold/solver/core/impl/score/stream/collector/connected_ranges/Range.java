package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import java.util.function.Function;

public final class Range<Range_, Point_ extends Comparable<Point_>> {
    private final Range_ value;
    private final RangeSplitPoint<Range_, Point_> startSplitPoint;
    private final RangeSplitPoint<Range_, Point_> endSplitPoint;

    public Range(Range_ value, Function<? super Range_, ? extends Point_> startMapping,
            Function<? super Range_, ? extends Point_> endMapping) {
        this.value = value;
        var start = startMapping.apply(value);
        var end = endMapping.apply(value);
        this.startSplitPoint = new RangeSplitPoint<>(start);
        this.endSplitPoint = (start == end) ? this.startSplitPoint : new RangeSplitPoint<>(end);
    }

    public Range_ getValue() {
        return value;
    }

    public Point_ getStart() {
        return startSplitPoint.splitPoint;
    }

    public Point_ getEnd() {
        return endSplitPoint.splitPoint;
    }

    public RangeSplitPoint<Range_, Point_> getStartSplitPoint() {
        return startSplitPoint;
    }

    public RangeSplitPoint<Range_, Point_> getEndSplitPoint() {
        return endSplitPoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Range<?, ?> that = (Range<?, ?>) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(value);
    }

    @Override
    public String toString() {
        return "Range{" +
                "value=" + value +
                ", start=" + getStart() +
                ", end=" + getEnd() +
                '}';
    }
}
