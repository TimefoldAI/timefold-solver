package ai.timefold.solver.core.impl.score.stream.collector.connectedRanges;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;

public final class IntervalTree<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>> {

    private final Function<? super Interval_, ? extends Point_> startMapping;
    private final Function<? super Interval_, ? extends Point_> endMapping;
    private final NavigableSet<IntervalSplitPoint<Interval_, Point_>> splitPointSet;
    private final ConnectedRangeChainImpl<Interval_, Point_, Difference_> consecutiveIntervalData;

    public IntervalTree(Function<? super Interval_, ? extends Point_> startMapping,
            Function<? super Interval_, ? extends Point_> endMapping,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        this.startMapping = startMapping;
        this.endMapping = endMapping;
        this.splitPointSet = new TreeSet<>();
        this.consecutiveIntervalData = new ConnectedRangeChainImpl<>(splitPointSet, differenceFunction);
    }

    public Interval<Interval_, Point_> getInterval(Interval_ intervalValue) {
        return new Interval<>(intervalValue, startMapping, endMapping);
    }

    public boolean isEmpty() {
        return splitPointSet.isEmpty();
    }

    public boolean contains(Interval_ o) {
        if (null == o || splitPointSet.isEmpty()) {
            return false;
        }
        var interval = getInterval(o);
        var floorStartSplitPoint = splitPointSet.floor(interval.getStartSplitPoint());
        if (floorStartSplitPoint == null) {
            return false;
        }
        return floorStartSplitPoint.containsIntervalStarting(interval);
    }

    public Iterator<Interval_> iterator() {
        return new IntervalTreeIterator<>(splitPointSet);
    }

    public boolean add(Interval<Interval_, Point_> interval) {
        var startSplitPoint = interval.getStartSplitPoint();
        var endSplitPoint = interval.getEndSplitPoint();

        var flooredStartSplitPoint = splitPointSet.floor(startSplitPoint);
        if (flooredStartSplitPoint == null || !flooredStartSplitPoint.equals(startSplitPoint)) {
            splitPointSet.add(startSplitPoint);
            startSplitPoint.createCollections();
            startSplitPoint.addIntervalStartingAtSplitPoint(interval);
        } else {
            flooredStartSplitPoint.addIntervalStartingAtSplitPoint(interval);
        }

        var ceilingEndSplitPoint = splitPointSet.ceiling(endSplitPoint);
        if (ceilingEndSplitPoint == null || !ceilingEndSplitPoint.equals(endSplitPoint)) {
            splitPointSet.add(endSplitPoint);
            endSplitPoint.createCollections();
            endSplitPoint.addIntervalEndingAtSplitPoint(interval);
        } else {
            ceilingEndSplitPoint.addIntervalEndingAtSplitPoint(interval);
        }

        consecutiveIntervalData.addInterval(interval);
        return true;
    }

    public boolean remove(Interval<Interval_, Point_> interval) {
        var startSplitPoint = interval.getStartSplitPoint();
        var endSplitPoint = interval.getEndSplitPoint();
        var flooredStartSplitPoint = splitPointSet.floor(startSplitPoint);
        if (flooredStartSplitPoint == null || !flooredStartSplitPoint.containsIntervalStarting(interval)) {
            return false;
        }

        flooredStartSplitPoint.removeIntervalStartingAtSplitPoint(interval);
        if (flooredStartSplitPoint.isEmpty()) {
            splitPointSet.remove(flooredStartSplitPoint);
        }

        var ceilEndSplitPoint = splitPointSet.ceiling(endSplitPoint);
        // Not null since the start point contained the interval
        ceilEndSplitPoint.removeIntervalEndingAtSplitPoint(interval);
        if (ceilEndSplitPoint.isEmpty()) {
            splitPointSet.remove(ceilEndSplitPoint);
        }

        consecutiveIntervalData.removeInterval(interval);
        return true;
    }

    public ConnectedRangeChain<Interval_, Point_, Difference_> getConnectedRangeChain() {
        return consecutiveIntervalData;
    }
}
