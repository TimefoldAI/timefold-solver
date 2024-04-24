package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class RangeSplitPoint<Range_, Point_ extends Comparable<Point_>>
        implements Comparable<RangeSplitPoint<Range_, Point_>> {
    final Point_ splitPoint;
    Map<Range_, Integer> startpointRangeToCountMap;
    Map<Range_, Integer> endpointRangeToCountMap;
    TreeMultiSet<Range<Range_, Point_>> rangesStartingAtSplitPointSet;
    TreeMultiSet<Range<Range_, Point_>> rangesEndingAtSplitPointSet;

    public RangeSplitPoint(Point_ splitPoint) {
        this.splitPoint = splitPoint;
    }

    protected void createCollections() {
        startpointRangeToCountMap = new IdentityHashMap<>();
        endpointRangeToCountMap = new IdentityHashMap<>();
        rangesStartingAtSplitPointSet = new TreeMultiSet<>(
                Comparator.<Range<Range_, Point_>, Point_> comparing(Range::getEnd)
                        .thenComparingInt(range -> System.identityHashCode(range.getValue())));
        rangesEndingAtSplitPointSet = new TreeMultiSet<>(
                Comparator.<Range<Range_, Point_>, Point_> comparing(Range::getStart)
                        .thenComparingInt(range -> System.identityHashCode(range.getValue())));
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
        return rangesStartingAtSplitPointSet.stream()
                .map(Range::getValue)
                .iterator();
    }

    public boolean isEmpty() {
        return rangesStartingAtSplitPointSet.isEmpty() && rangesEndingAtSplitPointSet.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RangeSplitPoint<?, ?> that = (RangeSplitPoint<?, ?>) o;
        return splitPoint.equals(that.splitPoint);
    }

    public boolean isBefore(RangeSplitPoint<Range_, Point_> other) {
        return compareTo(other) < 0;
    }

    public boolean isAfter(RangeSplitPoint<Range_, Point_> other) {
        return compareTo(other) > 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(splitPoint);
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
