package ai.timefold.solver.core.impl.score.stream.collector.connected_ranges;

import java.util.Objects;

import ai.timefold.solver.core.api.score.stream.common.ConnectedRange;
import ai.timefold.solver.core.api.score.stream.common.RangeGap;

final class RangeGapImpl<Range_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements RangeGap<Point_, Difference_> {
    private ConnectedRange<Range_, Point_, Difference_> previousConnectedRange;
    private ConnectedRange<Range_, Point_, Difference_> nextConnectedRange;
    private Difference_ length;

    RangeGapImpl(ConnectedRange<Range_, Point_, Difference_> previousConnectedRange,
            ConnectedRange<Range_, Point_, Difference_> nextConnectedRange, Difference_ length) {
        this.previousConnectedRange = previousConnectedRange;
        this.nextConnectedRange = nextConnectedRange;
        this.length = length;
    }

    ConnectedRange<Range_, Point_, Difference_> getPreviousConnectedRange() {
        return previousConnectedRange;
    }

    ConnectedRange<Range_, Point_, Difference_> getNextConnectedRange() {
        return nextConnectedRange;
    }

    @Override
    public Point_ getPreviousRangeEnd() {
        return previousConnectedRange.getEnd();
    }

    @Override
    public Point_ getNextRangeStart() {
        return nextConnectedRange.getStart();
    }

    @Override
    public Difference_ getLength() {
        return length;
    }

    void setPreviousConnectedRange(ConnectedRange<Range_, Point_, Difference_> previousConnectedRange) {
        this.previousConnectedRange = previousConnectedRange;
    }

    void setNextConnectedRange(ConnectedRange<Range_, Point_, Difference_> nextConnectedRange) {
        this.nextConnectedRange = nextConnectedRange;
    }

    void setLength(Difference_ length) {
        this.length = length;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RangeGapImpl<?, ?, ?> rangeGap))
            return false;
        return Objects.equals(getPreviousRangeEnd(), rangeGap.getPreviousRangeEnd()) &&
                Objects.equals(getNextRangeStart(), rangeGap.getNextRangeStart());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPreviousRangeEnd(), getNextRangeStart());
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
