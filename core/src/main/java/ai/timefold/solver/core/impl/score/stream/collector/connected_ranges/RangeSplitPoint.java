package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class RangeSplitPoint<Range_, Point_ extends Comparable<Point_>>
        implements Comparable<RangeSplitPoint<Range_, Point_>> {

    // Reuse these instances in createCollections().
    @SuppressWarnings("rawtypes")
    private static final Comparator COMPARATOR_START = comparator(Range::getStart);
    @SuppressWarnings("rawtypes")
    private static final Comparator COMPARATOR_END = comparator(Range::getEnd);

    private static <Range_, Point_ extends Comparable<Point_>> Comparator<Range<Range_, Point_>>
            comparator(Function<Range<Range_, Point_>, Point_> function) {
        return Comparator.comparing(function)
                .thenComparingInt(range -> System.identityHashCode(range.getValue()));
    }

    final Point_ splitPoint;
    Map<Range_, Integer> startpointRangeToCountMap;
    Map<Range_, Integer> endpointRangeToCountMap;
    TreeMultiSet<Range<Range_, Point_>> rangesStartingAtSplitPointSet;
    TreeMultiSet<Range<Range_, Point_>> rangesEndingAtSplitPointSet;

    public RangeSplitPoint(Point_ splitPoint) {
        this.splitPoint = splitPoint;
    }

    protected void createCollections() {
        // Almost always holds a single entry (one range starting/ending at this exact point);
        // avoid IdentityHashMap's default 64-slot table for what's typically a 1-entry map.
        startpointRangeToCountMap = new IdentityHashMap<>(1);
        endpointRangeToCountMap = new IdentityHashMap<>(1);
        rangesStartingAtSplitPointSet = new TreeMultiSet<>(COMPARATOR_END);
        rangesEndingAtSplitPointSet = new TreeMultiSet<>(COMPARATOR_START);
    }

    public boolean addRangeStartingAtSplitPoint(Range<Range_, Point_> range) {
        startpointRangeToCountMap.merge(range.getValue(), 1, Integer::sum);
        return rangesStartingAtSplitPointSet.add(range);
    }

    public void removeRangeStartingAtSplitPoint(Range<Range_, Point_> range) {
        Integer newCount = startpointRangeToCountMap.computeIfPresent(range.getValue(), (key, count) -> {
            if (count > 1) {
                return count - 1;
            }
            return null;
        });
        if (null == newCount) {
            rangesStartingAtSplitPointSet.remove(range);
        }
    }

    public boolean addRangeEndingAtSplitPoint(Range<Range_, Point_> range) {
        endpointRangeToCountMap.merge(range.getValue(), 1, Integer::sum);
        return rangesEndingAtSplitPointSet.add(range);
    }

    public void removeRangeEndingAtSplitPoint(Range<Range_, Point_> range) {
        Integer newCount = endpointRangeToCountMap.computeIfPresent(range.getValue(), (key, count) -> {
            if (count > 1) {
                return count - 1;
            }
            return null;
        });
        if (null == newCount) {
            rangesEndingAtSplitPointSet.remove(range);
        }
    }

    public boolean containsRangeStarting(Range<Range_, Point_> range) {
        return rangesStartingAtSplitPointSet.contains(range);
    }

    public boolean containsRangeEnding(Range<Range_, Point_> range) {
        return rangesEndingAtSplitPointSet.contains(range);
    }

    public Iterator<Range_> getValuesStartingFromSplitPointIterator() {
        var iterator = rangesStartingAtSplitPointSet.iterator();
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Range_ next() {
                return iterator.next().getValue();
            }
        };
    }

    public boolean isEmpty() {
        return rangesStartingAtSplitPointSet.isEmpty() && rangesEndingAtSplitPointSet.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof RangeSplitPoint<?, ?> other
                && splitPoint.equals(other.splitPoint);
    }

    public boolean isBefore(RangeSplitPoint<Range_, Point_> other) {
        return compareTo(other) < 0;
    }

    public boolean isAfter(RangeSplitPoint<Range_, Point_> other) {
        return compareTo(other) > 0;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(splitPoint);
    }

    @Override
    public int compareTo(RangeSplitPoint<Range_, Point_> other) {
        return splitPoint.compareTo(other.splitPoint);
    }

    @Override
    public String toString() {
        return splitPoint.toString();
    }
}
