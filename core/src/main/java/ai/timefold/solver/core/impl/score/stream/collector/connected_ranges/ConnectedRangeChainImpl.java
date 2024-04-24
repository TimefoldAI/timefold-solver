package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRange;
import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;
import ai.timefold.solver.core.api.score.stream.common.RangeGap;

public final class ConnectedRangeChainImpl<Range_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements ConnectedRangeChain<Range_, Point_, Difference_> {

    private final NavigableMap<RangeSplitPoint<Range_, Point_>, ConnectedRangeImpl<Range_, Point_, Difference_>> startSplitPointToConnectedRange;
    private final NavigableSet<RangeSplitPoint<Range_, Point_>> splitPointSet;
    private final NavigableMap<RangeSplitPoint<Range_, Point_>, RangeGapImpl<Range_, Point_, Difference_>> startSplitPointToNextGap;
    private final BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction;

    public ConnectedRangeChainImpl(NavigableSet<RangeSplitPoint<Range_, Point_>> splitPointSet,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        this.startSplitPointToConnectedRange = new TreeMap<>();
        this.startSplitPointToNextGap = new TreeMap<>();
        this.splitPointSet = splitPointSet;
        this.differenceFunction = differenceFunction;
    }

    void addRange(Range<Range_, Point_> range) {
        var intersectedConnectedRangeMap = startSplitPointToConnectedRange
                .subMap(Objects.requireNonNullElseGet(startSplitPointToConnectedRange.floorKey(range.getStartSplitPoint()),
                        range::getStartSplitPoint), true, range.getEndSplitPoint(), true);

        // Case: the connected range before this range does not intersect this range
        if (!intersectedConnectedRangeMap.isEmpty()
                && intersectedConnectedRangeMap.firstEntry().getValue().getEndSplitPoint()
                        .isBefore(range.getStartSplitPoint())) {
            // Get the tail map after the first connected range
            intersectedConnectedRangeMap = intersectedConnectedRangeMap.subMap(intersectedConnectedRangeMap.firstKey(),
                    false, intersectedConnectedRangeMap.lastKey(), true);
        }

        if (intersectedConnectedRangeMap.isEmpty()) {
            // Range does not intersect anything
            // Ex:
            //     -----
            //----       -----
            createNewConnectedRange(range);
            return;
        }

        // Range intersect at least one connected range
        // Ex:
        //      -----------------
        //  ------  ------  ---   ----
        var firstIntersectedConnectedRange = intersectedConnectedRangeMap.firstEntry().getValue();
        var oldStartSplitPoint = firstIntersectedConnectedRange.getStartSplitPoint();
        firstIntersectedConnectedRange.addRange(range);

        // Merge all the intersected connected range into the first intersected
        // connected range
        intersectedConnectedRangeMap.tailMap(oldStartSplitPoint, false).values()
                .forEach(firstIntersectedConnectedRange::mergeConnectedRange);

        // Remove all the intersected connected ranges after the first intersected
        // one, since they are now merged in the first
        intersectedConnectedRangeMap.tailMap(oldStartSplitPoint, false).clear();
        removeSpannedGapsAndUpdateIntersectedGaps(range, firstIntersectedConnectedRange);

        // If the first intersected connected range starts after the range,
        // we need to make the range start point the key for this connected range
        // in the map
        if (oldStartSplitPoint.isAfter(firstIntersectedConnectedRange.getStartSplitPoint())) {
            startSplitPointToConnectedRange.remove(oldStartSplitPoint);
            startSplitPointToConnectedRange.put(firstIntersectedConnectedRange.getStartSplitPoint(),
                    firstIntersectedConnectedRange);
            var nextGap = startSplitPointToNextGap.get(firstIntersectedConnectedRange.getStartSplitPoint());
            if (nextGap != null) {
                nextGap.setPreviousConnectedRange(firstIntersectedConnectedRange);
                nextGap.setLength(differenceFunction.apply(nextGap.getPreviousRangeEnd(),
                        nextGap.getNextRangeStart()));
            }
        }
    }

