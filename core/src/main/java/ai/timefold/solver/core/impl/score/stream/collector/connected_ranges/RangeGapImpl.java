package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRange;
import ai.timefold.solver.core.api.score.stream.common.RangeGap;

final class RangeGapImpl<Range_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements RangeGap<Point_, Difference_> {
    private ConnectedRange<Range_, Point_, Difference_> previousCluster;
    private ConnectedRange<Range_, Point_, Difference_> nextCluster;
    private Difference_ length;

    RangeGapImpl(ConnectedRange<Range_, Point_, Difference_> previousCluster,
            ConnectedRange<Range_, Point_, Difference_> nextCluster, Difference_ length) {
        this.previousCluster = previousCluster;
        this.nextCluster = nextCluster;
        this.length = length;
    }

    ConnectedRange<Range_, Point_, Difference_> getPreviousConnectedRange() {
        return previousCluster;
    }

    ConnectedRange<Range_, Point_, Difference_> getNextConnectedRange() {
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

    void setPreviousCluster(ConnectedRange<Range_, Point_, Difference_> previousCluster) {
        this.previousCluster = previousCluster;
    }

    void setNextCluster(ConnectedRange<Range_, Point_, Difference_> nextCluster) {
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
