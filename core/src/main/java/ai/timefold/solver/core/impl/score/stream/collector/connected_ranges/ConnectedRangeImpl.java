package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRange;

import org.jspecify.annotations.NonNull;

final class ConnectedRangeImpl<Range_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements ConnectedRange<Range_, Point_, Difference_> {

    private final NavigableSet<RangeSplitPoint<Range_, Point_>> splitPointSet;
    private final BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction;
    private RangeSplitPoint<Range_, Point_> startSplitPoint;
    private RangeSplitPoint<Range_, Point_> endSplitPoint;

    private int count;
    private int minimumOverlap;
    private int maximumOverlap;
    private boolean hasOverlap;
    private int cachedHashCode;
    private boolean hashCodeDirty = true;

    ConnectedRangeImpl(NavigableSet<RangeSplitPoint<Range_, Point_>> splitPointSet,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction,
            RangeSplitPoint<Range_, Point_> start,
            RangeSplitPoint<Range_, Point_> end, int count,
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

    static <Range_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
            ConnectedRangeImpl<Range_, Point_, Difference_>
            getConnectedRangeStartingAt(NavigableSet<RangeSplitPoint<Range_, Point_>> splitPointSet,
                    BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction,
                    RangeSplitPoint<Range_, Point_> start) {
        return new ConnectedSubrangeIterator<>(splitPointSet, start, splitPointSet.last(), differenceFunction).next();
    }

    RangeSplitPoint<Range_, Point_> getStartSplitPoint() {
        return startSplitPoint;
    }

    RangeSplitPoint<Range_, Point_> getEndSplitPoint() {
        return endSplitPoint;
    }

    void addRange(Range<Range_, Point_> range) {
        if (range.getEndSplitPoint().compareTo(getStartSplitPoint()) > 0
                && range.getStartSplitPoint().compareTo(getEndSplitPoint()) < 0) {
            hasOverlap = true;
        }
        if (range.getStartSplitPoint().compareTo(startSplitPoint) < 0) {
            startSplitPoint = splitPointSet.floor(range.getStartSplitPoint());
        }
        if (range.getEndSplitPoint().compareTo(endSplitPoint) > 0) {
            endSplitPoint = splitPointSet.ceiling(range.getEndSplitPoint());
        }
        minimumOverlap = -1;
        maximumOverlap = -1;
        hashCodeDirty = true;
        count++;
    }

    Iterable<ConnectedRangeImpl<Range_, Point_, Difference_>> getNewConnectedRanges(
            final NavigableSet<RangeSplitPoint<Range_, Point_>> newSplitPointSet) {
        return () -> new ConnectedSubrangeIterator<>(newSplitPointSet, startSplitPoint, endSplitPoint, differenceFunction);
    }

    void mergeConnectedRange(ConnectedRangeImpl<Range_, Point_, Difference_> laterConnectedRange) {
        if (endSplitPoint.compareTo(laterConnectedRange.startSplitPoint) > 0) {
            hasOverlap = true;
        }
        if (endSplitPoint.compareTo(laterConnectedRange.endSplitPoint) < 0) {
            endSplitPoint = laterConnectedRange.endSplitPoint;
        }
        count += laterConnectedRange.count;
        minimumOverlap = -1;
        maximumOverlap = -1;
        hashCodeDirty = true;
        hasOverlap |= laterConnectedRange.hasOverlap;
    }

    @Override
    public Iterator<Range_> iterator() {
        return new ContainedRangeIterator<>(splitPointSet.subSet(startSplitPoint, true, endSplitPoint, true));
    }

    @Override
    public int getContainedRangeCount() {
        return count;
    }

    @Override
    public boolean hasOverlap() {
        return hasOverlap;
    }

    private void recalculateMinimumAndMaximumOverlap() {
        var current = startSplitPoint;
        var activeRangeCount = 0;
        minimumOverlap = Integer.MAX_VALUE;
        maximumOverlap = Integer.MIN_VALUE;
        do {
            activeRangeCount += current.rangesStartingAtSplitPointSet.size() - current.rangesEndingAtSplitPointSet.size();
            if (activeRangeCount > 0) {
                minimumOverlap = Math.min(minimumOverlap, activeRangeCount);
                maximumOverlap = Math.max(maximumOverlap, activeRangeCount);
            }
            current = splitPointSet.higher(current);
        } while (activeRangeCount > 0 && current != null);
    }

    @Override
    public int getMinimumOverlap() {
        if (minimumOverlap == -1) {
            recalculateMinimumAndMaximumOverlap();
        }
        return minimumOverlap;
    }

    @Override
    public int getMaximumOverlap() {
        if (maximumOverlap == -1) {
            recalculateMinimumAndMaximumOverlap();
        }
        return maximumOverlap;
    }

    @Override
    public @NonNull Point_ getStart() {
        return startSplitPoint.splitPoint;
    }

    @Override
    public @NonNull Point_ getEnd() {
        return endSplitPoint.splitPoint;
    }

    @Override
    public @NonNull Difference_ getLength() {
        return differenceFunction.apply(startSplitPoint.splitPoint, endSplitPoint.splitPoint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        // splitPointSet is the tracker's full, problem-wide split-point set,
        // shared by reference across every ConnectedRangeImpl built from it.
        // It's compared here for correctness, but deliberately excluded from hashCode():
        // equals() gets it for free because both sides are almost always the same reference
        // (Objects.equals short-circuits on ==),
        // whereas hashCode() has no such shortcut
        // and would have to walk and hash every split point in the whole problem,
        // not just this range's.
        // The checks are ordered from cheapest to most expensive.
        // Min/max overlap are excluded as they are derived from the split points.
        return o instanceof ConnectedRangeImpl<?, ?, ?> that
                && hasOverlap == that.hasOverlap
                && count == that.count
                && Objects.equals(startSplitPoint, that.startSplitPoint)
                && Objects.equals(endSplitPoint, that.endSplitPoint)
                && Objects.equals(splitPointSet, that.splitPointSet);
    }

    @Override
    public int hashCode() {
        if (hashCodeDirty) {
            // Hand-rolled instead of Objects.hash(...):
            // that boxes every argument and allocates a varargs array per call,
            // on what is already a hot path.
            var result = Boolean.hashCode(hasOverlap);
            result = 31 * result + count;
            result = 31 * result + startSplitPoint.hashCode();
            result = 31 * result + endSplitPoint.hashCode();
            cachedHashCode = result;
            hashCodeDirty = false;
        }
        return cachedHashCode;
    }

    @Override
    public String toString() {
        return "ConnectedRange {" +
                "start=" + startSplitPoint +
                ", end=" + endSplitPoint +
                ", count=" + count +
                ", hasOverlap=" + hasOverlap +
                '}';
    }

}