    private void createNewConnectedRange(Range<Range_, Point_> range) {
        // Range does not intersect anything
        // Ex:
        //     -----
        //----       -----
        var startSplitPoint = splitPointSet.floor(range.getStartSplitPoint());
        var newConnectedRange =
                ConnectedRangeImpl.getConnectedRangeStartingAt(splitPointSet, differenceFunction, startSplitPoint);
        startSplitPointToConnectedRange.put(startSplitPoint, newConnectedRange);

        // If there is a connected range after this range, add a new gap
        // between this range and the next connected range
        var nextConnectedRangeEntry = startSplitPointToConnectedRange.higherEntry(startSplitPoint);
        if (nextConnectedRangeEntry != null) {
            var nextConnectedRange = nextConnectedRangeEntry.getValue();
            var difference = differenceFunction.apply(newConnectedRange.getEnd(), nextConnectedRange.getStart());
            var newGap = new RangeGapImpl<>(newConnectedRange, nextConnectedRange, difference);
            startSplitPointToNextGap.put(startSplitPoint, newGap);
        }

        // If there is a connected range before this range, add a new gap
        // between this range and the previous connected range
        // (this will replace the old gap, if there was one)
        var previousConnectedRangeEntry = startSplitPointToConnectedRange.lowerEntry(startSplitPoint);
        if (previousConnectedRangeEntry != null) {
            var previousConnectedRange = previousConnectedRangeEntry.getValue();
            var difference = differenceFunction.apply(previousConnectedRange.getEnd(), newConnectedRange.getStart());
            var newGap = new RangeGapImpl<>(previousConnectedRange, newConnectedRange, difference);
            startSplitPointToNextGap.put(previousConnectedRangeEntry.getKey(), newGap);
        }
    }

    private void removeSpannedGapsAndUpdateIntersectedGaps(Range<Range_, Point_> range,
            ConnectedRangeImpl<Range_, Point_, Difference_> connectedRange) {
        var firstGapSplitPointBeforeRange = Objects.requireNonNullElseGet(
                startSplitPointToNextGap.floorKey(range.getStartSplitPoint()), range::getStartSplitPoint);
        var intersectedRangeGapMap = startSplitPointToNextGap.subMap(firstGapSplitPointBeforeRange, true,
                range.getEndSplitPoint(), true);

        if (intersectedRangeGapMap.isEmpty()) {
            return;
        }

        var connectedRangeBeforeFirstIntersectedGap =
                (ConnectedRangeImpl<Range_, Point_, Difference_>) (intersectedRangeGapMap.firstEntry().getValue()
                        .getPreviousConnectedRange());
        var connectedRangeAfterFinalIntersectedGap =
                (ConnectedRangeImpl<Range_, Point_, Difference_>) (intersectedRangeGapMap.lastEntry().getValue()
                        .getNextConnectedRange());

        // All gaps that are not the first or last intersected gap will
        // be removed (as the range spans them)
        if (!range.getStartSplitPoint()
                .isAfter(connectedRangeBeforeFirstIntersectedGap.getEndSplitPoint())) {
            if (!range.getEndSplitPoint().isBefore(connectedRangeAfterFinalIntersectedGap.getStartSplitPoint())) {
                // Case: range spans all gaps
                // Ex:
                //   -----------
                //----  ------ -----
                intersectedRangeGapMap.clear();
            } else {
                // Case: range span first gap, but does not span the final gap
                // Ex:
                //   -----------
                //----  ------   -----
                var finalGap = intersectedRangeGapMap.lastEntry().getValue();
                finalGap.setPreviousConnectedRange(connectedRange);
                finalGap.setLength(
                        differenceFunction.apply(finalGap.getPreviousRangeEnd(),
                                finalGap.getNextRangeStart()));
                intersectedRangeGapMap.clear();
                startSplitPointToNextGap.put(connectedRange.getStartSplitPoint(), finalGap);
            }
        } else if (!range.getEndSplitPoint().isBefore(connectedRangeAfterFinalIntersectedGap.getStartSplitPoint())) {
            // Case: range span final gap, but does not span the first gap
            // Ex:
            //     -----------
            //----   -----   -----
            var previousGapEntry = intersectedRangeGapMap.firstEntry();
            var previousGap = previousGapEntry.getValue();
            previousGap.setNextConnectedRange(connectedRange);
            previousGap.setLength(
                    differenceFunction.apply(previousGap.getPreviousRangeEnd(), connectedRange.getStart()));
            intersectedRangeGapMap.clear();
            startSplitPointToNextGap
                    .put(((ConnectedRangeImpl<Range_, Point_, Difference_>) (previousGap
                            .getPreviousConnectedRange())).getStartSplitPoint(), previousGap);
        } else {
            // Case: range does not span either the first or final gap
            // Ex:
            //     ---------
            //----  ------   -----
            var finalGap = intersectedRangeGapMap.lastEntry().getValue();
            finalGap.setLength(differenceFunction.apply(finalGap.getPreviousRangeEnd(),
                    finalGap.getNextRangeStart()));

            var previousGapEntry = intersectedRangeGapMap.firstEntry();
            var previousGap = previousGapEntry.getValue();
            previousGap.setNextConnectedRange(connectedRange);
            previousGap.setLength(
                    differenceFunction.apply(previousGap.getPreviousRangeEnd(), connectedRange.getStart()));

            intersectedRangeGapMap.clear();
            startSplitPointToNextGap.put(previousGapEntry.getKey(), previousGap);
            startSplitPointToNextGap.put(connectedRange.getStartSplitPoint(), finalGap);
        }
    }

