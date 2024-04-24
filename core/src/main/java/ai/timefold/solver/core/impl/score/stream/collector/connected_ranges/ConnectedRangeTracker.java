package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRangeChain;

public final class ConnectedRangeTracker<Range_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>> {

    private final Function<? super Range_, ? extends Point_> startMapping;
    private final Function<? super Range_, ? extends Point_> endMapping;
    private final NavigableSet<RangeSplitPoint<Range_, Point_>> splitPointSet;
    private final ConnectedRangeChainImpl<Range_, Point_, Difference_> connectedRangeChain;

    public ConnectedRangeTracker(Function<? super Range_, ? extends Point_> startMapping,
            Function<? super Range_, ? extends Point_> endMapping,
            BiFunction<? super Point_, ? super Point_, ? extends Difference_> differenceFunction) {
        this.startMapping = startMapping;
        this.endMapping = endMapping;
        this.splitPointSet = new TreeSet<>();
        this.connectedRangeChain = new ConnectedRangeChainImpl<>(splitPointSet, differenceFunction);
    }

    public Range<Range_, Point_> getRange(Range_ rangeValue) {
        return new Range<>(rangeValue, startMapping, endMapping);
    }

    public boolean isEmpty() {
        return splitPointSet.isEmpty();
    }

    public boolean contains(Range_ o) {
        if (null == o || splitPointSet.isEmpty()) {
            return false;
        }
        var range = getRange(o);
        var floorStartSplitPoint = splitPointSet.floor(range.getStartSplitPoint());
        if (floorStartSplitPoint == null) {
            return false;
        }
        return floorStartSplitPoint.containsRangeStarting(range);
    }

    public Iterator<Range_> iterator() {
        return new ContainedRangeIterator<>(splitPointSet);
    }

    public boolean add(Range<Range_, Point_> range) {
        var startSplitPoint = range.getStartSplitPoint();
        var endSplitPoint = range.getEndSplitPoint();

        var flooredStartSplitPoint = splitPointSet.floor(startSplitPoint);
        if (flooredStartSplitPoint == null || !flooredStartSplitPoint.equals(startSplitPoint)) {
            splitPointSet.add(startSplitPoint);
            startSplitPoint.createCollections();
            startSplitPoint.addRangeStartingAtSplitPoint(range);
        } else {
            flooredStartSplitPoint.addRangeStartingAtSplitPoint(range);
        }

        var ceilingEndSplitPoint = splitPointSet.ceiling(endSplitPoint);
        if (ceilingEndSplitPoint == null || !ceilingEndSplitPoint.equals(endSplitPoint)) {
            splitPointSet.add(endSplitPoint);
            endSplitPoint.createCollections();
            endSplitPoint.addRangeEndingAtSplitPoint(range);
        } else {
            ceilingEndSplitPoint.addRangeEndingAtSplitPoint(range);
        }

        connectedRangeChain.addRange(range);
        return true;
    }

    public boolean remove(Range<Range_, Point_> range) {
        var startSplitPoint = range.getStartSplitPoint();
        var endSplitPoint = range.getEndSplitPoint();
        var flooredStartSplitPoint = splitPointSet.floor(startSplitPoint);
        if (flooredStartSplitPoint == null || !flooredStartSplitPoint.containsRangeStarting(range)) {
            return false;
        }

        flooredStartSplitPoint.removeRangeStartingAtSplitPoint(range);
        if (flooredStartSplitPoint.isEmpty()) {
            splitPointSet.remove(flooredStartSplitPoint);
        }

        var ceilEndSplitPoint = splitPointSet.ceiling(endSplitPoint);
        // Not null since the start point contained the range
        ceilEndSplitPoint.removeRangeEndingAtSplitPoint(range);
        if (ceilEndSplitPoint.isEmpty()) {
            splitPointSet.remove(ceilEndSplitPoint);
        }

        connectedRangeChain.removeRange(range);
        return true;
    }

    public ConnectedRangeChain<Range_, Point_, Difference_> getConnectedRangeChain() {
        return connectedRangeChain;
    }
}
