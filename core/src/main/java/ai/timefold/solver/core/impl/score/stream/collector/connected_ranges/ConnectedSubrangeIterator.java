package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

final class ConnectedSubrangeIterator<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements Iterator<ConnectedRangeImpl<Interval_, Point_, Difference_>> {
    // TODO: Make this incremental by only checking between the interval's start and end points
    private final NavigableSet<IntervalSplitPoint<Interval_, Point_>> splitPointSet;
    private final BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction;
    private final IntervalSplitPoint<Interval_, Point_> endSplitPoint;
    private IntervalSplitPoint<Interval_, Point_> current;

    public ConnectedSubrangeIterator(NavigableSet<IntervalSplitPoint<Interval_, Point_>> splitPointSet,
            IntervalSplitPoint<Interval_, Point_> startSplitPoint,
            IntervalSplitPoint<Interval_, Point_> endSplitPoint,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        this.splitPointSet = splitPointSet;
        this.current = getStart(startSplitPoint);
        this.endSplitPoint = endSplitPoint;
        this.differenceFunction = differenceFunction;
    }

    private IntervalSplitPoint<Interval_, Point_>
            getStart(IntervalSplitPoint<Interval_, Point_> start) {
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
    public ConnectedRangeImpl<Interval_, Point_, Difference_> next() {
        if (current == null) {
            throw new NoSuchElementException();
        }
        IntervalSplitPoint<Interval_, Point_> start = current;
        IntervalSplitPoint<Interval_, Point_> end;
        int activeIntervals = 0;
        int minimumOverlap = Integer.MAX_VALUE;
        int maximumOverlap = Integer.MIN_VALUE;
        int count = 0;
        boolean anyOverlap = false;
        do {
            count += current.intervalsStartingAtSplitPointSet.size();
            activeIntervals +=
                    current.intervalsStartingAtSplitPointSet.size() - current.intervalsEndingAtSplitPointSet.size();
            if (activeIntervals > 0) {
                minimumOverlap = Math.min(minimumOverlap, activeIntervals);
                maximumOverlap = Math.max(maximumOverlap, activeIntervals);
                if (activeIntervals > 1) {
                    anyOverlap = true;
                }
            }
            current = splitPointSet.higher(current);
        } while (activeIntervals > 0 && current != null);

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
