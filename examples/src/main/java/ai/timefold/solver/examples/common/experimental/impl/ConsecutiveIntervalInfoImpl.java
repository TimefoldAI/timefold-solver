package ai.timefold.solver.examples.common.experimental.impl;

import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;

import ai.timefold.solver.examples.common.experimental.api.ConsecutiveIntervalInfo;
import ai.timefold.solver.examples.common.experimental.api.IntervalBreak;
import ai.timefold.solver.examples.common.experimental.api.IntervalCluster;

public final class ConsecutiveIntervalInfoImpl<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements ConsecutiveIntervalInfo<Interval_, Point_, Difference_> {

    private final NavigableMap<IntervalSplitPoint<Interval_, Point_>, IntervalClusterImpl<Interval_, Point_, Difference_>> clusterStartSplitPointToCluster;
    private final NavigableSet<IntervalSplitPoint<Interval_, Point_>> splitPointSet;
    private final NavigableMap<IntervalSplitPoint<Interval_, Point_>, IntervalBreakImpl<Interval_, Point_, Difference_>> clusterStartSplitPointToNextBreak;
    private final BiFunction<Point_, Point_, Difference_> differenceFunction;

    public ConsecutiveIntervalInfoImpl(TreeSet<IntervalSplitPoint<Interval_, Point_>> splitPointSet,
            BiFunction<Point_, Point_, Difference_> differenceFunction) {
        this.clusterStartSplitPointToCluster = new TreeMap<>();
        this.clusterStartSplitPointToNextBreak = new TreeMap<>();
        this.splitPointSet = splitPointSet;
        this.differenceFunction = differenceFunction;
    }

    void addInterval(Interval<Interval_, Point_> interval) {
        var intersectedIntervalClusterMap = clusterStartSplitPointToCluster
                .subMap(Objects.requireNonNullElseGet(clusterStartSplitPointToCluster.floorKey(interval.getStartSplitPoint()),
                        interval::getStartSplitPoint), true, interval.getEndSplitPoint(), true);

        // Case: the interval cluster before this interval does not intersect this interval
        if (!intersectedIntervalClusterMap.isEmpty()
                && intersectedIntervalClusterMap.firstEntry().getValue().getEndSplitPoint()
                        .isBefore(interval.getStartSplitPoint())) {
            // Get the tail map after the first cluster
            intersectedIntervalClusterMap = intersectedIntervalClusterMap.subMap(intersectedIntervalClusterMap.firstKey(),
                    false, intersectedIntervalClusterMap.lastKey(), true);
        }

        if (intersectedIntervalClusterMap.isEmpty()) {
            // Interval does not intersect anything
            // Ex:
            //     -----
            //----       -----
            createNewIntervalCluster(interval);
            return;
        }

        // Interval intersect at least one cluster
        // Ex:
        //      -----------------
        //  ------  ------  ---   ----
        var firstIntersectedIntervalCluster = intersectedIntervalClusterMap.firstEntry().getValue();
        var oldStartSplitPoint = firstIntersectedIntervalCluster.getStartSplitPoint();
        firstIntersectedIntervalCluster.addInterval(interval);

        // Merge all the intersected interval clusters into the first intersected
        // interval cluster
        intersectedIntervalClusterMap.tailMap(oldStartSplitPoint, false).values()
                .forEach(firstIntersectedIntervalCluster::mergeIntervalCluster);

        // Remove all the intersected interval clusters after the first intersected
        // one, since they are now merged in the first
        intersectedIntervalClusterMap.tailMap(oldStartSplitPoint, false).clear();
        removeSpannedBreaksAndUpdateIntersectedBreaks(interval, firstIntersectedIntervalCluster);

        // If the first intersected interval cluster start after the interval,
        // we need to make the interval start point the key for this interval
        // cluster in the map
        if (oldStartSplitPoint.isAfter(firstIntersectedIntervalCluster.getStartSplitPoint())) {
            clusterStartSplitPointToCluster.remove(oldStartSplitPoint);
            clusterStartSplitPointToCluster.put(firstIntersectedIntervalCluster.getStartSplitPoint(),
                    firstIntersectedIntervalCluster);
            var nextBreak = clusterStartSplitPointToNextBreak.get(firstIntersectedIntervalCluster.getStartSplitPoint());
            if (nextBreak != null) {
                nextBreak.setPreviousCluster(firstIntersectedIntervalCluster);
                nextBreak.setLength(differenceFunction.apply(nextBreak.getPreviousIntervalClusterEnd(),
                        nextBreak.getNextIntervalClusterStart()));
            }
        }
    }

