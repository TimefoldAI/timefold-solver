package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

final class ConnectedSubrangeIterator<Range_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements Iterator<ConnectedRangeImpl<Range_, Point_, Difference_>> {
    // TODO: Make this incremental by only checking between the range's start and end points
    private final NavigableSet<RangeSplitPoint<Range_, Point_>> splitPointSet;
    private final BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction;
    private final RangeSplitPoint<Range_, Point_> endSplitPoint;
    private RangeSplitPoint<Range_, Point_> current;

    public ConnectedSubrangeIterator(NavigableSet<RangeSplitPoint<Range_, Point_>> splitPointSet,
            RangeSplitPoint<Range_, Point_> startSplitPoint,
            RangeSplitPoint<Range_, Point_> endSplitPoint,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        this.splitPointSet = splitPointSet;
        this.current = getStart(startSplitPoint);
        this.endSplitPoint = endSplitPoint;
        this.differenceFunction = differenceFunction;
    }

    private RangeSplitPoint<Range_, Point_>
            getStart(RangeSplitPoint<Range_, Point_> start) {
        while (start != null && start.isEmpty()) {
            start = splitPointSet.higher(start);
        }
        return start;
    }

    @Override
    public boolean hasNext() {
        return current != null && current.compareTo(endSplitPoint) <= 0 && !splitPointSet.isEmpty();
    }

    @Override
    public ConnectedRangeImpl<Range_, Point_, Difference_> next() {
        if (current == null) {
            throw new NoSuchElementException();
        }
        RangeSplitPoint<Range_, Point_> start = current;
        RangeSplitPoint<Range_, Point_> end;
        int activeRangeCount = 0;
        int minimumOverlap = Integer.MAX_VALUE;
        int maximumOverlap = Integer.MIN_VALUE;
        int count = 0;
        boolean anyOverlap = false;
        do {
            count += current.rangesStartingAtSplitPointSet.size();
            activeRangeCount +=
                    current.rangesStartingAtSplitPointSet.size() - current.rangesEndingAtSplitPointSet.size();
            if (activeRangeCount > 0) {
                minimumOverlap = Math.min(minimumOverlap, activeRangeCount);
                maximumOverlap = Math.max(maximumOverlap, activeRangeCount);
                if (activeRangeCount > 1) {
                    anyOverlap = true;
                }
            }
            current = splitPointSet.higher(current);
        } while (activeRangeCount > 0 && current != null);

        if (current != null) {
            end = splitPointSet.lower(current);
            current = getStart(current);
        } else {
            end = splitPointSet.last();
        }

        return new ConnectedRangeImpl<>(splitPointSet, differenceFunction, start, end, count,
                minimumOverlap, maximumOverlap, anyOverlap);
    }
}
