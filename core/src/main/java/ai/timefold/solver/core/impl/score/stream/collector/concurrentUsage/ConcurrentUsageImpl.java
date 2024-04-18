package ai.timefold.solver.core.impl.score.stream.collector.concurrentUsage;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.stream.common.ConcurrentUsage;

final class ConcurrentUsageImpl<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements ConcurrentUsage<Interval_, Point_, Difference_> {

    private final NavigableSet<IntervalSplitPoint<Interval_, Point_>> splitPointSet;
    private final BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction;
    private IntervalSplitPoint<Interval_, Point_> startSplitPoint;
    private IntervalSplitPoint<Interval_, Point_> endSplitPoint;

    private int count;
    private int minimumOverlap;
    private int maximumOverlap;
    private boolean hasOverlap;

    ConcurrentUsageImpl(NavigableSet<IntervalSplitPoint<Interval_, Point_>> splitPointSet,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction,
            IntervalSplitPoint<Interval_, Point_> start) {
        if (start == null) {
            throw new IllegalArgumentException("start (" + start + ") is null");
        }
        if (differenceFunction == null) {
            throw new IllegalArgumentException("differenceFunction (" + differenceFunction + ") is null");
        }
        this.splitPointSet = splitPointSet;
        this.startSplitPoint = start;
        this.endSplitPoint = start;
        this.differenceFunction = differenceFunction;
        this.count = 0;
        this.minimumOverlap = Integer.MAX_VALUE;
        this.maximumOverlap = Integer.MIN_VALUE;
        var activeIntervals = 0;
        var anyOverlap = false;
        var current = start;
        do {
            this.count += current.intervalsStartingAtSplitPointSet.size();
            activeIntervals += current.intervalsStartingAtSplitPointSet.size() - current.intervalsEndingAtSplitPointSet.size();
            if (activeIntervals > 0) {
                minimumOverlap = Math.min(minimumOverlap, activeIntervals);
                maximumOverlap = Math.max(maximumOverlap, activeIntervals);
                if (activeIntervals > 1) {
                    anyOverlap = true;
                }
            }
            current = splitPointSet.higher(current);
        } while (activeIntervals > 0 && current != null);
        this.hasOverlap = anyOverlap;
        this.endSplitPoint = (current != null) ? splitPointSet.lower(current) : splitPointSet.last();
    }

    ConcurrentUsageImpl(NavigableSet<IntervalSplitPoint<Interval_, Point_>> splitPointSet,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction,
            IntervalSplitPoint<Interval_, Point_> start,
            IntervalSplitPoint<Interval_, Point_> end, int count,
            int minimumOverlap, int maximumOverlap,
            boolean hasOverlap) {
        this.splitPointSet = splitPointSet;
        this.startSplitPoint = start;
        this.endSplitPoint = end;
        this.differenceFunction = differenceFunction;
        this.count = count;
        this.minimumOverlap = minimumOverlap;
        this.maximumOverlap = maximumOverlap;
        this.hasOverlap = hasOverlap;
    }

    IntervalSplitPoint<Interval_, Point_> getStartSplitPoint() {
        return startSplitPoint;
    }

    IntervalSplitPoint<Interval_, Point_> getEndSplitPoint() {
        return endSplitPoint;
    }

    void addInterval(Interval<Interval_, Point_> interval) {
        if (interval.getEndSplitPoint().compareTo(getStartSplitPoint()) > 0
                && interval.getStartSplitPoint().compareTo(getEndSplitPoint()) < 0) {
            hasOverlap = true;
        }
        if (interval.getStartSplitPoint().compareTo(startSplitPoint) < 0) {
            startSplitPoint = splitPointSet.floor(interval.getStartSplitPoint());
        }
        if (interval.getEndSplitPoint().compareTo(endSplitPoint) > 0) {
            endSplitPoint = splitPointSet.ceiling(interval.getEndSplitPoint());
        }
        minimumOverlap = -1;
        maximumOverlap = -1;
        count++;
    }

    Iterable<ConcurrentUsageImpl<Interval_, Point_, Difference_>> removeInterval(Interval<Interval_, Point_> interval) {
        return IntervalClusterIterator::new;
    }