    private void createNewIntervalCluster(Interval<Interval_, Point_> interval) {
        // Interval does not intersect anything
        // Ex:
        //     -----
        //----       -----
        var startSplitPoint = splitPointSet.floor(interval.getStartSplitPoint());
        var newCluster = new IntervalClusterImpl<>(splitPointSet, differenceFunction, startSplitPoint);
        clusterStartSplitPointToCluster.put(startSplitPoint, newCluster);

        // If there a cluster after this interval, add a new break
        // between this interval and the next cluster
        var nextClusterEntry = clusterStartSplitPointToCluster.higherEntry(startSplitPoint);
        if (nextClusterEntry != null) {
            var nextCluster = nextClusterEntry.getValue();
            var difference = differenceFunction.apply(newCluster.getEnd(), nextCluster.getStart());
            var newBreak = new IntervalBreakImpl<>(newCluster, nextCluster, difference);
            clusterStartSplitPointToNextBreak.put(startSplitPoint, newBreak);
        }

        // If there a cluster before this interval, add a new break
        // between this interval and the previous cluster
        // (this will replace the old break, if there was one)
        var previousClusterEntry = clusterStartSplitPointToCluster.lowerEntry(startSplitPoint);
        if (previousClusterEntry != null) {
            var previousCluster = previousClusterEntry.getValue();
            var difference = differenceFunction.apply(previousCluster.getEnd(), newCluster.getStart());
            var newBreak = new IntervalBreakImpl<>(previousCluster, newCluster, difference);
            clusterStartSplitPointToNextBreak.put(previousClusterEntry.getKey(), newBreak);
        }
    }

    private void removeSpannedBreaksAndUpdateIntersectedBreaks(Interval<Interval_, Point_> interval,
            IntervalClusterImpl<Interval_, Point_, Difference_> intervalCluster) {
        var firstBreakSplitPointBeforeInterval = Objects.requireNonNullElseGet(
                clusterStartSplitPointToNextBreak.floorKey(interval.getStartSplitPoint()), interval::getStartSplitPoint);
        var intersectedIntervalBreakMap = clusterStartSplitPointToNextBreak.subMap(firstBreakSplitPointBeforeInterval, true,
                interval.getEndSplitPoint(), true);

        if (intersectedIntervalBreakMap.isEmpty()) {
            return;
        }

        var clusterBeforeFirstIntersectedBreak =
                (IntervalClusterImpl<Interval_, Point_, Difference_>) (intersectedIntervalBreakMap.firstEntry().getValue()
                        .getPreviousIntervalCluster());
        var clusterAfterFinalIntersectedBreak =
                (IntervalClusterImpl<Interval_, Point_, Difference_>) (intersectedIntervalBreakMap.lastEntry().getValue()
                        .getNextIntervalCluster());

        // All breaks that are not the first or last intersected breaks will
        // be removed (as interval span them)
        if (!interval.getStartSplitPoint()
                .isAfter(clusterBeforeFirstIntersectedBreak.getEndSplitPoint())) {
            if (!interval.getEndSplitPoint().isBefore(clusterAfterFinalIntersectedBreak.getStartSplitPoint())) {
                // Case: interval spans all breaks
                // Ex:
                //   -----------
                //----  ------ -----
                intersectedIntervalBreakMap.clear();
            } else {
                // Case: interval span first break, but does not span the final break
                // Ex:
                //   -----------
                //----  ------   -----
                var finalBreak = intersectedIntervalBreakMap.lastEntry().getValue();
                finalBreak.setPreviousCluster(intervalCluster);
                finalBreak.setLength(
                        differenceFunction.apply(finalBreak.getPreviousIntervalClusterEnd(),
                                finalBreak.getNextIntervalClusterStart()));
                intersectedIntervalBreakMap.clear();
                clusterStartSplitPointToNextBreak.put(intervalCluster.getStartSplitPoint(), finalBreak);
            }
        } else if (!interval.getEndSplitPoint().isBefore(clusterAfterFinalIntersectedBreak.getStartSplitPoint())) {
            // Case: interval span final break, but does not span the first break
            // Ex:
            //     -----------
            //----   -----   -----
            var previousBreakEntry = intersectedIntervalBreakMap.firstEntry();
            var previousBreak = previousBreakEntry.getValue();
            previousBreak.setNextCluster(intervalCluster);
            previousBreak.setLength(
                    differenceFunction.apply(previousBreak.getPreviousIntervalClusterEnd(), intervalCluster.getStart()));
            intersectedIntervalBreakMap.clear();
            clusterStartSplitPointToNextBreak
                    .put(((IntervalClusterImpl<Interval_, Point_, Difference_>) (previousBreak
                            .getPreviousIntervalCluster())).getStartSplitPoint(), previousBreak);
        } else {
            // Case: interval does not span either the first or final break
            // Ex:
            //     ---------
            //----  ------   -----
            var finalBreak = intersectedIntervalBreakMap.lastEntry().getValue();
            finalBreak.setLength(differenceFunction.apply(finalBreak.getPreviousIntervalClusterEnd(),
                    finalBreak.getNextIntervalClusterStart()));

            var previousBreakEntry = intersectedIntervalBreakMap.firstEntry();
            var previousBreak = previousBreakEntry.getValue();
            previousBreak.setNextCluster(intervalCluster);
            previousBreak.setLength(
                    differenceFunction.apply(previousBreak.getPreviousIntervalClusterEnd(), intervalCluster.getStart()));

            intersectedIntervalBreakMap.clear();
            clusterStartSplitPointToNextBreak.put(previousBreakEntry.getKey(), previousBreak);
            clusterStartSplitPointToNextBreak.put(intervalCluster.getStartSplitPoint(), finalBreak);
        }
    }