    void removeRange(Range<Range_, Point_> range) {
        var connectedRangeEntry = startSplitPointToConnectedRange.floorEntry(range.getStartSplitPoint());
        var connectedRange = connectedRangeEntry.getValue();
        startSplitPointToConnectedRange.remove(connectedRangeEntry.getKey());
        var previousGapEntry = startSplitPointToNextGap.lowerEntry(connectedRangeEntry.getKey());
        var nextConnectedRangeEntry = startSplitPointToConnectedRange.higherEntry(connectedRangeEntry.getKey());
        startSplitPointToNextGap.remove(connectedRangeEntry.getKey());

        var previousGap = (previousGapEntry != null) ? previousGapEntry.getValue() : null;
        var previousConnectedRange = (previousGap != null)
                ? (ConnectedRangeImpl<Range_, Point_, Difference_>) previousGap.getPreviousConnectedRange()
                : null;

        var iterator = new ConnectedSubrangeIterator<>(splitPointSet,
                connectedRange.getStartSplitPoint(),
                connectedRange.getEndSplitPoint(),
                differenceFunction);
        while (iterator.hasNext()) {
            var newConnectedRange = iterator.next();
            if (previousGap != null) {
                previousGap.setNextConnectedRange(newConnectedRange);
                previousGap.setLength(differenceFunction.apply(previousGap.getPreviousConnectedRange().getEnd(),
                        newConnectedRange.getStart()));
                startSplitPointToNextGap
                        .put(((ConnectedRangeImpl<Range_, Point_, Difference_>) previousGap
                                .getPreviousConnectedRange()).getStartSplitPoint(), previousGap);
            }
            previousGap = new RangeGapImpl<>(newConnectedRange, null, null);
            previousConnectedRange = newConnectedRange;
            startSplitPointToConnectedRange.put(newConnectedRange.getStartSplitPoint(), newConnectedRange);
        }

        if (nextConnectedRangeEntry != null && previousGap != null) {
            previousGap.setNextConnectedRange(nextConnectedRangeEntry.getValue());
            previousGap.setLength(differenceFunction.apply(previousConnectedRange.getEnd(),
                    nextConnectedRangeEntry.getValue().getStart()));
            startSplitPointToNextGap.put(previousConnectedRange.getStartSplitPoint(),
                    previousGap);
        } else if (previousGapEntry != null && previousGap == previousGapEntry.getValue()) {
            // i.e. range was the last range in the connected range,
            // (previousGap == previousGapEntry.getValue()),
            // and there is no connected range after it
            // (previousGap != null as previousGapEntry != null,
            // so it must be the case nextConnectedRangeEntry == null)
            startSplitPointToNextGap.remove(previousGapEntry.getKey());
        }
    }

    @Override
    public Iterable<ConnectedRange<Range_, Point_, Difference_>> getConnectedRanges() {
        return (Iterable) startSplitPointToConnectedRange.values();
    }

    @Override
    public Iterable<RangeGap<Point_, Difference_>> getGaps() {
        return (Iterable) startSplitPointToNextGap.values();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ConnectedRangeChainImpl<?, ?, ?> that))
            return false;
        return Objects.equals(startSplitPointToConnectedRange,
                that.startSplitPointToConnectedRange)
                && Objects.equals(splitPointSet,
                        that.splitPointSet)
                && Objects.equals(startSplitPointToNextGap,
                        that.startSplitPointToNextGap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startSplitPointToConnectedRange, splitPointSet, startSplitPointToNextGap);
    }

    @Override
    public String toString() {
        return "ConnectedRangeChain {" +
                "connectedRanges=" + getConnectedRanges() +
                ", gaps=" + getGaps() +
                '}';
    }
}
