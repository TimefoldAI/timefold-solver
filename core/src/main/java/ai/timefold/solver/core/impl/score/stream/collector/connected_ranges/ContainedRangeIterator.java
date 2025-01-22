package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import java.util.Iterator;

final class ContainedRangeIterator<Range_, Point_ extends Comparable<Point_>> implements Iterator<Range_> {

    private final Iterator<RangeSplitPoint<Range_, Point_>> splitPointSetIterator;
    private Iterator<Range_> splitPointValueIterator;

    ContainedRangeIterator(Iterable<RangeSplitPoint<Range_, Point_>> splitPointSet) {
        this.splitPointSetIterator = splitPointSet.iterator();
        if (splitPointSetIterator.hasNext()) {
            splitPointValueIterator = splitPointSetIterator.next().getValuesStartingFromSplitPointIterator();
        }
    }

    @Override
    public boolean hasNext() {
        return splitPointValueIterator != null && splitPointValueIterator.hasNext();
    }

    @Override
    public Range_ next() {
        var next = splitPointValueIterator.next();

        while (!splitPointValueIterator.hasNext() && splitPointSetIterator.hasNext()) {
            splitPointValueIterator = splitPointSetIterator.next().getValuesStartingFromSplitPointIterator();
        }

        if (!splitPointValueIterator.hasNext()) {
            splitPointValueIterator = null;
        }

        return next;
    }
}