    void removeInterval(Interval<Interval_, Point_> interval) {
        var intervalClusterEntry = clusterStartSplitPointToCluster.floorEntry(interval.getStartSplitPoint());
        var intervalCluster = intervalClusterEntry.getValue();
        clusterStartSplitPointToCluster.remove(intervalClusterEntry.getKey());
        var previousBreakEntry = clusterStartSplitPointToNextBreak.lowerEntry(intervalClusterEntry.getKey());
        var nextIntervalClusterEntry = clusterStartSplitPointToCluster.higherEntry(intervalClusterEntry.getKey());
        clusterStartSplitPointToNextBreak.remove(intervalClusterEntry.getKey());

        var previousBreak = (previousBreakEntry != null) ? previousBreakEntry.getValue() : null;
        var previousIntervalCluster = (previousBreak != null)
                ? (IntervalClusterImpl<Interval_, Point_, Difference_>) previousBreak.getPreviousIntervalCluster()
                : null;

        for (var newIntervalCluster : intervalCluster.removeInterval(interval)) {
            if (previousBreak != null) {
                previousBreak.setNextCluster(newIntervalCluster);
                previousBreak.setLength(differenceFunction.apply(previousBreak.getPreviousIntervalCluster().getEnd(),
                        newIntervalCluster.getStart()));
                clusterStartSplitPointToNextBreak
                        .put(((IntervalClusterImpl<Interval_, Point_, Difference_>) previousBreak
                                .getPreviousIntervalCluster()).getStartSplitPoint(), previousBreak);
            }
            previousBreak = new IntervalBreakImpl<>(newIntervalCluster, null, null);
            previousIntervalCluster = newIntervalCluster;
            clusterStartSplitPointToCluster.put(newIntervalCluster.getStartSplitPoint(), newIntervalCluster);
        }

        if (nextIntervalClusterEntry != null && previousBreak != null) {
            previousBreak.setNextCluster(nextIntervalClusterEntry.getValue());
            previousBreak.setLength(differenceFunction.apply(previousIntervalCluster.getEnd(),
                    nextIntervalClusterEntry.getValue().getStart()));
            clusterStartSplitPointToNextBreak.put(previousIntervalCluster.getStartSplitPoint(),
                    previousBreak);
        } else if (previousBreakEntry != null && previousBreak == previousBreakEntry.getValue()) {
            // i.e. interval was the last interval in the cluster,
            // (previousBreak == previousBreakEntry.getValue()),
            // and there is no interval cluster after it
            // (previousBreak != null as previousBreakEntry != null,
            // so it must be the case nextIntervalClusterEntry == null)
            clusterStartSplitPointToNextBreak.remove(previousBreakEntry.getKey());
        }
    }

    @Override
    public Iterable<IntervalCluster<Interval_, Point_, Difference_>> getIntervalClusters() {
        return (Iterable) clusterStartSplitPointToCluster.values();
    }

    @Override
    public Iterable<IntervalBreak<Interval_, Point_, Difference_>> getBreaks() {
        return (Iterable) clusterStartSplitPointToNextBreak.values();
    }

    @Override
    public String toString() {
        return "Clusters {" +
                "intervalClusters=" + getIntervalClusters() +
                ", breaks=" + getBreaks() +
                '}';
    }
}
