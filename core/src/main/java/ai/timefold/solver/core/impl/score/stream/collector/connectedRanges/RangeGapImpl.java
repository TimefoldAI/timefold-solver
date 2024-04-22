package ai.timefold.solver.core.impl.score.stream.collector.connectedRanges;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRange;
import ai.timefold.solver.core.api.score.stream.common.RangeGap;

final class RangeGapImpl<Interval_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements RangeGap<Point_, Difference_> {
    private ConnectedRange<Interval_, Point_, Difference_> previousCluster;
    private ConnectedRange<Interval_, Point_, Difference_> nextCluster;
    private Difference_ length;

    RangeGapImpl(ConnectedRange<Interval_, Point_, Difference_> previousCluster,
            ConnectedRange<Interval_, Point_, Difference_> nextCluster, Difference_ length) {
        this.previousCluster = previousCluster;
        this.nextCluster = nextCluster;
        this.length = length;
    }

    public ConnectedRange<Interval_, Point_, Difference_> getPreviousConcurrentUsage() {
        return previousCluster;
    }

    public ConnectedRange<Interval_, Point_, Difference_> getNextConcurrentUsage() {
        return nextCluster;
    }

    @Override
    public Point_ getPreviousRangeEnd() {
        return previousCluster.getEnd();
    }

    @Override
    public Point_ getNextRangeStart() {
        return nextCluster.getStart();
    }

    @Override
    public Difference_ getLength() {
        return length;
    }

    void setPreviousCluster(ConnectedRange<Interval_, Point_, Difference_> previousCluster) {
        this.previousCluster = previousCluster;
    }

    void setNextCluster(ConnectedRange<Interval_, Point_, Difference_> nextCluster) {
        this.nextCluster = nextCluster;
    }

    void setLength(Difference_ length) {
        this.length = length;
    }

    @Override
    public String toString() {
        return "RangeGap{" +
                "start=" + getPreviousRangeEnd() +
                ", end=" + getNextRangeStart() +
                ", length=" + length +
                '}';
    }
}