    void mergeIntervalCluster(ConcurrentUsageImpl<Interval_, Point_, Difference_> laterIntervalCluster) {
        if (endSplitPoint.compareTo(laterIntervalCluster.startSplitPoint) > 0) {
            hasOverlap = true;
        }
        if (endSplitPoint.compareTo(laterIntervalCluster.endSplitPoint) < 0) {
            endSplitPoint = laterIntervalCluster.endSplitPoint;
        }
        count += laterIntervalCluster.count;
        minimumOverlap = -1;
        maximumOverlap = -1;
        hasOverlap |= laterIntervalCluster.hasOverlap;
    }

    @Override
    public Iterator<Interval_> iterator() {
        return new IntervalTreeIterator<>(splitPointSet.subSet(startSplitPoint, true, endSplitPoint, true));
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public boolean hasOverlap() {
        return hasOverlap;
    }

    private void recalculateMinimumAndMaximumOverlap() {
        var current = startSplitPoint;
        var activeIntervals = 0;
        minimumOverlap = Integer.MAX_VALUE;
        maximumOverlap = Integer.MIN_VALUE;
        do {
            activeIntervals += current.intervalsStartingAtSplitPointSet.size() - current.intervalsEndingAtSplitPointSet.size();
            if (activeIntervals > 0) {
                minimumOverlap = Math.min(minimumOverlap, activeIntervals);
                maximumOverlap = Math.max(maximumOverlap, activeIntervals);
            }
            current = splitPointSet.higher(current);
        } while (activeIntervals > 0 && current != null);
    }

    @Override
    public int getMinimumConcurrentUsage() {
        if (minimumOverlap == -1) {
            recalculateMinimumAndMaximumOverlap();
        }
        return minimumOverlap;
    }

    @Override
    public int getMaximumConcurrentUsage() {
        if (maximumOverlap == -1) {
            recalculateMinimumAndMaximumOverlap();
        }
        return maximumOverlap;
    }

    @Override
    public Point_ getStart() {
        return startSplitPoint.splitPoint;
    }

    @Override
    public Point_ getEnd() {
        return endSplitPoint.splitPoint;
    }

    @Override
    public Difference_ getLength() {
        return differenceFunction.apply(startSplitPoint.splitPoint, endSplitPoint.splitPoint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ConcurrentUsageImpl<?, ?, ?> that))
            return false;
        return count == that.count &&
                getMinimumConcurrentUsage() == that.getMinimumConcurrentUsage()
                && getMaximumConcurrentUsage() == that.getMaximumConcurrentUsage()
                && hasOverlap == that.hasOverlap && Objects.equals(
                        splitPointSet, that.splitPointSet)
                && Objects.equals(startSplitPoint,
                        that.startSplitPoint)
                && Objects.equals(endSplitPoint, that.endSplitPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(splitPointSet, startSplitPoint, endSplitPoint, count,
                getMinimumConcurrentUsage(), getMaximumConcurrentUsage(), hasOverlap);
    }

    @Override
    public String toString() {
        return "ConcurrentUsage {" +
                "start=" + startSplitPoint +
                ", end=" + endSplitPoint +
                ", count=" + count +
                ", minimumOverlap=" + getMinimumConcurrentUsage() +
                ", maximumOverlap=" + getMaximumConcurrentUsage() +
                ", hasOverlap=" + hasOverlap +
                ", set=" + splitPointSet +
                '}';
    }

    // TODO: Make this incremental by only checking between the interval's start and end points
    private final class IntervalClusterIterator
            implements Iterator<ConcurrentUsageImpl<Interval_, Point_, Difference_>> {

        private IntervalSplitPoint<Interval_, Point_> current = getStart(startSplitPoint);

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
        public ConcurrentUsageImpl<Interval_, Point_, Difference_> next() {
            IntervalSplitPoint<Interval_, Point_> start = current;
            IntervalSplitPoint<Interval_, Point_> end;
            int activeIntervals = 0;
            minimumOverlap = Integer.MAX_VALUE;
            maximumOverlap = Integer.MIN_VALUE;
            count = 0;
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
            hasOverlap = anyOverlap;

            return new ConcurrentUsageImpl<>(splitPointSet, differenceFunction, start, end, count,
                    minimumOverlap, maximumOverlap, hasOverlap);
        }
    }
}
